<div align="center">

<img src="docs/assets/anonymous-keys-banner.png" alt="Anonymous Keys DPI" width="100%">

# Anonymous Keys DPI

### Open Internet access for Android in high-censorship environments

[![Android CI](https://github.com/anonymouskeys/Project-X/actions/workflows/android.yml/badge.svg)](https://github.com/anonymouskeys/Project-X/actions/workflows/android.yml)
[![Latest Release](https://img.shields.io/github/v/release/anonymouskeys/Project-X?display_name=tag&sort=semver)](https://github.com/anonymouskeys/Project-X/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/anonymouskeys/Project-X/total)](https://github.com/anonymouskeys/Project-X/releases)
[![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)](#requirements)
[![Telegram](https://img.shields.io/badge/Telegram-Anonymous%20Keys-26A5E4?logo=telegram&logoColor=white)](https://t.me/anonymouskeys)
[![License](https://img.shields.io/github/license/anonymouskeys/Project-X)](LICENSE)

**Anonymous Keys DPI** is a free and open-source Android privacy and connectivity
application built for people who live, travel, study, or work in countries where
Internet access is restricted by heavy filtering, Deep Packet Inspection (DPI),
network interference, or widespread censorship.

[Download the latest APK](https://github.com/anonymouskeys/Project-X/releases/latest)
·
[Join Telegram](https://t.me/anonymouskeys)
·
[Report an issue](https://github.com/anonymouskeys/Project-X/issues)

</div>

---

## About the project

Anonymous Keys DPI combines an Android VPN interface, **Xray-core**, a native
**ByeDPI** layer, local SOCKS chaining, routing controls, and practical connection
testing in one application.

The project is intended for environments where ordinary connections may be
blocked, throttled, reset, filtered, or inspected. Its goal is to make advanced
connectivity tools more accessible without hiding the underlying controls from
experienced users.

This repository began from open-source networking projects, but the current
application has its own Anonymous Keys identity, user interface, release
pipeline, application ID, release certificate, community links, and integrated
DPI-bypass workflow.

> Anonymous Keys DPI is not a commercial VPN service and does not provide a
> server subscription. Users connect with their own compatible configurations
> and infrastructure.

---

## Why Anonymous Keys DPI exists

Access to information should not depend on where a person lives.

In many regions, Internet censorship is not an occasional inconvenience. It is
part of everyday life. Websites, social platforms, news sources, messaging
services, and independent resources may be blocked or made unreliable through
DNS interference, IP filtering, traffic shaping, connection resets, and DPI.

Anonymous Keys DPI was created to help users in these environments:

- reach lawful information and communication services;
- use compatible Xray configurations from a single Android client;
- apply ByeDPI strategies when DPI interferes with a connection;
- test connectivity quickly and adjust settings without rebuilding the app;
- keep a transparent, auditable, open-source tool available to the community.

The project does not promise universal access. Network restrictions differ
between providers, regions, protocols, destinations, and time periods. A method
that works today may require adjustment tomorrow.

---

## Main features

### Integrated connectivity stack

- Android VPN service
- Xray-core integration
- Native ByeDPI / ciadpi support
- Local SOCKS chaining between Xray and ByeDPI
- tun2socks integration
- VPN socket protection
- Per-destination routing support

### DPI-bypass controls

- Built-in DPI bypass switch
- Automatic and manual ByeDPI profiles
- Strategy presets
- Split-position controls
- Remembered successful strategy groups
- Fast delay and connectivity testing

### Configuration tools

- Compatible Xray configuration handling
- JSON configuration import
- Subscription and profile management inherited from the client foundation
- Routing settings
- Log viewer
- Backup and restore tools

### Anonymous Keys experience

- Dark-only interface
- Purple and cyan visual identity
- Anonymous Keys dragon artwork
- Custom launcher icon and splash screen
- Branded navigation drawer
- Direct GitHub and Telegram links
- Officially signed APK releases
- Automatic GitHub Release publishing

---

## Architecture

A typical protected connection follows this path:

```text
Android applications
        │
        ▼
Android VPN / TUN
        │
        ▼
tun2socks
        │
        ▼
Xray-core
        │
        ▼
Local ByeDPI SOCKS layer
        │
        ▼
User-configured VPN / proxy server
        │
        ▼
Internet
```

ByeDPI is used as a local transport-processing layer. It does not replace the
user's Xray server configuration.

---

## Download and installation

Official builds are published in
[GitHub Releases](https://github.com/anonymouskeys/Project-X/releases).

Download:

```text
Anonymous_keys_dpi.apk
```

A checksum is published beside every automated release:

```text
Anonymous_keys_dpi.apk.sha256
```

### Installation steps

1. Open the latest release.
2. Download `Anonymous_keys_dpi.apk`.
3. Allow installation from the browser or file manager when Android asks.
4. Install the APK.
5. Import or create a compatible connection profile.
6. Review the routing and DPI settings before connecting.

### Updating from older builds

Anonymous Keys DPI now uses the official Anonymous Keys signing certificate.

Android only allows an installed application to be updated by another APK signed
with a compatible certificate. If an older build was signed with a different
certificate, Android may require you to uninstall it before installing the
official release.

Export important configurations before uninstalling an older build.

All future official Anonymous Keys updates are intended to use the same release
key.

---

## Verify a release

### Verify SHA-256

On Linux, macOS, or Termux:

```bash
sha256sum Anonymous_keys_dpi.apk
cat Anonymous_keys_dpi.apk.sha256
```

The hashes must match exactly.

### Verify the APK signature

With Android SDK Build Tools installed:

```bash
apksigner verify --verbose --print-certs Anonymous_keys_dpi.apk
```

Only download release files from the official repository:

```text
https://github.com/anonymouskeys/Project-X
```

---

## Quick start

1. Install the latest official APK.
2. Open Anonymous Keys.
3. Import your compatible Xray configuration or subscription.
4. Select a profile.
5. Open DPI settings when your network requires bypass strategies.
6. Start with an automatic or moderate preset.
7. Run a delay test.
8. Connect.
9. Review the log if the connection fails.

DPI behavior is network-specific. Avoid assuming that the strongest preset is
always the best one. More aggressive fragmentation or manipulation may reduce
performance or break otherwise functional destinations.

---

## Requirements

- Android 8.0 or newer
- A compatible Xray configuration or subscription
- Network access
- Permission to create an Android VPN connection
- A server or service you are authorized to use

Some features and protocols may depend on the included native libraries, Android
version, device vendor, and server configuration.

---

## Telegram community

Development news, release announcements, project updates, and community
information are published in the official Telegram channel:

### [Join the Anonymous Keys Telegram channel](https://t.me/anonymouskeys)

```text
https://t.me/anonymouskeys
```

Please use GitHub Issues for reproducible technical bugs. Do not post private
keys, subscription links, server credentials, passwords, or sensitive logs in
public channels.

---

## Support the creator

Anonymous Keys DPI is developed and maintained with personal time and resources.

Voluntary donations can help with:

- development and maintenance;
- Android testing devices;
- server and infrastructure expenses;
- debugging across different networks;
- documentation and translations;
- future privacy and anti-censorship features.

Donations are optional and do not unlock paid features.

### TON

Send only assets supported by the TON wallet to this address:

```text
UQCezHtAYYkC0eJW26rkXgdT4fG9f9m6m-3oQanpd4bpyrEG
```

### USDT on TRON (TRC20)

Send only **USDT using the TRC20 network** to this address:

```text
TYUFWzRdicVgUgAf5HCPTVGHr6J7p2Kxrf
```

> Always verify the address and network before sending. Cryptocurrency
> transactions are generally irreversible. Sending an unsupported asset or
> using the wrong network can permanently lose funds.

Thank you for supporting the continued development of Anonymous Keys. ❤️

---

## Privacy and security notes

- Anonymous Keys DPI is a client application.
- The project does not operate or guarantee the privacy of third-party servers.
- A server operator may be able to observe connection metadata or unencrypted
  traffic reaching that server.
- Use encrypted protocols and infrastructure you trust.
- Review imported JSON and subscription sources before using them.
- Do not install APK files from unofficial mirrors unless you independently
  verify their signature and checksum.
- Logs may contain hostnames, IP addresses, profile names, or diagnostic details.
  Review them before sharing.

No networking application can guarantee anonymity by itself. Privacy depends on
the complete system: device, configuration, server, protocol, DNS behavior,
applications, browser state, accounts, and user behavior.

---

## Responsible and lawful use

Anonymous Keys DPI is intended for legitimate privacy, interoperability,
research, communication, and access to information.

Users are responsible for understanding and complying with laws, regulations,
contracts, workplace policies, school policies, and service terms applicable to
them.

The maintainers do not encourage unlawful access, attacks, credential theft,
fraud, harassment, malware distribution, or interference with systems and
networks.

---

## Troubleshooting

### Android refuses to install an update

The installed app may have been signed with another certificate. Export your
configuration, uninstall the old build, and install the official release.

### The VPN connects but websites do not open

Check:

- whether the profile itself works;
- DNS and routing settings;
- server availability;
- the selected ByeDPI preset;
- the application log;
- whether the destination is blocked by a different method.

### A stronger DPI preset performs worse

DPI bypass is not a simple strength scale. Different strategies alter traffic in
different ways. Use the least disruptive working preset.

### GitHub or Telegram does not open on the current network

The service itself may be filtered. Try a working connection, another network,
or a trusted access method available in your region.

### The delay test fails

A failed test may indicate server failure, routing problems, blocked test
destinations, DNS issues, or an incompatible DPI strategy. It does not always
mean the entire profile is unusable.

---

## Building from source

The Android project is located in:

```text
V2rayNG/
```

The complete GitHub Actions build also compiles native components and the Xray
Android library before running Gradle.

Important parts include:

```text
AndroidLibXrayLite/        Xray Android library
V2rayNG/                   Android application
compile-tun2socks.sh       tun2socks build
compile-byedpi.sh          ByeDPI native build
.github/workflows/         CI, signing, and release automation
```

Official CI currently uses Java 17, Android NDK r27, Go, gomobile, and Gradle.

The signed F-Droid universal artifact is produced as:

```text
V2rayNG/app/build/outputs/apk/fdroid/debug/Anonymous_keys_dpi.apk
```

Release signing secrets are not stored in the repository.

---

## Automated releases

Official releases are created from version tags:

```bash
git tag -a v1.0.1 -m "Anonymous Keys DPI v1.0.1"
git push origin v1.0.1
```

For a matching `v*` tag, GitHub Actions:

1. builds native tun2socks;
2. builds native ByeDPI;
3. builds the Xray Android library;
4. restores the signing key from encrypted repository secrets;
5. builds the APK;
6. verifies its signature;
7. calculates SHA-256;
8. creates or updates the GitHub Release;
9. uploads the APK and checksum.

Never move or reuse a published release tag.

---

## Contributing

Contributions are welcome.

Useful contributions include:

- reproducible bug reports;
- Android compatibility fixes;
- translations;
- documentation improvements;
- accessibility improvements;
- carefully tested DPI presets;
- UI and UX improvements;
- build-system maintenance;
- security reviews.

Before submitting an issue:

1. Check existing issues.
2. Test the latest release.
3. Remove private information from logs.
4. Describe the Android version, device, network type, profile type, selected DPI
   settings, and exact result.
5. Include clear reproduction steps.

Before submitting a pull request:

1. Keep changes focused.
2. Explain why the change is needed.
3. Avoid committing credentials, keystores, subscriptions, or generated build
   output.
4. Confirm that the project builds.
5. Describe how the change was tested.

---

## Project roadmap

Possible future directions include:

- improved automatic DPI strategy selection;
- clearer diagnostics and guided troubleshooting;
- additional routing controls;
- better import and export workflows;
- improved accessibility;
- more translations;
- expanded documentation;
- broader device and Android-version testing;
- carefully evaluated support for additional anti-censorship transports;
- desktop companion tools where appropriate.

Roadmap items are not guarantees and may change based on testing, security,
maintenance cost, and community feedback.

---

## Frequently asked questions

### Is Anonymous Keys DPI a VPN provider?

No. It is an Android client. You need a compatible configuration and server or
service.

### Does ByeDPI replace Xray?

No. In this project, ByeDPI is integrated into the local connection chain and
works alongside Xray.

### Does the application guarantee access on every network?

No. Censorship systems and provider behavior vary. Configuration and strategy
selection may need adjustment.

### Is the application anonymous?

The application alone cannot guarantee anonymity. See the privacy and security
notes above.

### Why is the APK named `Anonymous_keys_dpi.apk`?

The filename is part of the independent Anonymous Keys release identity and is
used consistently by the automated release workflow.

### Where should I ask for updates and announcements?

Join the official Telegram channel:

```text
https://t.me/anonymouskeys
```

### Where should I report bugs?

Use GitHub Issues:

```text
https://github.com/anonymouskeys/Project-X/issues
```

---

## Credits

Anonymous Keys DPI exists because of the work of many open-source developers.

The project includes or builds upon components and ideas from:

- [v2rayNG](https://github.com/2dust/v2rayNG)
- [Xray-core](https://github.com/XTLS/Xray-core)
- [ByeDPI](https://github.com/hufrea/byedpi)
- tun2socks and BadVPN-related components
- Android, Go, Kotlin, Gradle, and their open-source ecosystems

Please respect the licenses and attribution requirements of all included
components.

---

## License

See the repository's [`LICENSE`](LICENSE) file and the license files included
with individual third-party components.

Different bundled components may be distributed under different compatible
licenses. Their original notices remain authoritative for those components.

---

<div align="center">

## Anonymous Keys

**Freedom · Privacy · Open Internet**

[Telegram](https://t.me/anonymouskeys)
·
[Releases](https://github.com/anonymouskeys/Project-X/releases)
·
[Issues](https://github.com/anonymouskeys/Project-X/issues)

</div>
