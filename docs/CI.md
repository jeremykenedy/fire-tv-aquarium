# Continuous integration

## GitHub Actions

| Workflow | Checks | Credentials |
|---|---|---|
| Tests | Java swimming/configuration and Python tooling tests on macOS and Linux with Python 3.10 and 3.13; Android build; signature and APK privacy verification; 100% Java/Python coverage gate | None for checks; `CODACY_PROJECT_TOKEN` for explicitly enabled reporting |
| Code style | Google Java Format in AOSP style, Ruff lint/format, ShellCheck | None |
| Documentation | Local links, contents, banner XML, MIT license wording, immutable action pins | None |
| Security | Strict development dependency vulnerability audit, Bandit installer analysis, and Gitleaks source/history scan | None |
| GitGuardian scan | The same GitGuardian service used in the Fire TV tooling repo | `GITGUARDIAN_API_KEY`, enabled explicitly |
| SonarQube Cloud Scan | Java/Python analysis with compiled Android classes, measured line/branch coverage, and processed quality gate verification | `SONAR_TOKEN`, enabled explicitly |

Actions use full commit SHA pins. The Java formatter and Gitleaks releases are
pinned and checked against their published SHA-256 digests before execution.
Their downloads and redirects are restricted to HTTPS.
Python development tools are version-pinned. Dependabot watches action pins and
Python and Gradle development dependencies. No development tool is bundled in
the APK.

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
python3 -m pip install pip-audit==2.10.1
python3 -m pip_audit --strict -r requirements-dev.txt
bash scripts/check-secrets.sh
bash build.sh
python3 check_apk.py
bash scripts/test-coverage.sh
```

The Android build job runs the isolated coverage project in `coverage/`.
Gradle 8.14.3 is downloaded over HTTPS and checked against a pinned SHA-256
before use. JUnit and Robolectric execute the real Android application classes
with platform shadows; JaCoCo writes `build/coverage/jacoco.xml` and an HTML
report under `build/coverage/java-html/`. Only generated `R` and `BuildConfig`
classes are excluded. Every source file under `src/` must appear in the report,
with zero missed lines and branches. Test dependencies never enter the
production build, which still uses `build.sh`.

The same runner creates a local Python test environment and measures
`install.py`, `check_apk.py`, and every Python script under `scripts/`.
It requires 100% line and branch coverage and verifies the XML report contains
every expected source file. Fixtures exercise disconnected devices, rollback,
invalid serials, package identity, permission violations, missing media,
cleartext and certificate trust policies, documentation errors, unsafe XML,
and mutable action pins. Production checks use explicit failures and also run
under optimized Python, where language assertions are disabled.
Shell scripts receive ShellCheck
and functional CI execution; they are not assigned synthetic coverage results.
Device checks remain necessary for decoder performance and actual display output.

## External services

This public repo does not inherit another repository's secrets or service
registration. External checks still require service registration and any
credentials used by their workflows. Configuration files alone do not establish
an active service or a passing quality gate.

### GitGuardian

Add `GITGUARDIAN_API_KEY` under repository Actions secrets, then set the Actions
variable `GITGUARDIAN_ENABLED` to `true`. The workflow fails if enabled without
credentials. This integration is enabled for this repository. Its authenticated
manual scan completed without secret findings. It scans the commits selected
for each CI event; Gitleaks independently scans source and history.

### SonarQube Cloud

The public project is registered in the `jeremykenedy-12345` organization with
project key `jeremykenedy_fire-tv-aquarium`. Add an analysis token as the Actions
secret `SONAR_TOKEN`, then set `SONAR_ENABLED` to `true`. The workflow compiles
Android classes before analysis and supplies the SDK library to SonarJava.
Java coverage is imported from the JaCoCo XML report, and Python coverage from
`coverage.xml`. Both come from the strict coverage runner used by the core Tests
job. Python coverage selects files by path because some tests load them with
`importlib` under descriptive module names. Renderer device checks are
documented separately and do not manufacture coverage data.

View analysis results and quality gates in this repository's
[SonarQube Cloud project](https://sonarcloud.io/dashboard?id=jeremykenedy_fire-tv-aquarium).
The scanner waits up to five minutes for the processed quality gate and fails
the job if it does not pass. Check the result for the same branch or pull request
and commit when reviewing a change.
The README includes separate Sonar badges for Actions upload status, the
processed quality gate, and measured overall coverage.
Manual runs explicitly pass the selected branch to Sonar so a branch scan does
not replace the `main` result. Push and pull request runs use automatic detection.

Both integrations are enabled for main pushes, manual runs, and pull requests
from branches in this repository. Their authenticated Actions jobs skip fork
and Dependabot pull requests because those events cannot use repository Actions
secrets. Tests, code style, documentation, Bandit, and Gitleaks still run on those
pull requests. Merged changes are scanned by both services on `main`. These jobs
do not use `pull_request_target` to expose credentials to pull request code.

### Scrutinizer and Codacy

Import the public repository into the respective existing accounts.
Their repository configuration excludes generated build output and
binary media, while retaining application code for analysis. Scrutinizer must
use the triggering checkout rather than cloning another branch's `main`.
Codacy is registered for this repository and has completed its initial source
analysis. Its README badge uses this project's own ID. Source findings remain
visible in the dashboard; registration does not imply they are all resolved.
The Tests workflow can upload the real Java and Python coverage reports using
a repository-scoped `CODACY_PROJECT_TOKEN` secret and `CODACY_ENABLED=true`.
The reporter is pinned to version 14.1.3 with its published SHA-256 verified
before execution. Java paths receive the actual `src/` prefix; Python paths
come directly from coverage.py. Both reports are finalized for the checked-out
commit, and fork and Dependabot pull requests cannot access this secret.
This reporting feature is available for public open-source repositories.
Scrutinizer import is currently blocked by gateway and third-party service
errors on its repository import page. The README includes the requested build
and quality badge URLs for this repository's `main` branch. They may be
unavailable until the provider completes registration; they do not establish
a successful build or quality rating. This is a provider import failure,
not a confirmed paid-plan restriction.

### CodeFactor

The public repository is registered with CodeFactor. Its dashboard and README
badge use this repository's own URL. CodeFactor analyzes Java, Python, shell,
and repository configuration separately from the local formatting checks.

### Aikido

This repository is registered in the existing GitHub integration as
[Aikido repository 3327480](https://app.aikido.dev/repositories/3327480).
The README's Aikido badge links directly to that dashboard. The app has no
third-party runtime dependencies, but source, manifest, development dependency,
secret, and license scans remain useful. The badge identifies the integration;
it does not claim that no findings remain.
Branch Quick Scan requires a paid Aikido plan and is omitted. This integration
uses the scans available for the repository's configured `main` branch; no
paid branch-scan job or upgrade is required.

The launcher activity and screensaver service intentionally remain exported.
Android must be able to open the TV launcher and bind the screensaver. The
service requires the system `android.permission.BIND_DREAM_SERVICE` permission;
the activity uses saved local preferences and does not consume intent extras.
The Android coverage suite verifies those two entry points and their permission
boundary. Aikido's existing findings for these required exports are classified
individually with that evidence. Future exports and other manifest findings
remain scanned.

### StyleCI

This project uses Java and Python. StyleCI's free open-source plan covers PHP;
Python support requires a paid multi-language plan. It is omitted, along with
its badge. Java and Python formatting run in the Code style workflow.

Integrations or features unavailable on the existing plan are omitted rather
than added as skipped checks. No paid upgrades are required by this project.
PHP, Laravel, Packagist, and other unrelated badges are not included.

The two authenticated scan jobs are controlled by their repository variables;
set a variable to `false` to disable its job. A disabled or skipped job is not
a completed scan. Add provider status badges
only for this repository; do not reuse badge IDs from another project. A pending
provider registration must be documented rather than presented as passing.

The Sponsor badge links to `https://github.com/sponsors/jeremykenedy`.
GitHub sanitizes README HTML, so it uses a linked image rather than an iframe.

The downloads badge shows the total number of release asset downloads, including
APK and checksum files across all releases. It does not count installations or
active users. The release badge follows the latest stable GitHub release
automatically. Both use public repository data without embedded credentials.
GitHub workflow badges link to this repository's own checks.

## Release signing

CI creates a temporary local signing key for build verification. Release APKs
are built with the stable private application key and uploaded with their
checksum. See [Releasing](RELEASING.md).
