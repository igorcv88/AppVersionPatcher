# App Version Patcher

App Version Patcher is a configurable LSPosed module that changes the application version reported inside selected target processes, without modifying or resigning the target APK.

The module is application-agnostic. No package or version is preconfigured on a clean installation.

## Requirements

- LSPosed or a compatible implementation of libxposed API 101
- Android 8.1 or newer
- The target application included in the module scope

The module uses only the modern libxposed API. It does not use the legacy Xposed API, `XSharedPreferences`, or a custom cross-application `ContentProvider`.

## Supported hooks

Two independent hook strategies can be enabled for each package:

- **PackageManager:** patches `PackageInfo.versionName`, `versionCode`, and `longVersionCode` returned inside the target process. This covers applications that obtain their version through the standard Android package APIs.
- **RNDeviceInfo:** patches `RNDeviceModule#getConstants()` and the internal `PackageInfo` used by `react-native-device-info`. This covers React Native applications that use that library for version information.

Applications that read a hardcoded constant, use another library, or obtain the version exclusively from a server may require a different hook point.

## Configuration and scope

Configuration is stored in remote preferences managed by LSPosed. The module application uses the modern LSPosed service to:

- read and write the same preferences available to injected target processes;
- display the current module scope;
- request that an application be added to the scope when saving its first configuration.

The application list is ordered as follows:

1. applications with an active version override;
2. applications in the module scope without an override;
3. all other installed applications.

Configured applications remain visible at the top even when they are temporarily outside the scope. After saving a configuration, force-stop and reopen the target application. Updating the module may also require the restart procedure used by the installed LSPosed implementation.

## Usage

1. Install and activate App Version Patcher in LSPosed.
2. Open the module application and wait for the LSPosed service connection.
3. Select a target application.
4. Enter the desired `versionName` and, optionally, `versionCode`.
5. Enable PackageManager, RNDeviceInfo, or both.
6. Save the configuration and approve the scope request when required.
7. Force-stop and reopen the target application.

Granting root access to the module application is not required. Hook execution is provided by LSPosed inside the scoped target process.

## libxposed structure

- Java entry: `META-INF/xposed/java_init.list`
- Module configuration: `META-INF/xposed/module.prop`
- `minApiVersion=101`
- `targetApiVersion=101`
- `staticScope=false`

There is no package-specific default scope.

## Build and release

The GitHub Actions workflow runs only through `workflow_dispatch`. It:

- uses JDK 17 and Android SDK 36;
- builds `assembleRelease`;
- signs with `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD`;
- verifies the APK with `apksigner`;
- updates the rolling `latest` release with a single `AppVersionPatcher.apk` asset;
- does not upload a GitHub Actions artifact.

## Launcher icon

The launcher artwork is an original vector created directly for this project. It is not copied from an external icon pack or third-party asset. The Android resources include separate background and foreground layers for adaptive icons, plus legacy and monochrome fallbacks.

## Limitations and safety

Changing the version perceived by an application can affect migrations, feature gates, compatibility checks, and update flows. Use the smallest necessary scope and verify that important operations are still accepted by the remote service.

The module does not automatically discover the newest version from an application store and does not bypass server-side version enforcement when the client-reported value is ignored.

## License

MIT
