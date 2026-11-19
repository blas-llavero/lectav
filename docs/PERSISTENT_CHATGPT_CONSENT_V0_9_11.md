# LECTAV 0.9.11 — Persistent ChatGPT content-generation authorization

## Goal

The user should not have to reconfirm ChatGPT usage and credits before each manual document, trimester plan or automatic queue action. A single account-scoped, revocable setting is provided at **Settings → ChatGPT usage authorization**.

The agreement states that the user has reviewed the usage and credit settings of their account and authorizes LECTAV to generate content through ChatGPT. Once enabled, the same authorization applies across manual content generation, trimester planning and sequential automatic generation.

## Security properties

- The account must be authenticated and enabled for inference.
- The local consent grant is bound to a fingerprint derived from the signed-in account identity, preventing silent reuse for a different account.
- The checkbox can be disabled at any time; subsequent requests must recheck it.
- The authorization is intentionally not transferred by the `.lectavbak` file. Each new device or account requires its own authorization.
- Login tokens and other account secrets are never stored in portable backups.
- Genuine account quota, rate limits and paid-credit restrictions remain in effect. LECTAV cannot read the actual credit balance.
- The configurable automatic-queue request cap is a local protection rather than a prediction of account usage.

The legacy OpenAI usage-confirmation dialog is no longer necessary for each request. Purely local demo/Ollama generation does not require ChatGPT consent.

## Validation

Enable consent and create two documents; verify no new consent prompt appears. Create a trimester plan, test an automatic queue and disable consent; subsequent requests must be refused. Test switching to a different account and restoring a backup on another device. Confirm that all non-secret model preferences are preserved while the consent grant is not.
