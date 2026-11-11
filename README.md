# BSB-X-HUB

Consent-based Android device pairing starter project.

## Included
- Native Kotlin + Jetpack Compose Android app.
- GitHub Actions workflow that builds and uploads a debug APK.
- Minimal HTTPS JSON backend for 5-minute, single-use pairing tokens.
- Token hash stored server-side; raw token returned only at creation.
- Pairing endpoint consumes the token and returns an approval-pending request ID.

## Important current scope
This is a **buildable starter/MVP scaffold**, not a completed remote-control product. The Android app can create tokens and submit pairing requests once a backend URL is configured. The backend currently uses in-memory storage: restarting it clears tokens and requests. It does not yet implement owner-side request approval, WebRTC signaling/media, file transfer, GPS, microphone capture, or device locking. Those require additional implementation and device testing. No covert access, silent capture, or remote power-off is implemented.

## Build APK with GitHub Actions
1. Create a new GitHub repository named `BSB-X-HUB`.
2. Upload/push the contents of this folder to the repository's `main` branch.
3. Open **Actions** and run **BSB-X-HUB Android APK**, or push a change under `app/`.
4. Open the successful workflow run, then download artifact `BSB-X-HUB-debug`.
5. Install `app-debug.apk` on a test Android phone.

## Backend
Use Node.js 20+ and a platform that provides HTTPS.
```bash
cd backend
npm install
npm start
```
Deploy behind HTTPS; do not expose plain HTTP. Set `ADMIN_EMAIL` if needed. The in-memory implementation is for development only; use a persistent database, rate limiting, authentication, audit logs, and TLS termination before real deployment.

## Configure Android app
In `MainActivity.kt`, replace `https://YOUR_BACKEND_HOST` with your deployed HTTPS base URL, without a trailing slash. Rebuild via GitHub Actions.

## Security
- Pairing token is single-use and expires after 5 minutes.
- Pairing request is explicitly marked `AWAITING_OWNER_APPROVAL`.
- A token is not authorization to access a device.
- Add owner approval UI and per-feature Android runtime/system consent before enabling any sensitive capability.
