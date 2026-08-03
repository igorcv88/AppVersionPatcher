# Changelog

All notable changes to App Version Patcher are documented in this file.

The project uses Android `versionCode` plus semantic `versionName`. Release tags use the LSPosed repository-compatible format `versionCode-versionName`.

## [2.0.4] - 2026-08-02

### Changed

- Reworked the configuration screen with a lightweight native Android design.
- Added correct system-bar insets for Android 15 and newer edge-to-edge behavior.
- Added automatic light and dark themes without introducing a UI framework dependency.
- Changed the default interface language to English and added a Brazilian Portuguese translation.
- Added Android per-app language support for English and Brazilian Portuguese.
- Moved all user-facing configuration and dialog text into localized resources.
- Updated app-list rows, search, status, filters, and editor fields for clearer spacing and hierarchy.
- Improved direct `versionCode` range validation in the editor.

## [2.0.3] - 2026-08-02

### Changed

- Replaced the MIT License with PolyForm Noncommercial License 1.0.0 for current and future versions.
- Added a separate commercial-licensing path requiring explicit written permission.
- Redesigned the README with the project icon, badges, concise setup instructions, legitimate use cases, compatibility information, and responsible-use guidance.
- Added explicit documentation about LSPosed injection, root-concealment interactions, and detection limits.
- Added `RESPONSIBLE_USE.md`, `SECURITY.md`, `COMMERCIAL_LICENSING.md`, `NOTICE`, and `SUMMARY`.
- Added an SVG representation of the existing original launcher icon for repository documentation.
- Included the license and required notice inside the APK assets.
- Changed the release workflow to create a distinct release for every version using `versionCode-versionName`.
- Retained `latest` only as a movable tag pointing to the newest release commit.

## [2.0.2] - 2026-08-02

### Fixed

- Rejected `versionCode` values outside `0..2147483647`.
- Kept deprecated `PackageInfo.versionCode` and `getLongVersionCode()` consistent.
- Added defensive handling for invalid values already stored in preferences.

## [2.0.1] - 2026-08-02

### Changed

- Removed package-specific presets, default scope entries, version pairs, and private-use references.
- Made the module and documentation application-independent.
- Added adaptive, round, legacy, and monochrome launcher icon resources.
- Removed personal identifying information and signing-key details from public documentation.
- Preserved user-created configurations during upgrades.

## [2.0.0] - 2026-08-02

### Added

- Migrated the module to modern libxposed API 101.
- Added LSPosed remote preferences and dynamic scope management.
- Added configurable `PackageManager` and React Native `RNDeviceInfo` hooks.
- Added a signed manual GitHub Actions release workflow.

### Removed

- Removed the legacy Xposed API implementation.
- Removed the custom cross-application configuration provider.

## [1.1.0] - 2026-08-02

### Added

- Added a configurable application list and per-package version overrides.
- Added independent PackageManager and React Native hook selection.

## [1.0.0] - 2026-08-02

### Added

- Initial proof-of-concept module for overriding application-reported version metadata.
