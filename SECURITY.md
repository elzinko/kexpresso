# Security Policy

## Supported versions

kexpresso is pre-1.0; security fixes are applied to the latest released minor.

| Version | Supported          |
|---------|--------------------|
| 0.3.x   | :white_check_mark: |
| < 0.3   | :x:                |

## Reporting a vulnerability

**Please do not open a public issue for security vulnerabilities.**

Report privately through GitHub's
[private vulnerability reporting](https://github.com/elzinko/kexpresso/security/advisories/new)
(repository **Security** tab → **Report a vulnerability**). If that is unavailable, email
the maintainer at **thomas.couderc@gmail.com**.

Please include:

- a description of the issue and its impact,
- a minimal reproduction (ideally a failing pattern or input),
- the kexpresso and JDK versions.

We aim to acknowledge a report within **5 business days** and to provide a remediation
timeline after triage. Coordinated disclosure is appreciated — we will credit reporters
who wish to be named once a fix is released.

## Scope notes

kexpresso compiles to a standard `java.util.regex.Pattern`. Two areas are worth calling out:

- **ReDoS / catastrophic backtracking.** kexpresso ships a best-effort analyzer
  (`KexpressoPattern.analyze()`); it is a heuristic, not a guarantee. Patterns built from
  untrusted input should still be bounded and load-tested.
- **`raw(...)` and `Kexpresso.from(...)`.** These accept arbitrary regex; their safety is
  the same as hand-written regex. Validate and constrain untrusted patterns.
