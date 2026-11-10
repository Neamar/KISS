#!/usr/bin/env bash
# Cut a release: bump version, build signed AAB + APK, tag, push (F-Droid picks up the tag),
# optionally promote the current open-testing release to production, upload the new AAB to open testing,
# and create the GitHub release with the APK attached.
#
# Usage: ./release.sh patch|minor
#
# Required env:
#   KEYSTORE_FILE, KEY_ALIAS      release keystore (passwords are prompted if KEYSTORE_PASSWORD/KEY_PASSWORD are unset)
#   PLAY_CREDENTIALS              path to the Google Play service account JSON
# Optional env:
#   TELEGRAM_BOT_TOKEN, TELEGRAM_CHAT_ID   announce the release on Telegram
set -euo pipefail
cd "$(dirname "$0")"

bump=${1:-}
[[ $bump == patch || $bump == minor ]] || { echo "Usage: $0 patch|minor" >&2; exit 1; }
: "${KEYSTORE_FILE:?}" "${KEY_ALIAS:?}" "${PLAY_CREDENTIALS:?}"

[[ $(git branch --show-current) == master ]] || { echo "Not on master" >&2; exit 1; }
[[ -z $(git status --porcelain) ]] || { echo "Working tree not clean" >&2; exit 1; }
git fetch -q origin master
[[ $(git rev-parse HEAD) == $(git rev-parse origin/master) ]] || { echo "master is not in sync with origin/master" >&2; exit 1; }

old_code=$(sed -n 's/^ *versionCode \([0-9]*\)$/\1/p' app/build.gradle)
old_name=$(sed -n 's/^ *versionName "\(.*\)"$/\1/p' app/build.gradle)
IFS=. read -r major minor patch <<< "$old_name"
if [[ $bump == minor ]]; then minor=$((minor + 1)); patch=0; else patch=$((patch + 1)); fi
code=$((old_code + 1))
name="$major.$minor.$patch"

notes="fastlane/metadata/android/en-US/changelogs/$code.txt"
[[ -s $notes ]] || { echo "Missing $notes" >&2; exit 1; }
for f in fastlane/metadata/android/*/changelogs/"$code".txt; do
    (( $(wc -m < "$f") <= 500 )) || { echo "$f is over the 500 characters Play/F-Droid limit" >&2; exit 1; }
done

echo "Releasing $old_name ($old_code) -> $name ($code)"
cat "$notes"
read -rp "Continue? [y/N] " ok
[[ $ok == y ]] || exit 1
read -rp "Promote the current open testing release to production first? [y/N] " promote

if [[ -z ${KEYSTORE_PASSWORD:-} ]]; then read -rsp "Keystore password: " KEYSTORE_PASSWORD; echo; fi
if [[ -z ${KEY_PASSWORD:-} ]]; then read -rsp "Key password: " KEY_PASSWORD; echo; fi
export KEYSTORE_PASSWORD KEY_PASSWORD
ANDROID_PUBLISHER_CREDENTIALS=$(cat "$PLAY_CREDENTIALS")
export ANDROID_PUBLISHER_CREDENTIALS

sed -i "s/^\( *versionCode\) $old_code$/\1 $code/; s/^\( *versionName\) \"$old_name\"$/\1 \"$name\"/" app/build.gradle
trap 'git checkout -q app/build.gradle' ERR
./gradlew bundleRelease assembleRelease
trap - ERR

git commit -qm "$name" app/build.gradle
git tag "v$name"

# shortcut: only en-US release notes go to Play, as before; copy other languages once their codes match Play's
mkdir -p app/src/main/play/release-notes/en-US
cp "$notes" app/src/main/play/release-notes/en-US/default.txt
# Promote first: afterwards, --from-track beta would pick the new upload
[[ $promote != y ]] || ./gradlew promoteReleaseArtifact --from-track beta --promote-track production
./gradlew publishReleaseBundle --track beta

git push origin master "v$name"

apk="app/build/outputs/apk/release/KISS-v$name.apk"
cp app/build/outputs/apk/release/app-release.apk "$apk"
gh release create "v$name" --title "v$name" --notes-file "$notes" "$apk"

if [[ -n ${TELEGRAM_BOT_TOKEN:-} ]]; then
    # Strip @ from GitHub handles, Telegram would link them to unrelated Telegram users
    curl -fsS "https://api.telegram.org/bot$TELEGRAM_BOT_TOKEN/sendMessage" \
        --data-urlencode chat_id="${TELEGRAM_CHAT_ID:?}" \
        --data-urlencode text="KISS $name is out on GitHub: https://github.com/Neamar/KISS/releases/tag/v$name
Play Store and F-Droid will follow in the coming days.

$(sed 's/@//g' "$notes")" \
        --data-urlencode link_preview_options='{"is_disabled":true}' >/dev/null
fi
