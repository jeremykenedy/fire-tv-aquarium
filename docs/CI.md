# Continuous integration

## GitHub Actions

| Workflow | Checks | Credentials |
|---|---|---|
| Tests | Java swimming/configuration tests, installer tests on macOS and Linux with Python 3.10 and 3.13; Android build; signature and APK privacy verification | None |
| Code style | Google Java Format in AOSP style, Ruff lint/format, ShellCheck | None |
| Documentation | Local links, contents, banner XML, MIT license wording, immutable action pins | None |
| Security | Bandit installer analysis and Gitleaks source/history scan | None |
| GitGuardian scan | The same GitGuardian service used in the Fire TV tooling repo | `GITGUARDIAN_API_KEY`, enabled explicitly |
| SonarQube Cloud Scan | Java/Python analysis with compiled Android classes and installer coverage | `SONAR_TOKEN`, enabled explicitly |

Actions use full commit SHA pins. The Java formatter and Gitleaks releases are
pinned and checked against their published SHA-256 digests before execution.
Python development tools are version-pinned. Dependabot watches action pins and
Python development dependencies. No development tool is bundled in the APK.

The installer test matrix retains Python 3.10 and 3.13 on Linux and macOS.
macOS jobs use GitHub's standard `macos-15-intel` image to avoid the ARM runner
capacity cancellations observed during this release; local ARM64 macOS and
Android TV emulator checks are documented separately. Android build-tool
installation uses Ubuntu's main archive mirror with bounded retries, download
timeouts, and a ten-minute step limit. Package signature checks remain enabled.

For dependency PRs, compare the full diff, verify action SHAs against official
releases, and check CI for the exact reviewed head. A passing check that installs
an older tool does not validate a proposed update. Ruff's dependency pin,
`pyproject.toml` required version, and style-workflow install must agree. Re-run
checks after a repair or head change before approving and merging.

Run the local checks with JDK 21, Python, and ShellCheck installed:

```bash
python3 -m pip install -r requirements-dev.txt
bash test.sh
bash scripts/check-style.sh
python3 scripts/check-docs.py
bandit -q -ll install.py
bash scripts/check-secrets.sh
bash build.sh
python3 check_apk.py
```

## External services

This private repo does not inherit another repository's secrets or service
registration. External checks require access to this repository and a service
plan supporting private projects. Configuration files alone do not establish
an active service or a passing quality gate.

### GitGuardian

Add `GITGUARDIAN_API_KEY` under repository Actions secrets, then set the Actions
variable `GITGUARDIAN_ENABLED` to `true`. The workflow fails if enabled without
credentials. Run it manually once and verify the result. Secret scanning with
Gitleaks already runs independently of this integration.

### SonarQube Cloud

Import this repository into the existing `jeremykenedy-12345` organization,
retaining private project visibility. At setup, the organization's OSS plan
allowed public projects only; private analysis requires a compatible plan.
Add an analysis token as the Actions
secret `SONAR_TOKEN`, then set `SONAR_ENABLED` to `true`. The workflow compiles
Android classes before analysis and supplies the SDK library to SonarJava.
Installer coverage comes from the actual Python tests; renderer device checks
are documented separately and are not represented as synthetic line coverage.

### Scrutinizer and Codacy

Import the repository into the respective existing accounts with private-repo
access. Their repository configuration excludes generated build output and
binary media, while retaining application code for analysis. Scrutinizer must
use the triggering checkout rather than cloning another branch's `main`.

### Aikido

Add this repository to the existing GitHub integration in the Aikido dashboard
if the account supports private repository access. The app has no third-party
runtime dependencies, but source and development dependency scanning remain
useful. Verify the repository's own dashboard before adding its badge.

### StyleCI

This project uses Java and Python. Java formatting runs in the Code style
workflow. StyleCI's Python support requires a compatible multi-language plan;
its open-source PHP configuration does not cover this app. Enable the project
only if the existing account supports it, and use this repository's own ID for
any StyleCI badge.

External scan jobs stay explicitly disabled until credentials and access are
configured. A disabled job is not a completed scan. Add provider status badges
only after the specific repository integration is active; do not reuse badge
IDs from another project.

The downloads badge identifies the private repository rather than showing an
unavailable public download count. The release badge records the current version
and is updated with each release. GitHub workflow badges link to this
repository's own checks.

## Release signing

CI creates a temporary local signing key for build verification. Release APKs
are built with the stable private application key and uploaded with their
checksum. See [Releasing](RELEASING.md).
