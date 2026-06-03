# Roadmap

Direction and bets for kexpresso. This is a living document; priorities shift with
evidence. See [WHEN-TO-USE.md](WHEN-TO-USE.md) for the honest positioning that frames it.

## Guiding principles

1. **Honesty over hype.** We document where kexpresso *loses* to raw `Regex`. Trust scales.
2. **Stay simple.** The core (`kexpresso-core`) must stay tiny and dependency-free. Anything
   heavy (analysis, generation) ships as an *optional* module — a purist takes only the core.
3. **The moat is what regex can't do about itself.** A better builder is a commodity.
   Introspection (explain, analyze, visualize, reverse) is the differentiation.

## The strategic spine: an internal AST

Today the builder concatenates a `String`. Moving the internals to a small **AST**
(a `sealed` node hierarchy that renders to the same regex string) keeps the public API and
output identical, but unlocks a whole class of features that raw regex cannot offer:
`describe()`, ReDoS analysis, railroad diagrams, pattern optimization, and eventually the
reverse direction (regex → DSL). **This is the backbone of 1.0.**

## Releases

### ✅ 0.1.0 — Foundation
Public fluent API (`kexpresso { }` → `KexpressoPattern`), full base DSL, lookarounds,
Text/Writing helpers, CI/CD, JitPack + GitHub Packages, Dokka docs.

### ✅ 0.2.0 — Usefulness
String operations (`replaceAll`/`split`/`matchEntire`), composition (`include`/`raw`/
`backreference`), DSL completions, **12 ready-to-use domain helpers**, Gradle 8.7, hosted
API docs.

### 🚧 0.3.0 — Prove it & make it safe
- **The honest enabler** — JMH `benchmarks` module + [WHEN-TO-USE.md](WHEN-TO-USE.md) with
  measured numbers (0 % match overhead; construction cost; verbosity trade-offs).
- **ReDoS safety** — detect catastrophic-backtracking shapes (nested quantifiers) and warn
  at build time / via `analyze()`. The first *non-cosmetic* reason to adopt kexpresso.
- More helpers where they add objective brevity (`ipv6`, `macAddress`, `base64`, …).

### 🔭 0.4.0 — Kotlin-idiomatic ergonomics
- **Typed captures** — `captureInt("year")`, type-safe named access, destructuring.
  (This is what made magic-regexp succeed in the JS world; it wins the purist.)
- **`describe()`** — a pattern explains itself in English (first AST-powered feature).
- **`kexpresso-test`** — `pattern.examples(n)` generates strings that match, for tests
  and confidence.

### 🌟 1.0.0 — Make it a category
- **AST refactor** (the spine above), with the public API unchanged — the 183-test suite is
  the regression net.
- **Kotlin Multiplatform** — JVM / JS / Native / WASM. `Regex` is already multiplatform and
  the DSL is pure string-building, so the port is mostly build setup + isolating the few
  JVM-only bits (`toPattern()`, fixed-length lookbehind).
- **Modularization** — `kexpresso-core` / `-analysis` / `-test` / `-kotlinx`.
- **Reverse** — `Kexpresso.from("\\d{4}-\\d{2}")` → DSL code + `describe()`. Everyone
  inherits cryptic regexes; turning them back into readable form is the killer feature.

## Assumptions to test (riskiest first)

1. **Appetite.** Do Kotlin developers actually want to trade brevity for readability? The
   library being *technically* free doesn't prove demand. **Cheapest test:** publish, post to
   r/Kotlin with the honest WHEN-TO-USE framing, observe stars/feedback before investing in
   0.4.0.
2. **Honesty vs. growth.** A truly honest comparison says "use `Regex` for ~60 % of
   patterns." We bet that long-term trust beats short-term adoption. Revisit if adoption stalls.
3. **AST cost/benefit.** The refactor is non-trivial. Validate with a spike that keeps every
   existing test green (identical output) before committing to it for 1.0.

## Explicitly out of scope (for now)

- Maven Central publishing (needs signing + credentials; JitPack covers the need today).
- A `creditCard()` validity helper (PCI surface + Luhn can't be done by regex).
- A broad international `phoneNumber()` helper (format diversity guarantees false results) —
  `e164Phone()` is the scoped, honest version.
