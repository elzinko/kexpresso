# Publishing to Maven Central

This guide walks the maintainer through enabling **Maven Central** publishing for kexpresso,
from a one-time account/namespace/GPG setup to cutting a release. Once it's set up, every
tagged release publishes automatically from the `Release` workflow
(`.github/workflows/release.yml`).

Kexpresso publishes through the **Vanniktech Maven Publish plugin** to the new **Sonatype
Central Portal** (`central.sonatype.com`), with GPG-signed artifacts. Consumers then depend on
`io.github.elzinko:kexpresso:<version>` with **no token** and no extra repository config.

> You only need to do steps 1–4 **once**. After that, releasing is just step 6 (tag a release).

---

## Overview of the secrets

The release workflow reads four GitHub Actions repo secrets and maps them to the
`ORG_GRADLE_PROJECT_*` Gradle properties the plugin expects:

| GitHub secret                     | Maps to (`ORG_GRADLE_PROJECT_*`)        | What it is                                   |
| --------------------------------- | --------------------------------------- | -------------------------------------------- |
| `MAVEN_CENTRAL_USERNAME`          | `mavenCentralUsername`                  | Central Portal **user token** username       |
| `MAVEN_CENTRAL_PASSWORD`          | `mavenCentralPassword`                  | Central Portal **user token** password       |
| `SIGNING_IN_MEMORY_KEY`           | `signingInMemoryKey`                    | ASCII-armored GPG **private** key            |
| `SIGNING_IN_MEMORY_KEY_PASSWORD`  | `signingInMemoryKeyPassword`            | Passphrase for that GPG key                  |

Until `MAVEN_CENTRAL_USERNAME` exists, the "Publish to Maven Central" step is **skipped** (it's
guarded by `if: ${{ env.MAVEN_CENTRAL_USERNAME != '' }}`), so the release still succeeds and
GitHub Packages publishing is unaffected.

---

## 1. Create a Central Portal account and verify the `io.github.elzinko` namespace

1. Sign up / log in at <https://central.sonatype.com/> (you can sign in with GitHub).
2. Go to **Namespaces** and click **Add Namespace**. Enter **`io.github.elzinko`**.
   - Central uses the `io.github.<username>` convention for GitHub-hosted projects. This is why
     kexpresso's `group` was changed from `com.github.elzinko` to `io.github.elzinko`.
3. Central shows a **verification key** (a random string) and asks you to prove you own the
   GitHub account. The standard method: **create a public GitHub repository** named exactly
   the verification key it gives you (e.g. `https://github.com/elzinko/<verification-key>`).
4. Back on Central, click **Verify Namespace**. Once verified, the temporary repo can be
   deleted. The namespace stays verified.

---

## 2. Generate a GPG signing key

Maven Central requires every artifact (and its checksums) to be GPG-signed.

```bash
# Generate a key (choose RSA, 4096 bits, no expiry or a long one; set a passphrase).
gpg --full-generate-key

# List keys to find the key id (the long hex after `sec  rsa4096/`):
gpg --list-secret-keys --keyid-format=long
# Example output: sec   rsa4096/ABCD1234EF567890 2026-06-06 [SC]

# Publish the PUBLIC key to a keyserver so Central can verify signatures:
gpg --keyserver keyserver.ubuntu.com --send-keys ABCD1234EF567890
# (optionally also: keys.openpgp.org)
```

### Export the private key for `SIGNING_IN_MEMORY_KEY`

The plugin uses an **in-memory** ASCII-armored private key. Export it:

```bash
gpg --armor --export-secret-keys ABCD1234EF567890
```

Copy the **entire** block, including the
`-----BEGIN PGP PRIVATE KEY BLOCK-----` / `-----END PGP PRIVATE KEY BLOCK-----` lines and the
blank lines. GitHub Actions secrets preserve newlines, so paste it **verbatim** (multi-line) —
no need to collapse it to `\n`. That whole block is the value of the `SIGNING_IN_MEMORY_KEY`
secret; the passphrase you chose is `SIGNING_IN_MEMORY_KEY_PASSWORD`.

---

## 3. Generate a Central Portal user token

1. On <https://central.sonatype.com/>, open your **Account** → **Generate User Token**.
2. Central returns a **username** and **password** pair (these are token credentials, not your
   login). These become `MAVEN_CENTRAL_USERNAME` and `MAVEN_CENTRAL_PASSWORD`.

---

## 4. Add the four GitHub repo secrets

In the repository: **Settings → Secrets and variables → Actions → New repository secret**.
Add all four with these **exact** names:

