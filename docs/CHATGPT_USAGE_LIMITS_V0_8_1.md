# LECTAV 0.8.1 — ChatGPT requests and real account limits

## Core rule

LECTAV cannot read the actual remaining ChatGPT plan credits or usage balance. Neither a client-side counter nor a generated estimate should be presented as an authoritative balance. OpenAI's real account restrictions, quota and applicable billing conditions remain independent.

The original implementation included per-request usage confirmation and local protective counters. Later LECTAV releases changed this model: local artificial weekly attempt limits were removed and account-specific consent can be configured persistently in Settings. Historical behavior in this document should not be mistaken for the current app's controls.

## Responsible generation

- Require an authenticated account and supported model before sending a request.
- Do not invent available model identifiers or promise unlimited requests.
- Stop clearly on quota, authentication, network and server errors.
- Separate local FOSS/offline models from requests that reach a third-party service.
- Never transmit pupil personal data unless explicitly permitted under applicable safeguards.
- Do not use silent retries to defeat an account's limits.
- Preserve generated files already saved when a later request fails.

## Testing

Verify authentication expiry, missing models, server errors, user cancellation and disconnected network. Confirm that the UI does not claim to know the real account balance. Check the current persistent consent preference separately from deprecated per-request confirmation behavior.
