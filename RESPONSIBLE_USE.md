# Responsible Use

App Version Patcher is intended for software development, compatibility testing, interoperability research, and other lawful and authorized uses.

Users are responsible for ensuring that their use complies with:

- applicable laws and regulations;
- third-party terms and policies;
- authorization requirements;
- intellectual-property and contractual obligations; and
- security and privacy requirements.

Do not use this software to bypass payment, licensing, access controls, anti-fraud protections, or security mechanisms. Do not use it to obtain access, functionality, data, or benefits that you are not authorized to receive.

Legitimate examples include testing software you develop or administer, reproducing version-dependent bugs, validating compatibility behavior, testing migrations and feature gates, and conducting interoperability research in controlled or authorized environments.

The project does not guarantee that changing locally reported version metadata will alter server-side behavior. Users remain responsible for the consequences of configurations they apply.

## Root and framework visibility

The module does not request root access, execute `su`, alter mount namespaces, modify system properties, or change denylist and concealment settings.

It does require LSPosed injection in every target process where hooks are active. Framework injection can be detectable. Allowing injection may also conflict with root-concealment settings maintained by KernelSU, Magisk, APatch, Zygisk Next, or related tools.

The project does not promise root concealment, anti-detection, or compatibility with security-sensitive applications. Use the smallest necessary scope and test each target environment independently.

## No warranty

The software is provided as-is, without warranty. See [LICENSE](LICENSE) for the controlling license terms.
