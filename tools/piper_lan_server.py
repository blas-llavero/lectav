#!/usr/bin/env python3
"""LECTAV: opt-in, local-network Piper voice synthesis (GPL-3.0-or-later).

Requirements: piper CLI, ffmpeg, a .onnx voice and corresponding .onnx.json.
No proprietary cloud services. Bind only to a trusted local interface.
"""
from __future__ import annotations
import json
import os
import re
import secrets
import subprocess
import tempfile
import wave
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

HOST = os.getenv("LECTAV_BIND", "127.0.0.1")
PORT = int(os.getenv("LECTAV_PORT", "8765"))
TOKEN = os.environ.get("LECTAV_TOKEN", "")
VOICE_A = Path(os.getenv("LECTAV_VOICE_A", ""))
VOICE_B = Path(os.getenv("LECTAV_VOICE_B", ""))
MAX_INPUT = 24000


def voice_file(voice: Path) -> Path:
    if not voice.is_file() or voice.suffix != ".onnx":
        raise RuntimeError("Missing Piper ONNX voice. Configure LECTAV_VOICE_A/B.")
    if not Path(str(voice) + ".json").is_file():
        raise RuntimeError("Missing corresponding ONNX JSON voice configuration.")
    return voice


def render_segment(text: str, voice: Path, work: Path, n: int) -> Path:
    wav = work / f"part_{n:04}.wav"
    subprocess.run(
        ["piper", "--model", str(voice_file(voice)), "--output_file", str(wav)],
        input=text, text=True, capture_output=True, check=True, timeout=180,
    )
    if not wav.is_file() or wav.stat().st_size < 64:
        raise RuntimeError("Piper produced no usable WAV file")
    return wav


def convert_to_mp3(parts: list[Path], work: Path) -> bytes:
    # Decode and concatenate PCM using ffmpeg's concat filter; sample-rate and
    # channels may differ between separately installed Piper voices.
    args = ["ffmpeg", "-hide_banner", "-loglevel", "error", "-nostdin", "-y"]
    for item in parts:
        args.extend(["-i", str(item)])
    nodes = "".join(f"[{i}:a]" for i in range(len(parts)))
    args.extend([
        "-filter_complex", f"{nodes}concat=n={len(parts)}:v=0:a=1[out]",
        "-map", "[out]", "-ac", "1", "-ar", "24000",
        "-codec:a", "libmp3lame", "-b:a", "96k", str(work / "output.mp3")
    ])
    subprocess.run(args, check=True, capture_output=True, timeout=240)
    final = work / "output.mp3"
    if not final.is_file() or not final.stat().st_size:
        raise RuntimeError("MP3 conversion failed")
    if final.stat().st_size > 20 * 1024 * 1024:
        raise RuntimeError("Audio exceeds 20MB")
    return final.read_bytes()


def parse_script(text: str, podcast: bool) -> list[tuple[Path, str]]:
    text = text.strip()
    if not podcast:
        # Short sentences avoid huge one-shot Piper synthesis.
        sentences = re.split(r"(?<=[.!?])\s+", text)
        chunks, current = [], ""
        for sentence in sentences:
            if len(current) + len(sentence) > 550 and current:
                chunks.append(current)
                current = ""
            current += (" " if current else "") + sentence
        if current:
            chunks.append(current)
        return [(VOICE_A, part) for part in chunks]
    lines = [line.strip() for line in text.splitlines() if line.strip()]
    if not lines or any(not re.match(r"^[AB]:\s*\S", line) for line in lines):
        raise ValueError("Podcast dialogue must use A: / B: at the start of each line.")
    return [
        (VOICE_A if line.startswith("A:") else VOICE_B, line[2:].strip())
        for line in lines
    ]


class Handler(BaseHTTPRequestHandler):
    def do_POST(self) -> None:
        if self.path != "/v1/synthesize":
            self.send_error(404)
            return
        if not TOKEN or not secrets.compare_digest(
            self.headers.get("Authorization", ""), f"Bearer {TOKEN}"
        ):
            self.send_error(401, "Missing or incorrect server token")
            return
        try:
            size = int(self.headers.get("Content-Length", "0"))
            if size < 2 or size > MAX_INPUT * 4:
                self.send_error(413, "Request too large")
                return
            obj = json.loads(self.rfile.read(size))
            body = obj.get("text")
            mode = obj.get("mode")
            if not isinstance(body, str) or not 1 <= len(body) <= MAX_INPUT:
                raise ValueError("Text must contain 1–24000 characters")
            if mode not in ("audio", "podcast"):
                raise ValueError("Mode must be audio or podcast")
            segments = parse_script(body, mode == "podcast")
            if len(segments) > 120:
                raise ValueError("Too many voice segments (max 120)")
            with tempfile.TemporaryDirectory(prefix="lectav_piper_") as tmp:
                work = Path(tmp)
                paths = [render_segment(t, v, work, i) for i, (v, t) in enumerate(segments)]
                audio = convert_to_mp3(paths, work)
            self.send_response(200)
            self.send_header("Content-Type", "audio/mpeg")
            self.send_header("Content-Length", str(len(audio)))
            self.end_headers()
            self.wfile.write(audio)
        except (ValueError, json.JSONDecodeError) as e:
            self.send_error(400, str(e))
        except Exception as e:
            # Do not leak server filesystem paths or internal subprocess stderr.
            print(f"Audio synthesis failed: {type(e).__name__}: {e}", flush=True)
            self.send_error(500, "Piper or ffmpeg failed; check local server log.")

    def log_message(self, format: str, *args: object) -> None:
        # Avoid storing user-authored teaching content in logs.
        print("%s - %s" % (self.address_string(), format % args), flush=True)


if __name__ == "__main__":
    if len(TOKEN) < 24:
        raise SystemExit("Set LECTAV_TOKEN to a random secret of >=24 chars")
    voice_file(VOICE_A)
    voice_file(VOICE_B)
    print(f"LECTAV Piper listening on {HOST}:{PORT} (local LAN only)")
    ThreadingHTTPServer((HOST, PORT), Handler).serve_forever()