- `MAVEN_CENTRAL_USERNAME`
- `MAVEN_CENTRAL_PASSWORD`
- `SIGNING_IN_MEMORY_KEY`
- `SIGNING_IN_MEMORY_KEY_PASSWORD`

---

## 5. (Optional) Test locally before releasing

You can validate everything except the actual upload without any credentials:

```bash
# Builds + runs the gates (detekt, kover) — succeeds with NO credentials, unsigned.
./gradlew build

# Installs all artifacts to ~/.m2 under io/github/elzinko/kexpresso* — unsigned.
./gradlew publishToMavenLocal
```

To test a **signed** local install (with your GPG key), pass the signing properties:

```bash
ORG_GRADLE_PROJECT_signingInMemoryKey="$(gpg --armor --export-secret-keys ABCD1234EF567890)" \
ORG_GRADLE_PROJECT_signingInMemoryKeyPassword="<passphrase>" \
  ./gradlew publishToMavenLocal
```

> A full Central dry run requires the account + token and can only be done by actually
> uploading to a staging repository (step 6).

---

## 6. Cut a release

Releases are tag-driven (see `.github/workflows/release.yml`):

```bash
git tag v0.7.0
git push origin v0.7.0
```

The `Release` job (on `macos-latest`, the only host that can build the Apple/iOS targets) then:

1. Runs a JVM sanity test.
2. Publishes all targets to **GitHub Packages**.
3. **Publishes signed artifacts to Maven Central** (this step, once the four secrets exist).
4. Generates checksums, attests provenance, and creates the GitHub Release.

### README install-snippet version

The README's install snippets (Gradle, KMP `commonMain`, both Maven blocks, prose
coordinates) show a concrete version — they are kept honest by two mechanisms:

- **CI guardrail (`ci.yml`)** — every PR runs `scripts/sync-readme-version.sh
  --check <version-from-build.gradle.kts>`. If the README has drifted from the
  project version, the build fails. A stale README cannot reach `main`.
- **Auto-PR on release (`sync-readme-version.yml`)** — when a GitHub Release is
  published, this workflow opens a PR that updates the README to the released
  version. **Merge it with one click** — the CI guardrail will then pass for
  every subsequent PR.

  *Note on auto-merge:* pushes authenticated with the default `GITHUB_TOKEN`
  don't trigger workflows (a GitHub security feature), so CI does not run on
  the auto-PR. The auto-PR's body explains the situation; the simplest path is
  to push an empty commit (`git commit --allow-empty -m "trigger ci" && git
  push`) on the branch, which triggers CI and lets you squash-merge normally.

**Local one-liner during release prep** (recommended): after bumping
`version` in `build.gradle.kts`, run

```bash
scripts/sync-readme-version.sh "$(sed -nE 's/^version = .*\"([^"]+)\".*$/\1/p' build.gradle.kts | head -1)"
```

and include the README change in the same commit as the version bump.

The workflow runs **`publishAndReleaseToMavenCentral`**: it uploads the signed artifacts,
waits for the Central Portal's validation, and **auto-releases** on success — no manual
Portal click. Central's validation gates the release: if signing or POM metadata is invalid,
nothing is published — and because Central runs **before** the immutable GitHub Packages upload,
that step hasn't run either, so you can simply fix and re-tag the same version cleanly.

- Follow the staging → validation → published transition under
  <https://central.sonatype.com/> → **Deployments** if you wish.
- Prefer a manual review gate? Change the workflow step from
  `publishAndReleaseToMavenCentral` back to **`publishToMavenCentral`** — it stops at a staged
  deployment for you to review and **Publish** by hand on the Portal.

### Verify

- It typically takes a few minutes to ~30 minutes for a newly released version to appear at
  <https://central.sonatype.com/artifact/io.github.elzinko/kexpresso> and longer to be
  searchable on <https://search.maven.org/>.
- Smoke-test from a clean project:

  ```kotlin
  repositories { mavenCentral() }
  dependencies { implementation("io.github.elzinko:kexpresso:0.7.0") }
  ```

---

## Troubleshooting

- **`Cannot perform signing task … no configured signatory`** locally — you ran a publish task
  with no signing key set. Local `build` / `publishToMavenLocal` are intentionally unsigned;
  signing only activates when `signingInMemoryKey` is provided.
- **Central rejects the deployment** — common causes: namespace not verified (step 1), missing
  POM fields (name/description/url/license/developer/scm — all wired in `build.gradle.kts`), or
  the public key not on a keyserver (step 2).
- **GitHub Packages still works** — the release publishes to GitHub Packages via
  `publishAllPublicationsToGitHubPackagesRepository`, independently of the Central step.
