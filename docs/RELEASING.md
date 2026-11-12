# Hardened APK publication

Future releases require a manual `workflow_dispatch` run on protected `main`.
The input is an exact `v<versionName>` tag whose commit is an ancestor of
reviewed `main`; a tag push alone never starts publication. The build produces
an unsigned optimized APK without signing secrets. The signing job runs no
repository code and uses the protected `release-signing` environment; it signs
and verifies the public signer identity, then generates `SHA256SUMS.txt`. A
separate protected `release-publish` job gets repository write permission. It
creates a new release only after all gates succeed and never replaces an
existing release or assets. Stable releases are not automatically promoted to
latest; hyphenated versions are marked prerelease.

## Owner setup required before any future release

These are required GitHub controls, not controls this local change installed.
Until they are configured, keep the workflow disabled in **Actions → Publish
signed Wear OS APK → … → Disable workflow**. Disable the historical workflow
before merging to prevent old refs/reruns from using its broad secret access.
Inspect and cancel any queued historical publishing runs; do not rerun them.

1. **Settings → Rules → Rulesets**: protect `main` with required pull requests,
   independent approvals, stale-review dismissal and required checks. Restrict
   direct pushes, force pushes, deletion and bypasses. Require review for
   `.github/workflows/`, Gradle files/wrapper and release/security docs (for
   example through CODEOWNERS plus required code-owner review). Ancestry alone
   does not prove review: this branch rule and the release reviewer do that.
2. Add a tag ruleset for `v*`, restrict creation to the trusted release operator,
   and prohibit updates/deletion and broad bypasses. Tags are inputs to the
   protected-main manual workflow; tag pushes do not publish. The preflight
   rejects a tag whose workflow file differs from the reviewed main version.
3. **Settings → Environments**: create `release-signing` and `release-publish`.
   Require an independent reviewer, prevent self-review and administrator
   bypass, and restrict deployment branches to `main` only. This is essential:
   it prevents an untrusted branch's modified workflow from accessing the
   signing or publishing environments. If the repository/plan cannot enforce
   these controls, leave publishing disabled and use a separately reviewed
   release process.
4. Put only these four secrets in **release-signing**, never repository or
   organization scope: `RELEASE_KEYSTORE_BASE64`, `RELEASE_STORE_PASSWORD`,
   `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`. Remove same-named repository
   secrets and any organization-secret access granted to this repository after
   privately transferring/checking the values. Otherwise unapproved old jobs
   could still access them. Do not put signing secrets in `release-publish`.
   Keep an encrypted offline key backup. No secrets belong in source or logs.
5. Before approval, independently inspect the dispatch on `main`, exact
   tag/commit and reviewed-main ancestry, along with workflow/Gradle changes,
   preflight/build results and artifact identity. Approve signing only for that
   reviewed revision. Approve publication only after signer verification and
   strict checksum/artifact-set validation succeed.
   Inspect existing releases first; a version already released must not be
   reused. Re-enable the workflow only once all controls are enforced.

The preflight has only read access and executes inline version/ancestry checks,
not Gradle or project scripts. The build has read access and no signing secrets.
The signing runner has no checkout, Gradle, repository write permission or
project scripts; secrets are scoped to its signing step. Upload/downloads are
same-run immutable Actions artifacts with one-day retention. These will be
created only by an authorized future workflow run; none were uploaded during
this cleanup. `release-publish` executes no project code. No cache is restored
into the signing or publishing jobs.

## Public signer identity and migration

The explicit expected certificate SHA-256 is:

```text
1556e4db1f0958784731d64f0ca2b2c76a4da3764c9942558bef715ebb9fd66d
```

