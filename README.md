<div align="center">
  <img src="docs/app-icon.svg" width="128" height="128" alt="App Version Patcher icon">

  <h1>App Version Patcher</h1>

  <p>Override application-reported version metadata at runtime with LSPosed.</p>

  <p>
    <a href="#requirements"><img alt="Android 8.1+" src="https://img.shields.io/badge/Android-8.1%2B-3DDC84?logo=android&amp;logoColor=white"></a>
    <a href="#requirements"><img alt="libxposed API 101" src="https://img.shields.io/badge/libxposed-API%20101-6C63FF"></a>
    <a href="https://github.com/igorcv88/AppVersionPatcher/actions/workflows/android.yml"><img alt="Build" src="https://img.shields.io/github/actions/workflow/status/igorcv88/AppVersionPatcher/android.yml?branch=main&amp;label=build"></a>
    <a href="https://github.com/igorcv88/AppVersionPatcher/releases/latest"><img alt="Release" src="https://img.shields.io/github/v/release/igorcv88/AppVersionPatcher?label=release"></a>
    <a href="https://github.com/igorcv88/AppVersionPatcher/releases"><img alt="Downloads" src="https://img.shields.io/github/downloads/igorcv88/AppVersionPatcher/total"></a>
    <a href="LICENSE"><img alt="License: PolyForm Noncommercial 1.0.0" src="https://img.shields.io/badge/license-PolyForm%20Noncommercial%201.0.0-5C4EE5"></a>
  </p>
</div>

## Features

- Per-application `versionName` and optional `versionCode` overrides.
- Standard Android `PackageManager` hooks.
- React Native support for apps using `react-native-device-info`.
- Dynamic LSPosed scope requests and remote preferences.
- No modification or resigning of target APKs.
- Signed, versioned APK releases.

## Requirements

- Android 8.1 or newer.
- LSPosed or another implementation compatible with libxposed API 101.
- The target application included in the module scope.

The configuration app does not request root access or network access.

## Installation

1. Download `AppVersionPatcher.apk` from the [latest release](https://github.com/igorcv88/AppVersionPatcher/releases/latest).
2. Install the APK and enable **App Version Patcher** in LSPosed.
3. Add the intended target applications to the module scope.
4. Restart the affected processes, or perform the restart required by your LSPosed implementation.

## Usage

1. Open **App Version Patcher** and select a target application.
2. Enter the desired `versionName`.
3. Optionally enter a `versionCode` from `0` through `2147483647`.
4. Enable **PackageManager**, **RNDeviceInfo**, or both.
5. Save the configuration.
6. Force-stop and reopen the target application.

Configured applications stay at the top of the list. Existing settings are preserved when the module is updated.

## Legitimate use cases

- Software development and QA.
- Compatibility and regression testing.
- Testing version-dependent migrations or feature gates.
- Validating Android and React Native version-reporting behavior.
- Interoperability research in controlled or authorized environments.
- Reproducing version-specific bugs without rebuilding a target APK.

## Compatibility

The `PackageManager` method covers applications that read their version from Android `PackageInfo`. The `RNDeviceInfo` method covers React Native applications that use `react-native-device-info`.

Applications using unrelated libraries, hardcoded values, or exclusively server-side checks may require different hook points.

## Root and injection considerations

App Version Patcher does not execute `su`, alter mount namespaces, change system properties, modify denylist settings, or disable root-concealment tools. Its own hooks only change version values in memory.

However, the target process must receive LSPosed injection for the module to work. Framework injection can itself be detectable, and allowing injection may conflict with a root-concealment policy:

- With Zygisk Next **Enforce DenyList**, applications in the denylist do not receive Zygisk or LSPosed modules.
- On KernelSU, **Unmount Modules** is used as the application denylist switch.
- Disabling unmounting to permit injection can leave module mount changes visible to that process.
- Zygisk Next's **Unmount Only** mode can allow module injection while restoring mount changes, when supported and correctly configured.

These behaviors are controlled by the root and injection framework, not by App Version Patcher. The module cannot guarantee root concealment. Use the smallest necessary scope and test sensitive applications individually.

## Responsible use

App Version Patcher is intended for software development, compatibility testing, interoperability research, and other lawful and authorized uses. Users are responsible for ensuring that their use complies with applicable laws, third-party terms, and authorization requirements.

Do not use this software to bypass payment, licensing, access controls, anti-fraud protections, or security mechanisms. The software is provided as-is, without warranty.

See [RESPONSIBLE_USE.md](RESPONSIBLE_USE.md) for the full project policy.

## Releases

Each published version receives its own release and tag in the format:

```text
versionCode-versionName
```

The `latest` tag is maintained only as a pointer to the newest release commit. Release notes come from [CHANGELOG.md](CHANGELOG.md).

## License

The public source code is available under the [PolyForm Noncommercial License 1.0.0](LICENSE). Commercial use is not granted by the public license. The license and required notice are also included inside the APK.

Commercial licensing may be granted separately through a written agreement with the copyright holder. See [COMMERCIAL_LICENSING.md](COMMERCIAL_LICENSING.md).

Copies of earlier versions already received under the MIT License retain the permissions granted to those copies at the time of distribution.
