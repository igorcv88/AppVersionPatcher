# Security Policy

## Supported versions

Security fixes are provided for the latest published release. Older versions may no longer receive fixes.

## Reporting a vulnerability

Do not publish unpatched vulnerability details, secrets, signing material, private application data, or unredacted device logs in a public issue.

Use GitHub private vulnerability reporting when it is available for this repository. Otherwise, open a minimal issue asking the repository owner to establish a private contact channel, without including the sensitive details.

A useful report should include:

- App Version Patcher version;
- Android version;
- LSPosed or compatible framework name and API version;
- relevant root and injection environment;
- affected target type;
- reproducible steps;
- expected and observed behavior; and
- redacted logs or a minimal proof of concept.

## Security boundary

App Version Patcher changes selected version values inside an injected process. It is not a root-hiding, anti-detection, sandboxing, or access-control tool.

The module does not intentionally change mount state, properties, package-visibility controls, denylist rules, or concealment modules. Nevertheless, LSPosed injection and the surrounding root environment may be detectable by a target application. Reports about unintended information exposure caused by this module's own code are in scope; general framework-detection behavior should be reported to the relevant framework project.
