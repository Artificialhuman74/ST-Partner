#!/usr/bin/env bash
# One command from "this is ready" to "it's on Google Play":
#   1. asks Play which version codes are already taken and bumps the version past them
#   2. runs the unit tests and builds the signed release bundle (dist/ keeps a copy)
#   3. uploads it to a Play track and sends it for review
#   4. commits the version bump, tags it (v<versionName>) and pushes both
#
#   ./release.sh --notes "Live order tracking on the order page"
#   ./release.sh --track internal         production | beta (open) | alpha (closed, default) | internal
#   ./release.sh --rollout 20             staged rollout to 20% of users
#   ./release.sh --draft                  upload, then press "Roll out" yourself in Play Console
#   ./release.sh --name 8.0               choose the version name instead of bumping the last number
#   ./release.sh --no-upload              just build the bundle (add --bump for a new version)
#   ./release.sh --no-push                don't push the commit and tag to GitHub
#
# Uploading needs a Google Play service account key, read from $PLAY_SERVICE_ACCOUNT or
# ~/.android/play-service-account.json (one key works for every app in the account).
# Setup steps are in RELEASING.md.
set -euo pipefail

PACKAGE=com.sanathanatattva.delivery
AAB_PREFIX=st-delivery
TRACK=alpha # closed testing; switch to production once Play grants production access

cd "$(dirname "$0")"
GRADLE_FILE=app/build.gradle.kts
KEY_FILE=${PLAY_SERVICE_ACCOUNT:-$HOME/.android/play-service-account.json}
API=https://androidpublisher.googleapis.com/androidpublisher/v3/applications/$PACKAGE
UPLOAD_API=https://androidpublisher.googleapis.com/upload/androidpublisher/v3/applications/$PACKAGE
export JAVA_HOME=${JAVA_HOME:-/Applications/Android Studio.app/Contents/jbr/Contents/Home}

notes="" new_name="" status=completed rollout="" upload=1 push=1 bump=0 allow_dirty=0
while (($#)); do
  case $1 in
    --notes) notes=$2; shift ;;
    --track) TRACK=$2; shift ;;
    --name) new_name=$2; shift ;;
    --rollout) rollout=$2; status=inProgress; shift ;;
    --draft) status=draft ;;
    --no-upload) upload=0 ;;
    --no-push) push=0 ;;
    --bump) bump=1 ;;
    --allow-dirty) allow_dirty=1 ;;
    -h|--help) sed -n '2,20p' "${0##*/}" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) echo "Unknown option: $1 (see --help)" >&2; exit 2 ;;
  esac
  shift
done

step() { printf '\n\033[1;32m▸ %s\033[0m\n' "$*"; }
die() { printf '\n\033[1;31m✗ %s\033[0m\n' "$*" >&2; exit 1; }

