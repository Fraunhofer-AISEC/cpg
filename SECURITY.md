# Security Policy

The following file contains information about the security policy and procedures used in our code property graph library.

## Supported Versions

We aim to keep semantic versioning in mind and try to release a new major version if the (public) API has changed. We therefore release a new major version every few months and only support the current major version.

| Version | Supported          |
|---------| ------------------ |
| 9.x.x   | :white_check_mark: |
| < 9.0.0 | :x:                |

## Reporting a Vulnerability

Should you encounter a vulnerability in our software, please use the possibility to privately report a vulnerability through GitHub using https://github.com/Fraunhofer-AISEC/cpg/security/advisories/new.

We will then get in contact with you, assess the impact of the reported issue and try to fix it. After a fix is released, we will publish a Security Advisory (see below).

## Security Advisories

All fixed security issues will be accompanied by a security advisory. We aim to provide them in two formats

* Using GitHub's internal database (https://github.com/Fraunhofer-AISEC/cpg/security/advisories), in order to inform GitHub users as soon as possible
* In the repo itself in the folder [docs/csaf](./docs/csaf/) using the [CSAF](https://docs.oasis-open.org/csaf/csaf/v2.0/os/csaf-v2.0-os.html) standard. This allows also for a more fine-grained reporting of a security issue as well as the current status and possible affected components.