Evidence: the public
[v0.6.1 APK](https://github.com/keylimesoda/thumbscript/releases/download/v0.6.1/Still-Scriptures-0.6.1-release.apk).
On 2026-11-12 a cached copy was checked against the release asset's GitHub API
`digest`; its SHA-256 was
`0815ea0bda00222ae8b19978660151f5b9f0afbf60ee23842a76c3ecec8aa4c7`.
Android build-tools 35.0.0 `apksigner verify --print-certs` succeeded and returned
the certificate digest above (subject: Android Debug). The APK file digest is
**not** the signer fingerprint. No private key was used for discovery.

The workflow requires exactly one signer with that certificate SHA-256. Merely
passing APK signature verification is insufficient. Missing signing secrets,
a new key or a local debug-key fallback cannot silently publish an incompatible
APK. No claim is made that the matching private key is available here.

If a dedicated new distribution key is needed, keep publishing blocked. On a
trusted machine, create and verify a signed candidate, record its **public**
certificate SHA-256 and evidence, then review a separate change to the explicit
expected fingerprint with migration approval. Never derive the expectation
from the same CI keystore at release time or guess it. A different certificate
usually requires uninstalling the existing sideloaded preview, losing private
local app data; communicate that migration and backup limits before publishing.
Existing releases and their assets stay untouched. This pipeline is for
sideloadable APKs; it does not establish Play App Signing or Store readiness.

## Future release procedure

After owner setup and independent review:

1. Change the literal `versionName` and increment `versionCode`; review/merge
   all changes to protected `main`.
2. The authorized operator creates a new exact `v<versionName>` tag at that
   reviewed commit and pushes it. Do not move or reuse release tags. The tag
   push itself does not run a publishing workflow. Only numeric
   `major.minor.patch` versions with optional alphanumeric prerelease segments
   are accepted; metadata/shell characters are rejected.
3. In **Actions → Publish signed Wear OS APK → Run workflow**, select `main`
   and enter the tag in `release_tag`. The workflow file runs from the current
   protected main commit, not from the tag; preflight rejects a stale/non-main
   dispatch, off-main tag, version mismatch, or workflow-file mismatch. It then
   builds the tagged commit without signing secrets. `RELEASE_UNSIGNED=true`
   disables Gradle signing only for this build; ordinary local signing behavior
   is preserved.
4. Review and approve the protected signing environment. The signing job also
   checks the APK's embedded version, verifies its cryptographic signature and
   compares its certificate digest to the explicit expectation.
5. Review and approve publishing. The job rechecks that the tag still resolves
   to the gated commit, checks the APK checksum and creates the release with
   its APK and `SHA256SUMS.txt`. A moved tag, failed gate, missing approval or
   existing release blocks publication. No clobber/upload-to-existing path exists.

## Pinned action provenance

All remaining actions use full commit SHAs. Each SHA was resolved from the
upstream tag via read-only GitHub commit API on 2026-11-12 and GitHub reported
`commit.verification.verified: true`:

| Upstream action/tag | Verified full SHA |
| --- | --- |
| `actions/checkout` v4.3.1 | `34e114876b0b11c390a56381ad16ebd13914f8d5` |
| `actions/setup-java` v4.7.1 | `c5195efecf7bdfc987ee8bae7a71cb8b11521c00` |
| `actions/upload-artifact` v4.6.2 | `ea165f8d65b6e75b540449e92b4886f43607fa02` |
| `actions/download-artifact` v4.3.0 | `d3f86a106a0bac45b974a628896c90dbdf5c8093` |

`android-actions/setup-android` and `softprops/action-gh-release` were removed.
All jobs use the `ubuntu-24.04` runner label. The signing job invokes the
pinned Java 17 setup, searches `ANDROID_HOME`, `ANDROID_SDK_ROOT`, then the
Ubuntu SDK fallback for `cmdline-tools/latest/bin/sdkmanager`, and fails if none
exists. It installs Android build-tools 35.0.0, verifies `aapt`, `zipalign` and
`apksigner` are executable, and persists the selected SDK root for signing.
The official [Ubuntu 24.04 runner inventory](https://github.com/actions/runner-images/blob/main/images/ubuntu/Ubuntu2404-Readme.md)
checked 2026-11-12 lists Android Command Line Tools 12.0 and build-tools 35.0.0;
this is inventory evidence, not a completed Actions run. Environment signing
secrets are explicitly mapped only in the signing step. GitHub CLI is used
only in the later publish job. These pins do not freeze the hosted runner
image revision, JDK patch, Gradle dependency graph or Android SDK command-line
tools distribution. Changes to these dependencies and action pins require review.

## Observed GitHub failure and access limits

Read-only GitHub queries confirm
[run 43301422499](https://github.com/keylimesoda/thumbscript/actions/runs/43301422499)
failed on `push` to `main` at `3187253e6639690e2084ce3ac4ff8b303c559de2`, with
no jobs or commit check-runs. The Actions summary explicitly reports:
`Invalid workflow file` at line 17, column 30: `Unrecognized named-value:
'runner'` in the expression `runner.temp`. The old workflow used
`${{ runner.temp }}` in job-level `env`; GitHub's [context-availability
table](https://docs.github.com/en/actions/reference/workflows-and-actions/contexts#context-availability)
does not allow the `runner` context at `jobs.<job_id>.env`. This parser error,
not missing secrets or a build/signing failure, explains the zero-job run. The
replacement workflow removes that expression. The original and replacement
YAML both pass the workflow JSON schema used for local validation; that does
not prove GitHub service acceptance. On 2026-11-12 the main-branch protection
endpoint returned `404 Branch not protected`, the repository rulesets list was
empty, no repository environments existed, the repository-level Actions
secrets list was empty, and the current workflow was active. Configure the
required protections and environments, then disable the historical workflow
before merging; keep it disabled until the new controls are verified.

## Local cleanup validation (2026-11-12)

Both worktrees started clean on their requested `security/release-guard-*`
branches. `actionlint` was not installed. Equivalent targeted validation used
duplicate-aware YAML 1.2 loading, Python jsonschema Draft 7 against
SchemaStore's GitHub workflow schema,
`bash -n` for every inline shell step, full-SHA action-reference checks and
`git diff --check`. All passed. The schema was retrieved read-only from
`SchemaStore/schemastore/src/schemas/json/github-workflow.json`; its SHA-256 was
`10f7b2dba224b461c81bbd288b07c9c64712f0d0df58ff0856d09b2431612d8b`.
The original workflows also passed this schema, so this check does not explain
the server-side zero-job rejection.

Targeted local fixtures executed the actual inline gates without credentials:
Untidy's pause notice exits 1; Thumbscript accepts a valid reviewed tag from
protected-main dispatch and rejects a wrong event/ref, malformed tag, app-version
mismatch, off-main tag, stale main SHA, workflow-file drift and duplicate version.
The signer check accepts exactly the pinned public certificate and rejects
wrong, multiple or missing signer identities. The publish gate accepts the exact
APK+checksum pair and rejects digest/name mismatches, multiple manifest lines or
an extra artifact. Static assertions confirm secret isolation, environment
assignments, publish dependencies, full action pins and the contents-write
boundary.

The full unsigned local `:app:assembleRelease` build passed (46 tasks) and
produced `app-release-unsigned.apk` (4,090,996 bytes). As expected for this
unsigned artifact, `apksigner verify` returned `DOES NOT VERIFY`; this does not
validate hosted-runner SDK provisioning or a signed release. No end-to-end
signing or GitHub service execution was performed. No credentials were printed,
decoded or reconstructed. No pushes, tags, workflow dispatches, artifact uploads
or release mutations were performed. GitHub protections and environment-secret
configuration require owner action; no workflow was dispatched, so local
validation cannot certify GitHub Actions runtime behavior.
