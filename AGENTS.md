# Project standards

- Keep the application offline. Do not add ads, analytics, tracking libraries, network permissions, or external runtime services.
- Preserve the Android package name and signing key so upgrades keep the installed app and its settings.
- Render at native 3840x2160 and verify the actual surface and display composition on a Fire TV.
- Test remote navigation, settings persistence, preview, idle activation, and exit on the device.
- Keep app code dependency-free and use Android platform APIs.
- Keep signing keys, device addresses, device backups, and build artifacts out of Git.
- Follow the readme-standards skill for any requested README work. Read the full skill before starting. Follow readme-banners and readme-badges for their respective work.
- Use descriptive branch names and plain language. Do not add tool branding, attribution notices, emojis, or em dashes.
