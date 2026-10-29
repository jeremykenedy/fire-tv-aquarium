# Continuous integration

## GitHub Actions

| Workflow | Checks | Credentials |
|---|---|---|
| Tests | Java swimming/configuration and Python tooling tests on macOS and Linux with Python 3.10 and 3.13; Android build; signature and APK privacy verification; 100% Java/Python coverage gate | None |
| Code style | Google Java Format in AOSP style, Ruff lint/format, ShellCheck | None |
| Documentation | Local links, contents, banner XML, MIT license wording, immutable action pins | None |
| Security | Strict development dependency vulnerability audit, Bandit installer analysis, and Gitleaks source/history scan | None |

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

## Deferred external services

GitGuardian, SonarCloud, Codacy, CodeFactor and Aikido are deferred. Their
workflows, reporting step, provider configuration and badges have been removed.
Builds, tests, lint, documentation, privacy and independent security checks remain.
