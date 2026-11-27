# Permanent APK signing

The distributed APK is built on main and signed with a dedicated, owner-controlled PKCS12 key. PR builds run tests and assemble debug APKs but never publish an installable artifact. Missing or mismatched signing credentials fail the main build, with no fallback debug APK.

## One-time owner setup

Create a private PKCS12 keystore outside the repository using JDK keytool:

```powershell
keytool -genkeypair -storetype PKCS12 -keystore lectav-release.p12 -alias lectav -keyalg RSA -keysize 3072 -validity 10000 -dname "CN=LECTAV"
```

Use a strong unique password. Keep an encrypted offline backup of the keystore and password. Never commit the file or share either secret in chat.

In repository Settings > Secrets and variables > Actions, create repository secrets:
- LECTAV_KEYSTORE_B64: the complete Base64 encoding of the PKCS12 file (not a file path).
- LECTAV_KEYSTORE_PASSWORD: the keystore password (also used for the PKCS12 key).

Create repository variable LECTAV_CERT_SHA256 with the SHA-256 fingerprint of the public signing certificate. Export the certificate in DER format and hash it:

```powershell
keytool -exportcert -storetype PKCS12 -keystore lectav-release.p12 -alias lectav -file lectav-public.cer
(Get-FileHash .\lectav-public.cer -Algorithm SHA256).Hash
```

To copy Base64 directly to the clipboard in PowerShell without displaying it:
```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes((Resolve-Path .\lectav-release.p12).Path)) | Set-Clipboard
```
Clear the clipboard after pasting. Run the workflow from main. Download only the lectav-signed-apk artifact. The APK is a permanently signed debug build, not a hardened release build.

## Migration

Old CI debug builds may have different signing keys. Android cannot update those with a new key. Before uninstalling, export and verify a complete encrypted LECTAV backup outside the app-specific directory, copy large media separately if needed, and keep the password. Install the new signed APK and restore. Login credentials and downloaded voice models may need reconfiguration. If the original private key is available, use it instead of replacing it, provided it can be safely converted to PKCS12 and meets the signing configuration.

Future APKs must use the same key and a higher versionCode. Never replace the key just to fix a build. Review workflow changes and restrict main-branch write access because trusted main workflows can access signing secrets.
