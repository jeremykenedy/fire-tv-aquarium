# Contributing

Keep changes scoped and use descriptive branch names. Read [AGENTS.md](AGENTS.md)
and preserve the package name, stable signing key, saved preferences, offline
operation, and remote behavior.

The app uses platform APIs and original meshes and shaders. Do not add network
permissions, runtime services, ad libraries, analytics, or tracking. Avoid adding
runtime dependencies for functionality already available in Android.

Run the commands in [Testing](README.md#testing) and [CI](docs/CI.md), then verify
behavior changes on the relevant Fire TV, Android TV, and Google TV environments.
Check both directions of remote navigation,
persistence after restarting the process, preview, mode switching, automatic
idle activation, remote exit, and actual native 4K composition where relevant.

Follow the existing Java, Python, shell, documentation, and banner conventions.
Keep signing keys and device state out of Git. Include relevant validation in
change descriptions. Report device-specific limits with the device model and
firmware and Android API version rather than claiming every TV is verified.
Separate emulator results from physical hardware results. For Random changes,
verify fixed choices, excluded versions, saved Random preferences, and refresh
on a new showing. Restore the first observed system settings after device tests.

Documentation changes should keep the README contents, installation examples,
settings tables, screenshots, and release notes consistent. Use the project's
README, badge, and banner skills where applicable. Dependabot updates must align
tool pins and validate the actual proposed version on the reviewed commit.
