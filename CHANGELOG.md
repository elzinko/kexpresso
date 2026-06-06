# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Added

- **Four new domain helpers in `Domains.kt`:**
  - `ipv6()` — IPv6 address, full and `::` -compressed forms (e.g. `2001:db8::1`, `::1`).
    Embedded IPv4 notation and zone IDs are not covered; documented caveats included.
  - `macAddress()` — IEEE 802 MAC address, colon- or hyphen-separated
    (e.g. `01:23:45:67:89:AB`, `01-23-45-67-89-ab`). Cisco dot notation not supported.
  - `base64()` — standard Base64 with optional `=`/`==` padding
    (e.g. `S2V4cHJlc3Nv`, `dGVzdA==`). Also matches the empty string; URL-safe Base64 not matched.
  - `jwt()` — JSON Web Token in compact serialisation: three base64url segments separated by dots.
    Structural validation only — signature and claims are not verified.

- **`KexpressoPattern.examples(count, seed)`** — AST-driven string generation. Walks the
  pattern's internal AST and produces up to `count` distinct strings that satisfy `matches()`.
  Deterministic for a given `seed` (uses `kotlin.random.Random`; works on all KMP targets).
  Supported nodes (guaranteed to produce matching strings): `Sequence`, `Literal`, `Token`
  primitives (digit, letter, whitespace, anchors, …), `Quantifier`, `Group`, and `Alternation`.
  Best-effort (no match guarantee): `Raw` nodes (i.e. domain helpers like `email()` / `ipv4()`
  and `Kexpresso.from()`), lookarounds, and backreferences — generation still completes without
  throwing.

