# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Added

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
- **Lookaround support** — `followedBy`, `notFollowedBy`, `precededBy`, `notPrecededBy`
  (positive/negative lookahead and lookbehind).
- **Distribution & release pipeline** — the library is now publishable:
  - Consumable via [JitPack](https://jitpack.io/#elzinko/kexpresso) as
    `com.github.elzinko:kexpresso:<tag>` (`jitpack.yml`, JDK 17).
  - `maven-publish` configuration producing a main jar, a sources jar, and a
    Dokka-generated javadoc jar, with a complete POM (license, developer, SCM).
  - Tag-driven **Release** workflow (`v*.*.*`) that builds, publishes to
    GitHub Packages, and creates a GitHub Release with generated notes.
  - Release version is injected via `-PreleaseVersion=<tag>`.

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
- **`KexpressoPattern`** — immutable, thread-safe result type wrapping a compiled `Regex`,
  exposing `matches`, `containsMatchIn`, `find`, `findAll`, `toRegex`, `toPattern`,
  `source`, and `options`.
- **Top-level entry points** — `kexpresso { }` function and `Kexpresso.pattern { }`
  object-oriented alternative, both accepting optional `vararg RegexOption`.
- **Text helpers** (`Text.kt`) — `word`, `pseudo`, `email`, `url` extension functions.
- **Writing helpers** (`Writing.kt`) — `sentence` and `paragraph` extension functions
  composed from core primitives.
- **CI/CD** — GitHub Actions workflow running compile, test, Detekt, and JaCoCo on every
  push and pull request.
- **Detekt** — static analysis configuration at `config/detekt/detekt.yml`.
- **JaCoCo** — test coverage reporting.

[Unreleased]: https://github.com/elzinko/kexpresso/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/elzinko/kexpresso/releases/tag/v0.1.0
