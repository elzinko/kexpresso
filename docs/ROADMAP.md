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

### ✅ 0.3.0 — Prove it & make it safe
- ✅ **The honest enabler** — JMH `benchmarks` module + [WHEN-TO-USE.md](WHEN-TO-USE.md) with
  measured numbers (**0 % match overhead**; construction cost; verbosity trade-offs).
- ✅ **ReDoS safety** — `KexpressoPattern.analyze()` / `isPotentiallyVulnerable` detect the
  nested-unbounded-quantifier shape. The first *non-cosmetic* reason to adopt kexpresso.
- ✅ **AST spine** (`Ast.kt`) — internal `RegexNode` representation; output byte-identical,
  full test suite as the regression net. The backbone the rest of 1.0 builds on.
- ✅ **`describe()`** — a pattern explains itself in English (first AST-powered feature).
- ✅ **Typed captures** — `MatchResult.captures` with type-safe `int`/`string`/… accessors.
  (The kind of ergonomics that made magic-regexp succeed; it wins the purist.)
- ⏭️ Deferred: more helpers (`ipv6`, `macAddress`, `base64`, …).

### ✅ 0.4.0 — Read existing regexes + supply-chain hardening
- ✅ **Reverse** — `Kexpresso.from(regex)` → `describe()` / `toKexpressoCode()`. Matching is
  always exact; best-effort parse into the `RegexNode` AST, honest `raw()` fallback. The
  differentiating "killer" feature, built on the AST spine.
- ✅ **Supply-chain hardening** — CodeQL, OpenSSF Scorecard, Dependabot, SHA-pinned Actions,
  release provenance + checksums, coverage gate, FUNDING/SECURITY/Code of Conduct.

### ✅ 0.5.0 — Multiplatform (JVM + JS + Wasm + Native)
- ✅ **Kotlin Multiplatform** — Kotlin 1.9.24; targets `jvm`, `js(IR)`, `wasmJs`, and
  host-conditional Native (`linuxX64`/`mingwX64` on Linux, `macosX64`/`macosArm64` on macOS).
  Logic in `commonMain`; `toPattern()` is a JVM-only extension; portable literal escaping. The
  31-test portable suite passes on JVM/JS/Wasm/Native; JVM-flavoured constructs stay JVM-only.
- ✅ Tooling: jacoco → Kover, JMH removed (numbers stay in docs), multi-target publishing.

### 🔭 Next (toward 1.0.0) — Make it a category
- ✅ **Publish macOS/iOS Native artifacts** — the release now runs on a `macos-latest` runner
  (the most capable Kotlin/Native host), publishing every target — JVM, JS, Wasm, Linux,
  Windows, macOS, **iOS** (`iosArm64`/`iosX64`/`iosSimulatorArm64`) — from a single host with
  complete metadata. An `Apple & Native` PR workflow verifies the Apple targets.
- **`kexpresso-test`** — `pattern.examples(n)` generates strings that match, for tests.
- **Kotlin 2.x / Gradle 9 / Dokka 2** upgrades (the Dependabot majors deferred during KMP).
- **Modularization** — `kexpresso-core` / `-analysis` / `-test` (likely premature until the lib grows).
- **1.0.0** — once the above land and the new multiplatform coordinates have soaked, cut 1.0
  with an API-stability commitment + migration notes.

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
