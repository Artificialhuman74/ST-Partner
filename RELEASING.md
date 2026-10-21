# Releasing to Google Play

```bash
./release.sh --notes "What's new in this version (up to 500 characters)"
```

That checks which version codes Play already has, bumps `versionCode` / `versionName` past them,
runs the unit tests, builds the signed bundle (a copy stays in `dist/`), uploads it to the
production track and sends it for review, then commits the bump, tags it `v<versionName>` and
pushes. Commit your work first; the script stops if there are uncommitted changes.

Other options: `--track internal|alpha|beta|production`, `--rollout 20` (staged, 20% of users),
`--draft` (press Roll out yourself in Play Console), `--name 8.0` (pick the version name),
`--no-upload` (build only), `--no-push`. `./release.sh --help` lists them all.

## One-time setup

Uploading uses a Google Play service account, the same one for every app in the developer account.

1. Google Cloud Console → pick or create a project → **APIs & Services → Library** → enable
   **Google Play Android Developer API**.
2. **IAM & Admin → Service accounts → Create service account** (no roles needed).
   Open it → **Keys → Add key → JSON**. Move the downloaded file to
   `~/.android/play-service-account.json` and `chmod 600` it. Never commit it.
3. Play Console → **Users and permissions → Invite new users** → the service account's email →
   **App permissions** → add the app → allow *Release to production, exclude devices, and use
   Play App Signing* and *Release apps to testing tracks* → Invite.

A different key path can be given with `PLAY_SERVICE_ACCOUNT=/path/to/key.json ./release.sh`.
