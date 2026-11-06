# LECTAV 0.8.0 — ChatGPT sign-in integration pilot

**Status:** Android implementation compiled in CI; actual live OAuth must be tested on a device with an eligible account. Not a production-ready multi-user school LMS.

## What this first Android version implements

- Settings → **Compte ChatGPT** → **Continua amb ChatGPT** starts the **official open-source ChatGPT-plan OAuth flow** in the system browser, using the published `https://auth.openai.com/api/accounts/authorize` endpoint.
- Generate fresh 256-bit PKCE verifier, SHA-256 code challenge, anti-CSRF `state`, and OIDC `nonce` on **each attempt**.
- Bind a callback listener exclusively to `127.0.0.1` on a randomly available port, with the exact required `/auth/callback` path; match the `state` before exchanging the authorization code. Bind before opening the system browser. A callback with a wrong state, missing issued client ID, or mismatched returning client ID is rejected.
- Create and keep an opaque per-installation host ID `urn:uuid:<UUID>`, and register on first sign-in using `dynamic_agent_client`. Reauthorization uses the issued `client_id` and retained `id_token_hint`.
- Exchange the code over HTTPS, then **verify the ID token signature against the issuer's published JWKS**, issuer, audience, expiration, issue time, and nonce before saving credentials.
- Protect the access, refresh and ID tokens with Android **Keystore AES-256-GCM** encryption in private application storage. No secrets are committed to GitHub or bundled into the APK.
- Check `chatgpt.tokens.use.direct` and `resource.invoke` grants before enabling generation. Reconsent may be requested after a prior decline. If disconnected, the app retains the local library, curriculum and draft demo modes.
- Settings → ChatGPT → **Desconnecta** attempts the official remote revocation and clears the local encrypted tokens; if network revocation cannot be confirmed, the app explains that the user should review their ChatGPT Settings.
- In a saved course, **Materials → Genera** includes a new **ChatGPT (compte propi)** mode, which queries the account's available models at `GET https://api.openai.com/v1/models`, and allows a user to select a listed model.
- Only upon explicit generation, send the educational subject, year, topic, type and language as a text prompt to `POST https://api.openai.com/v1/responses`, using that account's OAuth access token with `store:false` and `stream:true`. Consume SSE until `response.completed`; incomplete responses do not become saved PDFs.
- Keep the resulting response as an **editable draft**. PDF generation occurs locally only when the user explicitly presses the Save button, preserving private files and course associations.

## Important limits and conditions

- **Live login and inference need a personal device test.** CI compilation alone cannot prove that account permissions, the browser callback, dynamic client registration, model catalog or inference work with a particular account.
- ChatGPT-plan inference is available only to eligible accounts with explicit consent, supported models, workspace policies, quotas and permitted region. There is no automatic fallback to a paid API key or hidden charges.
- This is a **single active account** first version. Each teacher/learner identity and progress must be separated before offering it on shared classroom tablets. Do not sign in on shared pupil devices in this prototype.
- The ChatGPT account is **not** the school's user account. Learner completion currently remains local and is not protected as authoritative student assessment or multi-user records.
- The API route supports text generation, **not** generation of MP4/avatar videos, audio or images. LECTAV will require a separate independently vetted FOSS video/voice animation stack.
- No personal student records or textbook photographs are sent to OpenAI automatically. The user must avoid adding sensitive data to free-text topic fields. Contents that users explicitly enter and submit to ChatGPT mode leave the device for inference. The local Ollama/demo modes are still available.
- The first-version screens for login and generation are in Catalan. The broader app remains compatible with its existing language selector; translating these newly introduced screens is a later accessibility task.
- Keystore encryption prevents ordinary extraction of stored tokens; it does not protect a rooted or compromised device or from other shared-device misuse.
- Future work: more robust secure account switching, browser interruption and cancellation, KeyStore recovery, external login testing, PKCE/JWT unit tests, secure curriculum-based prompts and per-user learner profiles.

## Test steps

1. Upgrade from 0.7.0 **without uninstalling**, if Android accepts the APK signing certificate. Retain a safe export of any important materials before testing.
2. Verify offline demo, saved courses, PDF reading and previously imported MP4s still work while signed out.
3. Settings → Compte ChatGPT → Continue with ChatGPT. Approve account and explicit ChatGPT-plan usage in the system browser, return to LECTAV.
4. Verify the displayed connected identity and authorized-plan status. If declined, verify generated content remains disabled and the local modes still work.
5. Open a saved subject → Materials → Generate → ChatGPT, review the available model list and generate a short topic-based explanation; save and open its PDF. Verify the response is not silently saved before confirmation.
6. Disconnect; verify the account is no longer usable for inference and existing local files remain.
7. Test denial, offline, unavailable callback, missing scope, expired tokens, quota exhaustion and switching language. **Do not upload, paste or share credentials when reporting errors.**

## Official sources

- https://developers.openai.com/siwc/quickstart
- https://developers.openai.com/siwc/token-sharing-open-source
- https://developers.openai.com/siwc/token-sharing-open-source/sign-in
- https://developers.openai.com/siwc/token-sharing-open-source/profiles-and-sessions
- https://developers.openai.com/siwc/token-sharing-open-source/models-and-inference
- https://developers.openai.com/siwc/token-sharing-open-source/preview-limitations
