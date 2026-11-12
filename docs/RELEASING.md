# GitHub APK releases

Tag pushes build and publish an installable, signed APK. Thumbscript's signing
key is stored only as GitHub Actions secrets. Never commit a keystore or send
signing credentials in Discord.

## One-time setup

In `Settings → Secrets and variables → Actions`, add:

- `RELEASE_KEYSTORE_BASE64` — base64 encoding of Thumbscript's `.jks` file
- `RELEASE_STORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`

Generate a long-lived key on a trusted computer; keep an encrypted offline
backup. Example (choose your own alias and passwords; never paste them into chat
or commit them):

```sh
keytool -genkeypair -v -keystore thumbscript-release.jks -alias thumbscript \
  -keyalg RSA -keysize 4096 -validity 10000
base64 -w0 thumbscript-release.jks
```

Upload the base64 output and matching alias/password values directly into this
repository's GitHub Actions secrets. Do not reuse the Untidy keystore. Losing
the signing key prevents future in-place APK updates.

## Release procedure

1. Update `versionName` and increment `versionCode` in `app/build.gradle.kts`.
2. Merge the release commit to `main`.
3. Create and push an exact matching `v<versionName>` tag, e.g. `v0.6.2`.
4. Actions verifies the tag/version match, builds `:app:assembleRelease`, checks
   the APK signature, generates `SHA256SUMS.txt`, and publishes both assets on
   the GitHub Release. Hyphenated versions (beta/rc) are marked prerelease.

The workflow fails before building if required secrets are missing. Untagged
main builds continue using the developer's existing local signing behavior and
do not publish.

## Signing migration warning

The existing public APK (`Still Scriptures v0.6.1`) was produced before this
workflow and signed with the previous debug certificate. The original private
debug keystore is not present on this build host. The new Thumbscript release
key therefore will **not** update that installation in place. For the first
release signed by the new key, users must uninstall the old build before
installing the new APK; this removes local data. After that one-time migration,
later releases signed with the same key install as upgrades and preserve data.
Back up saved reading data before uninstalling. Do not publish the first
new-key APK until you accept this migration.

This produces sideloadable GitHub releases, not Google Play releases. It is not
a Play App Signing key and does not replace Play Console publishing.