- **Maven Central publishing.** Kexpresso now publishes to **Maven Central** (Sonatype Central
  Portal) via the [Vanniktech Maven Publish plugin](https://github.com/vanniktech/gradle-maven-publish-plugin),
  which configures every Kotlin Multiplatform publication (sources + Dokka javadoc jars, POM,
  GPG signing) and the Central Portal upload tasks. This is the frictionless, **no-token**
  install path for consumers. See [`docs/PUBLISHING.md`](docs/PUBLISHING.md) for the
  maintainer setup (Central account, `io.github.elzinko` namespace verification, GPG key,
  and the four `MAVEN_CENTRAL_*` / `SIGNING_IN_MEMORY_KEY*` repo secrets).
- **GPG signing** of all published artifacts (required by Maven Central). Signing is
  credential-optional locally: `./gradlew build` and `publishToMavenLocal` succeed unsigned
  when no signing key is configured.

### Changed

- **BREAKING — groupId is now `io.github.elzinko`** (was `com.github.elzinko`). Maven Central
  requires the `io.github.<user>` namespace for GitHub-verified accounts; `com.github.*` is
  not accepted. Update your dependency coordinate from
  `com.github.elzinko:kexpresso:<version>` to `io.github.elzinko:kexpresso:<version>`.
  GitHub Packages publishing continues to work alongside Maven Central.

---

## [0.6.0] - 2026-06-06

### Added

- **Apple & iOS Native targets, published.** Added `iosArm64`, `iosX64`, and
  `iosSimulatorArm64` alongside the existing `macosX64` / `macosArm64`. The **release now runs
  on a `macos-latest` runner** — the most capable Kotlin/Native host — so it builds and
  publishes *every* target (JVM, JS, Wasm, Linux, Windows, macOS, iOS) from a single host,
  yielding complete and consistent multiplatform metadata (a consumer resolving the root module
  now sees the Apple/iOS variants). This unblocks Kotlin Multiplatform apps that target Apple
  platforms from depending on kexpresso in shared `commonMain`.
- **`Apple & Native` CI workflow** — verifies the Apple/iOS targets (tests on the macOS host +
  iOS simulator) and that all targets build on macOS, on every pull request.

---

## [0.5.0] - 2026-06-04

### Added

- **Kotlin Multiplatform support (JVM + JS/IR).** The full DSL — builder, `describe()`,
  `analyze()`, typed captures, and the reverse (regex → DSL) API — now lives in `commonMain`
  and is published for the `jvm` and `js(IR, nodejs)` targets. Kotlin was bumped from 1.8.20 to
  **1.9.24**.
- **Native + Wasm targets.** Added `wasmJs(nodejs)` and host-conditional Kotlin/Native targets:
  `linuxX64` + `mingwX64` on Linux, `macosX64` + `macosArm64` on macOS (each native target can
  only be cross-compiled from a host of the same family, so they are built per host and the
  published artifact set is assembled across hosts). The portable `commonTest` suite (31 tests)
  passes identically on JVM, JS, Wasm, and the built native targets — **no tests needed to move
  from `commonTest` to `jvmTest`**: the Kotlin/Wasm engine matches ECMAScript and accepts the
  whole portable suite, and the Kotlin/Native `kotlin.text.Regex` engine is a superset of it
  (it even accepts the `\A` / `\z` / `\Z` / `\G` anchors, named groups, named/numeric
  backreferences, lookahead, lookbehind, and atomic groups). JVM-flavoured constructs that the
  smaller JS/Wasm engine rejects at `Regex` compile time (`\A` / `\z`, atomic groups, possessive
  quantifiers, some lookbehind) remain JVM-only and continue to be exercised in `jvmTest`.
  Building the `macos*` targets locally requires a full Xcode install; a Command-Line-Tools-only
  macOS host still builds jvm/js/wasmJs and skips the Apple targets with a warning.

### Changed

- **Literal escaping is now portable.** `literal(...)` / `char(...)` previously rendered via
  the JVM-only `Regex.escape()` (`\Q…\E`); they now use a hand-written, per-character escaper
  that backslash-escapes regex metacharacters. This changes the generated `source` for literals
  (e.g. `literal("a.b")` now renders as `a\.b` instead of `\Qa.b\E`) — **matching behaviour is
  unchanged**, only the source string differs.
- **`KexpressoPattern.toPattern()` is now a JVM-only extension function** (it returns a
  `java.util.regex.Pattern`, which does not exist on Kotlin/JS). It was previously a member of
  the common `KexpressoPattern` class. All other public API is unchanged.
- **Coverage tooling: JaCoCo → Kotlinx Kover.** The coverage gate (line ≥ 85%) is now a Kover
  verification rule wired into `check`; the XML report for Codecov moved from
  `build/reports/jacoco/test/jacocoTestReport.xml` to `build/reports/kover/report.xml`.

### Removed

- **JMH benchmark module** (`src/jmh/`) and the `me.champeau.jmh` Gradle plugin. JMH is
  JVM-only and incompatible with the KMP build model; the benchmark numbers remain documented in
  `docs/WHEN-TO-USE.md` and `benchmarks/README.md`.

### Breaking

- **Published artifact coordinates now carry a target suffix.** Gradle consumers can keep using
  `com.github.elzinko:kexpresso:<version>` (Gradle module metadata resolves the correct target
  automatically), but plain-Maven JVM consumers must switch to `com.github.elzinko:kexpresso-jvm`.
- **Published vs. buildable targets.** The release/JitPack build runs on Linux, so the
  *published* artifacts are `jvm`, `js`, `wasmJs`, `linuxX64`, and `mingwX64`. The `macosX64` /
  `macosArm64` targets are supported and build from source, but pre-built macOS Native artifacts
  are **not yet published** — publishing them needs a macOS release runner (tracked as a roadmap
  follow-up).
- Constructs that the DSL can build but that only run on the JVM regex engine — `startOfText()` /
  `endOfText()` (`\A` / `\z`), atomic groups, possessive quantifiers, and some lookbehind — throw
  at `Regex` compile time on JS. They are JVM-only at runtime; use `startOfLine()` / `endOfLine()`
  for portable anchoring.

---

## [0.4.0] - 2026-06-03

### Added

- **Reverse API** (`Reverse.kt`) — read an existing raw regex back into kexpresso.
  - `Kexpresso.from(regex)` compiles the regex **verbatim** (so matching is *always exact* —
    `Kexpresso.from(r).matches(x) == Regex(r).matches(x)` for every input) and best-effort parses
    it into the internal AST to power `describe()` and `toKexpressoCode()`. An invalid regex
    propagates `PatternSyntaxException`, exactly as `Regex(...)` would.
  - `KexpressoPattern.toKexpressoCode()` emits compilable kexpresso DSL Kotlin source for **any**
    pattern — builder-made or `from`-parsed — enabling round-tripping. Recognised tokens map to
    their friendly DSL call (e.g. `\d` → `digit()`, `[a-zA-Z]` → `letter()`); literals,
    quantifiers, groups, lookarounds, alternation, and back-references map to their DSL forms.
  - The parser models the common constructs and **honestly degrades anything it does not model to
    `raw("…")`** (possessive quantifiers like `a++`, atomic groups `(?>…)`, inline-flag groups
    `(?i)`, unmodeled classes). Best-effort parsing never alters match behaviour and never throws
    on a valid regex.

### Security

- **Supply-chain & quality hardening** — CodeQL static analysis, OpenSSF Scorecard,
  Dependabot (Gradle + GitHub Actions), all GitHub Actions pinned to commit SHAs, build
  provenance attestation + SHA-256 checksums on releases, a private vulnerability reporting
  policy (`SECURITY.md`), and a JaCoCo coverage gate (line ≥ 85%) enforced in CI.

---

## [0.3.0] - 2026-06-03

### Added

- **Honest positioning & benchmarks** — `docs/WHEN-TO-USE.md` documents when to use
  kexpresso and, just as importantly, when **not** to (the verbosity trade-off, per-persona
  verdicts), a JMH `benchmarks` module proving **0 % match-time overhead** versus a raw
  pre-compiled `Regex` (reproducible with `./gradlew jmh`), and a published `docs/ROADMAP.md`.
- **Typed captures API** (`Captures.kt`) — ergonomic, type-safe extraction of named and
  indexed capture groups from any `MatchResult` via the new `MatchResult.captures` extension
  property. `Captures` exposes:
  - Nullable accessors by name — `string(name)`, `int(name)`, `long(name)`, `double(name)`,
    `boolean(name)` — returning `null` when the group is absent, unmatched, or its value
    cannot be parsed.
  - Nullable accessors by index (0 = whole match, 1 = first group, …) — same five types.
  - Throwing variants by name — `stringOrThrow`, `intOrThrow`, `longOrThrow`, `doubleOrThrow`,
    `booleanOrThrow` — each throwing `NoSuchElementException` if the group is absent, and
    `NumberFormatException` / `IllegalArgumentException` (with a message naming the group and
    the offending value) if the value cannot be parsed.
  - Example: `pattern.find("2026-06-03")?.captures?.int("year")` → `2026`.
- **AST-backed internal representation** (`Ast.kt`) — the builder now assembles a small
  `sealed` `RegexNode` hierarchy (`SequenceNode`, `Token`, `Literal`, `Raw`, `Quantifier`,
  `Group`, `Alternation`, `Lookaround`, `Backreference`) instead of concatenating a string.
  Each node renders to the **exact same** regex `source` (byte-for-byte identical, the full
  test suite is the regression net) and additionally carries enough structure to describe
  itself. This is the "AST spine" the roadmap calls the backbone of 1.0 — it unlocks
  introspection features without changing the public API or output.
- **`KexpressoPattern.describe()`** — returns a readable, deterministic English description of
  a pattern derived from its AST, e.g.
  `kexpresso { startOfText(); oneOrMore { digit() }; endOfText() }.describe()` →
  `"start of text, one or more of (a digit), end of text"`. Domain/helper fragments degrade
  gracefully to ``raw regex `…` ``. The public two-argument `KexpressoPattern(source, regex)`
  constructor is preserved and falls back to a `Raw(source)` AST node.
- **ReDoS analysis API** (`Analysis.kt`) — best-effort static heuristic for
  catastrophic-backtracking risk, accessible via `KexpressoPattern.analyze()` and the
  convenience property `KexpressoPattern.isPotentiallyVulnerable`.
  - Detects the canonical "evil regex" shape: nested unbounded quantifiers — an unbounded
    quantifier (`*`, `+`, or `{n,}`) applied to a group whose body itself contains an
    unbounded quantifier (e.g. `(?:a+)+`, `(a*)*`, `(?:\w+)+`).
  - Returns a `ReDoSReport` with zero or more `ReDoSFinding`s (each with a human-readable
    `message`, source `index`, and `ReDoSSeverity.WARNING` severity).
  - False-positive guards: quantifier chars inside `[...]`, escaped quantifiers (`\+`,
    `\*`), bounded outer or inner quantifiers, possessive quantifiers (`*+`, `++`), and
    atomic groups `(?>...)`.
  - Honestly documented as a **heuristic, not a proof** — a clean result does not
    guarantee the pattern is free of catastrophic backtracking.

---

## [0.2.0] - 2026-06-03

### Added

- **Domain helpers** (`Domains.kt`) — 12 ready-to-use `KexpressoBuilder` extension
  functions covering common real-world formats:
  - `ipv4()` — IPv4 address in dotted-decimal notation (each octet 0–255).
  - `uuid()` — RFC 4122 UUID, versions 1–5, case-insensitive hex.
  - `slug()` — URL/CMS slug (lowercase alphanumeric groups separated by hyphens).
  - `hexColor()` — CSS hex color `#RGB`, `#RGBA`, `#RRGGBB`, or `#RRGGBBAA`.
  - `semanticVersion()` — SemVer 2.0.0 string, with optional pre-release and build metadata.
  - `isoDate()` — ISO-8601 calendar date `YYYY-MM-DD` (month/day structurally validated;
    day-of-month not checked against calendar).
  - `isoTime()` — ISO-8601 time `HH:MM[:SS][Z|±HH:MM]` (leap seconds excluded).
  - `integerNumber()` — signed/unsigned integer without leading zeros.
  - `decimalNumber()` — decimal number with optional fractional part.
  - `hashtag()` — social-media hashtag (`#` + letter + word chars).
  - `mention()` — @mention, 1–50 alphanumeric/underscore chars (Twitter/X convention).
  - `e164Phone()` — E.164 international phone number in compact form (e.g. `+14155552671`).
- **DSL completions** — new primitives and anchor on `KexpressoBuilder`:
  - `lowercaseLetter()` → `[a-z]`
  - `alphanumeric()` → `[a-zA-Z0-9]`
  - `tab()` → `\t`, `newline()` → `\n`, `carriageReturn()` → `\r`
  - `nonWordBoundary()` → `\B`
- **Composition & escape hatch** — new methods on `KexpressoBuilder`:
  - `raw(pattern)` — inserts a raw regex fragment verbatim (no escaping).
  - `include(pattern)` — embeds a compiled `KexpressoPattern` as a non-capturing group,
    enabling sub-patterns to be defined once and reused safely.
  - `backreference(n: Int)` — numeric back-reference `\n` (requires n ≥ 1).
  - `backreference(name: String)` — named back-reference `\k<name>`.
  - Both `backreference(name)` and `capture(name, block)` now validate the group name
    eagerly and throw `IllegalArgumentException` with a clear message instead of a
    later `PatternSyntaxException`.
- **String-operations API** — new methods on `KexpressoPattern` delegating to the
  underlying `Regex`:
  - `replaceFirst(input, replacement)` — replace the first match.
  - `replaceAll(input, replacement)` — replace all matches with a fixed string.
  - `replaceAll(input, transform)` — replace all matches using a per-match transform.
  - `split(input, limit)` — split the input around pattern matches.
  - `matchEntire(input)` — attempt a full-string match, returning `MatchResult?`.

### Changed

- **Build tooling** — upgraded the Gradle wrapper from 7.6.1 to **8.7**, clearing the
  "incompatible with Gradle 8.0" deprecation warning. (A residual Gradle-9.0 forward
  notice remains; it originates from plugin internals, not this build script.)
- **Hosted API docs** — the Dokka API reference is now published to GitHub Pages on
  every push to `main` (<https://elzinko.github.io/kexpresso/>) via a new `Docs` workflow.

---

## [0.1.0] - 2026-06-03

### Added

- **Fluent DSL** — `KexpressoBuilder` with a full set of primitives, character-class
  helpers, anchors, quantifiers, and grouping/alternation constructs:
  - Primitives: `literal`, `char`, `digit`, `nonDigit`, `whitespace`, `space`,
    `nonWhitespace`, `wordChar`, `nonWordChar`, `anyChar`, `letter`, `capitalLetter`,
    `endPunctuation`.
  - Character classes: `anyOf`, `noneOf`, `inRange`.
  - Anchors: `startOfLine`, `endOfLine`, `startOfText`, `endOfText`, `wordBoundary`.
  - Quantifiers (all support `greedy` parameter): `optional`, `zeroOrMore`, `oneOrMore`,
    `exactly`, `atLeast`, `between`.
  - Grouping and alternation: `group`, `capture` (numbered), `capture(name)` (named),
    `oneOf`.
- **Lookaround support** — `followedBy`, `notFollowedBy`, `precededBy`, `notPrecededBy`
  (positive/negative lookahead and lookbehind).
- **`KexpressoPattern`** — immutable, thread-safe result type wrapping a compiled `Regex`,
  exposing `matches`, `containsMatchIn`, `find`, `findAll`, `toRegex`, `toPattern`,
  `source`, and `options`.
- **Top-level entry points** — `kexpresso { }` function and `Kexpresso.pattern { }`
  object-oriented alternative, both accepting optional `vararg RegexOption`.
- **Text helpers** (`Text.kt`) — `word`, `pseudo`, `email`, `url` extension functions.
- **Writing helpers** (`Writing.kt`) — `sentence` and `paragraph` extension functions
  composed from core primitives.
- **Distribution & release pipeline** — the library is publishable:
  - Consumable via [JitPack](https://jitpack.io/#elzinko/kexpresso) as
    `com.github.elzinko:kexpresso:<tag>` (`jitpack.yml`, JDK 17).
  - `maven-publish` configuration producing a main jar, a sources jar, and a
    Dokka-generated javadoc jar, with a complete POM (license, developer, SCM).
  - Tag-driven **Release** workflow (`v*.*.*`) that builds, publishes to
    GitHub Packages, and creates a GitHub Release with generated notes.
  - Release version is injected via `-PreleaseVersion=<tag>`.
- **CI/CD** — GitHub Actions workflow running compile, test, Detekt, and JaCoCo on every
  push and pull request.
- **Detekt** — static analysis configuration at `config/detekt/detekt.yml`.
- **JaCoCo** — test coverage reporting.

[Unreleased]: https://github.com/elzinko/kexpresso/compare/v0.6.0...HEAD
[0.6.0]: https://github.com/elzinko/kexpresso/compare/v0.5.0...v0.6.0
[0.5.0]: https://github.com/elzinko/kexpresso/compare/v0.4.0...v0.5.0
[0.4.0]: https://github.com/elzinko/kexpresso/compare/v0.3.0...v0.4.0
[0.3.0]: https://github.com/elzinko/kexpresso/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/elzinko/kexpresso/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/elzinko/kexpresso/releases/tag/v0.1.0