((${#notes} <= 500)) || die "Release notes are ${#notes} characters; Play allows 500."
[[ -f keystore.properties ]] || die "keystore.properties is missing, so the bundle can't be signed."
if ((!allow_dirty)) && [[ -n $(git status --porcelain --untracked-files=no -- . ':!.idea') ]]; then
  die "There are uncommitted changes. Commit them first so the release matches the code (or pass --allow-dirty)."
fi

# ── Google Play API ────────────────────────────────────────────────────────────

WORK=$(mktemp -d)
EDIT=""
cleanup() {
  [[ -n $EDIT ]] && play DELETE "$API/edits/$EDIT" >/dev/null 2>&1 || true
  rm -rf "$WORK"
}
trap cleanup EXIT

b64url() { openssl base64 -A | tr '+/' '-_' | tr -d '='; }

# An access token from the service account key (a signed JWT, exchanged at Google's token endpoint).
play_login() {
  [[ -f $KEY_FILE ]] || die "No Play service account key at $KEY_FILE (see RELEASING.md), or use --no-upload."
  jq -r .private_key "$KEY_FILE" >"$WORK/key.pem"
  local now header claims sig
  now=$(date +%s)
  header=$(printf '{"alg":"RS256","typ":"JWT"}' | b64url)
  claims=$(jq -nc --arg iss "$(jq -r .client_email "$KEY_FILE")" --argjson iat "$now" \
    '{iss:$iss, scope:"https://www.googleapis.com/auth/androidpublisher", aud:"https://oauth2.googleapis.com/token", iat:$iat, exp:($iat+3600)}' | b64url)
  sig=$(printf '%s.%s' "$header" "$claims" | openssl dgst -sha256 -sign "$WORK/key.pem" | b64url)
  rm -f "$WORK/key.pem"
  TOKEN=$(curl -sS https://oauth2.googleapis.com/token \
    -d grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer -d assertion="$header.$claims.$sig" | jq -r '.access_token // empty')
  [[ -n $TOKEN ]] || die "Google refused the service account key in $KEY_FILE."
}

# play METHOD URL [curl args…] → response body; prints Play's own error message on failure.
play() {
  local method=$1 url=$2 out code
  shift 2
  out=$(curl -sS -X "$method" -H "Authorization: Bearer $TOKEN" -w $'\n%{http_code}' "$@" "$url") || return 1
  code=${out##*$'\n'}
  out=${out%$'\n'*}
  if [[ $code != 2* ]]; then
    PLAY_ERROR=$(jq -r '.error.message // empty' <<<"$out" 2>/dev/null || true)
    echo "Google Play said ($code): $PLAY_ERROR" >&2
    return 1
  fi
  printf '%s' "$out"
}

open_edit() { EDIT=$(play POST "$API/edits" -H 'Content-Type: application/json' -d '{}' | jq -r .id) && [[ -n $EDIT ]]; }

# ── Version ────────────────────────────────────────────────────────────────────

code=$(sed -nE 's/^[[:space:]]*versionCode = ([0-9]+).*/\1/p' "$GRADLE_FILE" | head -1)
name=$(sed -nE 's/^[[:space:]]*versionName = "([^"]+)".*/\1/p' "$GRADLE_FILE" | head -1)
[[ -n $code && -n $name ]] || die "Couldn't read versionCode / versionName from $GRADLE_FILE."

if ((upload)); then
  step "Checking which versions Play already has"
  play_login
  open_edit || die "Couldn't open a Play edit for $PACKAGE. Is the service account invited to this app?"
  taken=$(play GET "$API/edits/$EDIT/bundles" | jq '[.bundles[]?.versionCode] | max // 0') || die "Couldn't list the bundles on Play."
  play DELETE "$API/edits/$EDIT" >/dev/null || true
  EDIT=""
  echo "Highest version code on Play: $taken. This build: $code ($name)."
  ((code > taken)) || bump=1
  next_code=$((code > taken ? code : taken + 1))
else
  next_code=$((code + 1))
fi
[[ -n $new_name ]] && bump=1

if ((bump)); then
  code=$next_code
  name=${new_name:-$(awk -F. -v OFS=. '{ $NF = $NF + 1; print }' <<<"$name")}
  sed -i '' -E "s/^([[:space:]]*versionCode = )[0-9]+/\1$code/; s/^([[:space:]]*versionName = )\"[^\"]+\"/\1\"$name\"/" "$GRADLE_FILE"
fi
git rev-parse -q --verify "refs/tags/v$name" >/dev/null && die "Tag v$name already exists; pass --name to pick a new version name."
step "Releasing $name (version code $code)"

# ── Build ──────────────────────────────────────────────────────────────────────

step "Running unit tests and building the signed bundle"
./gradlew --quiet testDebugUnitTest bundleRelease || {
  ((bump)) && git checkout -- "$GRADLE_FILE"
  die "The build failed, so nothing was released."
}
mkdir -p dist
aab=dist/$AAB_PREFIX-$name-vc$code.aab
cp app/build/outputs/bundle/release/app-release.aab "$aab"
echo "Bundle: $PWD/$aab ($(du -h "$aab" | cut -f1 | tr -d ' '))"

if ((bump)); then
  git commit --quiet -m "Release $name (versionCode $code)" -- "$GRADLE_FILE"
  echo "Committed the version bump."
fi

if ((!upload)); then
  echo
  echo "Built only. Upload it in Play Console → Test and release → $TRACK → Create new release."
  exit 0
fi

# ── Upload ─────────────────────────────────────────────────────────────────────

step "Uploading to Google Play ($TRACK)"
play_login # the build can outlast a token's first minutes; a fresh one costs nothing
PLAY_ERROR=""

# One edit: upload the bundle, put it on the track, commit. A failed commit throws the edit away.
send_release() {
  local release
  open_edit || return 1
  play POST "$UPLOAD_API/edits/$EDIT/bundles?uploadType=media" \
    -H 'Content-Type: application/octet-stream' --data-binary @"$aab" --max-time 1200 >/dev/null || return 1
  release=$(jq -nc --arg name "$name" --arg code "$code" --arg status "$status" --arg notes "$notes" --arg rollout "$rollout" '
    { name: $name, versionCodes: [$code], status: $status }
    + (if $notes == "" then {} else { releaseNotes: [{ language: "en-US", text: $notes }] } end)
    + (if $rollout == "" then {} else { userFraction: (($rollout | tonumber) / 100) } end)')
  play PUT "$API/edits/$EDIT/tracks/$TRACK" -H 'Content-Type: application/json' \
    -d "$(jq -nc --arg track "$TRACK" --argjson release "$release" '{ track: $track, releases: [$release] }')" >/dev/null || return 1
  play POST "$API/edits/$EDIT:commit" >/dev/null || return 1
  EDIT=""
}

if ! send_release; then
  # Until an app's first release is published, Play takes only drafts.
  [[ $status != draft && $PLAY_ERROR == *"draft app"* ]] || die "Play didn't accept the release on the $TRACK track."
  play DELETE "$API/edits/$EDIT" >/dev/null 2>&1 || true
  echo "This app hasn't been published yet, so Play only takes drafts. Sending it as a draft."
  status=draft rollout=""
  send_release || die "Play didn't accept the release on the $TRACK track."
fi

case $status in
  draft) echo "Uploaded as a draft. Open Play Console → $TRACK and press Roll out." ;;
  inProgress) echo "Sent for review; it rolls out to $rollout% of users once approved." ;;
  *) echo "Sent for review; it goes live once Google approves it (usually a few hours)." ;;
esac

# ── Git ────────────────────────────────────────────────────────────────────────

git tag -a "v$name" -m "Release $name (versionCode $code) to $TRACK"
if ((push)) && git remote get-url origin >/dev/null 2>&1; then
  step "Pushing to GitHub"
  git push --quiet origin HEAD "v$name" || echo "Push failed; run: git push origin HEAD v$name" >&2
fi
step "Done: $name ($code) is on the $TRACK track"
