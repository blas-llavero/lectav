# LECTAV — optional ChatGPT sign-in and AI plans

Status: **technical design / not implemented**. Verified with OpenAI's documentation on 2026-11-06. This file is not a functioning login and no access token, API key, password, or account information is stored here.

## Product behavior

LECTAV should allow teachers and learners to:

1. Work offline with the existing local library without an account.
2. Optionally choose **Continue with ChatGPT** to connect their own ChatGPT account.
3. Grant a separate authorization if they wish to use supported inference from their **eligible ChatGPT plan**; identity-only sign-in is not inference authorization.
4. Generate a curriculum-linked didactic programme, course topics, theory, exercises, worked solutions, quizzes and **avatar scripts** as original, *unverified drafts*. Review before adopting or sharing.
5. Use a local, FOSS PDF renderer/authoring engine to save/export PDFs to device/SD. Track session progress locally in isolated personal profiles.
6. Optionally share reviewed documents, with user control. No automatic GitHub commits, public publishing, or upload of student records.
7. Use a separately verified FOSS text-to-speech / avatar animation / local MP4 encoder for per-session video. **Sign in with ChatGPT's currently documented plan-inference route does NOT include hosted image generation, audio transcription or video creation.** Do not conflate text/script generation with creating MP4 files.

## New supported integration — Sign in with ChatGPT for open source clients

Official OpenAI docs:
- https://developers.openai.com/siwc/quickstart
- https://developers.openai.com/siwc/token-sharing-open-source
- https://developers.openai.com/siwc/token-sharing-open-source/sign-in
- https://developers.openai.com/siwc/token-sharing-open-source/profiles-and-sessions
- https://developers.openai.com/siwc/token-sharing-open-source/models-and-inference
- https://developers.openai.com/siwc/token-sharing-open-source/preview-limitations
- https://openai.com/policies/sign-in-with-chatgpt-terms/

For eligible Plus/Pro users, the OSS flow can allow ChatGPT-plan inference through an OAuth access token, *without collecting an OpenAI API key or charging API calls separately on that special authorized route*. Eligibility, account usage caps, workspace policy, model catalog and user permission all apply. Other uses of the public OpenAI API can still be billed separately. No user may borrow the plan of another person.

## Secure Android implementation stages

1. Investigate Android compatibility of the OpenAI-defined **HTTP 127.0.0.1 loopback callback**, including local HTTP listener and system-browser behavior, and follow any current Android-specific official guidance. Never substitute a homemade authorization or scraper of ChatGPT webpages.
2. For a per-device OSS client, prepare a stable random `ext_agent_host_id` and use the documented dynamic-client first registration `client_id=dynamic_agent_client`; save the OpenAI-issued `client_id` for future sign-ins to that account.
3. Use standard OAuth 2.0 authorization-code flow with PKCE/S256, random state and nonce. Open official OpenAI authorization in the Android system browser; validate redirect, OAuth state and ID token issuer/audience/nonce/JWKS according to the official spec.
4. Request identity scopes `openid profile email` and, when users explicitly opt in to plan usage, `offline_access resource.invoke chatgpt.tokens.use.direct`. Confirm granted scopes before inference. Do not infer permission from a successful login.
5. Store access/refresh/id tokens only in protected, application-owned device storage (Android Keystore-backed encryption), never in GitHub, logs, APK constants, public web storage, analytics or a central server. Refresh and revoke securely. Provide sign out, disconnect and multiple user accounts (with independent school roles and course/progress data).
6. Query the authorized user's available models using the documented model catalog. For plan inference, use OAuth token at `POST https://api.openai.com/v1/responses`; **`store:false`, `stream:true`**, client-side streaming through `response.completed`, with supported `input` fields only. Do NOT use ChatGPT internal `backend-api` routes.
7. Show model, connected account, plan status, local/offline fallback, data-to-be-sent preview, usage-limit handling, and a link to ChatGPT settings. Do not perform requests from a teacher's subscription on a pupil's behalf; each user authorizes their own requests.
8. Provide optional non-ChatGPT FOSS local inference (llama.cpp) or privately hosted Ollama, while making consent and privacy differences clear.
9. Add integration tests for callback spoofing, nonce/state validation, session expiry/refresh, revoked consent, user switching, account isolation, usage limits, interrupted streams, offline mode and restarting the app.

## Curriculum and child protections

- Only an authoritative, versioned curriculum registry can define applicable legal standards by country/territory, level, subject and effective date. AI proposes a teacher-editable sequence and maps it to real official competencies, criteria and sabers, never invents legislation or names a legal 'first topic' without grounds.
- Teacher-created plan vs pupil lesson materials must have different editable/published privileges.
- Do not send student names, marks, support plans, images, school identifiers or other sensitive/minor data to ChatGPT by default. Implement explicit informed consent, role-based access, storage minimization and appropriate GDPR protections for deployment in schools.
- An educational LECTAV account and a ChatGPT account are distinct things; one alone does not imply the other. Personal local learner progress must not be merged between students on a shared device.

## What we have *not* done yet

No working ChatGPT login, account-plan token authorization, real ChatGPT-generated course, speech/avatar video engine or multi-user isolation is implemented by this document. The existing v0.7 app continues to support offline materials, editable prototype plans and optional self-hosted Ollama.

For Android, the exact native OAuth loopback flow must be verified securely before any sign-in controls are marked functional or shipped to pupils.
