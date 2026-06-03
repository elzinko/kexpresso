# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

_Nothing yet._

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
