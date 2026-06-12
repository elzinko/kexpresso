# API Surface Review — pre-1.0 freeze

> **Status**: review of 2026-06-10, kept as a historical record. All recommended actions
> shipped in v0.9.0: `space()`, `capitalLetter()`, and `pseudo()` are deprecated (replaced
> by `whitespace()`, `uppercaseLetter()`, and `handle()`), `Writing.kt` no longer calls
> deprecated names, and every 🟡 symbol is marked `@ExperimentalKexpressoApi`. The 🔴/🟡
> notes below describe the pre-0.9.0 state.
> **Goal**: classify every public symbol so we know what is safe to freeze in 1.0,
> what should be opt-in `@ExperimentalKexpressoApi`, what should become `internal`,
> and what should be renamed/refactored before the freeze.

## Decision matrix

| Mark | Meaning | Action before 1.0 |
|---|---|---|
| 🟢 **Keep** | Solid, idiomatic, intended user-facing API. | None. |
| 🟡 **@OptIn** | Useful but may evolve (best-effort feature, advanced helper, experimental). | Annotate with `@ExperimentalKexpressoApi`; document. |
| 🟠 **Internal** | Currently public but is an implementation detail (AST nodes, helpers). | Change visibility to `internal`. |
| 🔴 **Rename/refactor** | Naming inconsistency or design issue worth fixing while we still can. | Discuss and apply BEFORE 1.0. |

---

## Findings by file

### `Kexpresso.kt`

| Symbol | Signature (abbreviated) | Mark | Rationale |
|---|---|---|---|
| `kexpresso` | `fun kexpresso(vararg options: RegexOption, block: …): KexpressoPattern` | 🟢 | Primary entry point; natural DSL name. |
| `Kexpresso` | `object Kexpresso` | 🟢 | OO companion entry point; keeps parity with top-level `kexpresso {}`. |
| `Kexpresso.pattern` | `fun pattern(vararg options: RegexOption, block: …): KexpressoPattern` | 🟢 | Clean OO form. |
| `KexpressoPattern` | `class KexpressoPattern` | 🟢 | Central value type; well-specified. |
| `KexpressoPattern(source, regex)` | `constructor(source: String, regex: Regex)` | 🟡 | Public constructor that drops the AST to a `Raw` node is useful as an escape hatch but degrades `describe()` and `toKexpressoCode()`. Callers who use it get second-class introspection. Mark `@ExperimentalKexpressoApi` and document the limitation explicitly. |
| `KexpressoPattern.source` | `val source: String` | 🟢 | Core property. |
| `KexpressoPattern.regex` | `val regex: Regex` | 🟢 | Direct Kotlin `Regex` access is expected. |
| `KexpressoPattern.options` | `val options: Set<RegexOption>` | 🟢 | Mirrors `Regex.options`. |
| `KexpressoPattern.describe` | `fun describe(): String` | 🟢 | Human-readable description; well-documented. |
| `KexpressoPattern.matches` | `fun matches(input: CharSequence): Boolean` | 🟢 | Core matching. |
| `KexpressoPattern.containsMatchIn` | `fun containsMatchIn(input: CharSequence): Boolean` | 🟢 | Core matching. |
| `KexpressoPattern.find` | `fun find(input: CharSequence, startIndex: Int = 0): MatchResult?` | 🟢 | Core matching. |
| `KexpressoPattern.findAll` | `fun findAll(input: CharSequence): Sequence<MatchResult>` | 🟢 | Core matching. |
| `KexpressoPattern.toRegex` | `fun toRegex(): Regex` | 🟢 | Interop accessor. |
| `KexpressoPattern.replaceFirst` | `fun replaceFirst(input: CharSequence, replacement: String): String` | 🟢 | Standard replace; well-documented. |
| `KexpressoPattern.replaceAll` (String) | `fun replaceAll(input: CharSequence, replacement: String): String` | 🟢 | Standard replace. |
| `KexpressoPattern.replaceAll` (transform) | `fun replaceAll(input: CharSequence, transform: (MatchResult) -> CharSequence): String` | 🟢 | Transform variant. |
| `KexpressoPattern.split` | `fun split(input: CharSequence, limit: Int = 0): List<String>` | 🟢 | Standard split. |
| `KexpressoPattern.matchEntire` | `fun matchEntire(input: CharSequence): MatchResult?` | 🟢 | Standard full-match. |
| `KexpressoPattern.toString` | `override fun toString(): String` | 🟢 | Returns `source`; sensible. |
| `KexpressoPattern.equals` / `hashCode` | — | 🟢 | Value semantics based on source + options. |
| `KexpressoBuilder` | `class KexpressoBuilder` | 🟢 | DSL receiver; central builder. |
| `KexpressoBuilder.build` | `fun build(vararg options: RegexOption): KexpressoPattern` | 🟡 | Compiles the builder; useful for advanced users who hold a builder reference. However, exposing `build` publicly means callers can hold a mutable builder and call `build()` multiple times, which is a subtle footgun (the builder is still mutable after the first call). Consider whether this should stay public or become `internal`. At minimum, annotate `@ExperimentalKexpressoApi` until the mutability story is settled. |
| `KexpressoBuilder.literal` | `fun literal(text: String): KexpressoBuilder` | 🟢 | Core primitive. |
| `KexpressoBuilder.char` | `fun char(c: Char): KexpressoBuilder` | 🟢 | Core primitive. |
| `KexpressoBuilder.digit` | `fun digit(): KexpressoBuilder` | 🟢 | Core primitive. |
| `KexpressoBuilder.nonDigit` | `fun nonDigit(): KexpressoBuilder` | 🟢 | Core primitive. |
| `KexpressoBuilder.whitespace` | `fun whitespace(): KexpressoBuilder` | 🟢 | Core primitive. |
| `KexpressoBuilder.space` | `fun space(): KexpressoBuilder` | 🔴 | Alias for `whitespace()`, retained for backward compatibility per KDoc. Having two names for the same concept is confusing if frozen for 5 years. Deprecate `space()` before 1.0 and direct users to `whitespace()`. |
| `KexpressoBuilder.nonWhitespace` | `fun nonWhitespace(): KexpressoBuilder` | 🟢 | Core primitive. |
| `KexpressoBuilder.wordChar` | `fun wordChar(): KexpressoBuilder` | 🟢 | Core primitive. |
| `KexpressoBuilder.nonWordChar` | `fun nonWordChar(): KexpressoBuilder` | 🟢 | Core primitive. |
| `KexpressoBuilder.anyChar` | `fun anyChar(): KexpressoBuilder` | 🟢 | Core primitive. |
| `KexpressoBuilder.letter` | `fun letter(): KexpressoBuilder` | 🟢 | Core primitive; ASCII-scoped, documented. |
| `KexpressoBuilder.capitalLetter` | `fun capitalLetter(): KexpressoBuilder` | 🔴 | Inconsistent with sibling `lowercaseLetter()`. If we have `lowercaseLetter()`, the symmetric name is `uppercaseLetter()`, not `capitalLetter()`. Freezing this asymmetry for 1.0 would be awkward. Rename to `uppercaseLetter()` (keep a deprecated alias if necessary for a clean cut). |
| `KexpressoBuilder.endPunctuation` | `fun endPunctuation(): KexpressoBuilder` | 🟢 | Specific utility; well-named. |
| `KexpressoBuilder.anyOf` | `fun anyOf(chars: String): KexpressoBuilder` | 🟢 | Character class primitive; named consistently with `noneOf`. |
| `KexpressoBuilder.noneOf` | `fun noneOf(chars: String): KexpressoBuilder` | 🟢 | Character class primitive. |
| `KexpressoBuilder.inRange` | `fun inRange(from: Char, to: Char): KexpressoBuilder` | 🟢 | Character range primitive. |
| `KexpressoBuilder.startOfLine` | `fun startOfLine(): KexpressoBuilder` | 🟢 | Anchor. |
| `KexpressoBuilder.endOfLine` | `fun endOfLine(): KexpressoBuilder` | 🟢 | Anchor. |
| `KexpressoBuilder.startOfText` | `fun startOfText(): KexpressoBuilder` | 🟢 | Anchor. |
| `KexpressoBuilder.endOfText` | `fun endOfText(): KexpressoBuilder` | 🟢 | Anchor. |
| `KexpressoBuilder.wordBoundary` | `fun wordBoundary(): KexpressoBuilder` | 🟢 | Anchor. |
| `KexpressoBuilder.nonWordBoundary` | `fun nonWordBoundary(): KexpressoBuilder` | 🟢 | Anchor. |
| `KexpressoBuilder.lowercaseLetter` | `fun lowercaseLetter(): KexpressoBuilder` | 🟢 | Core primitive. |
| `KexpressoBuilder.alphanumeric` | `fun alphanumeric(): KexpressoBuilder` | 🟢 | Core primitive. |
| `KexpressoBuilder.tab` | `fun tab(): KexpressoBuilder` | 🟢 | Control character. |
| `KexpressoBuilder.newline` | `fun newline(): KexpressoBuilder` | 🟢 | Control character. |
| `KexpressoBuilder.carriageReturn` | `fun carriageReturn(): KexpressoBuilder` | 🟢 | Control character. |
| `KexpressoBuilder.optional` | `fun optional(greedy: Boolean = true, block: …): KexpressoBuilder` | 🟢 | Quantifier DSL. |
| `KexpressoBuilder.zeroOrMore` | `fun zeroOrMore(greedy: Boolean = true, block: …): KexpressoBuilder` | 🟢 | Quantifier DSL. |
| `KexpressoBuilder.oneOrMore` | `fun oneOrMore(greedy: Boolean = true, block: …): KexpressoBuilder` | 🟢 | Quantifier DSL. |
| `KexpressoBuilder.exactly` | `fun exactly(n: Int, block: …): KexpressoBuilder` | 🟢 | Quantifier DSL. |
| `KexpressoBuilder.atLeast` | `fun atLeast(n: Int, greedy: Boolean = true, block: …): KexpressoBuilder` | 🟢 | Quantifier DSL. |
| `KexpressoBuilder.between` | `fun between(min: Int, max: Int, greedy: Boolean = true, block: …): KexpressoBuilder` | 🟢 | Quantifier DSL. |
| `KexpressoBuilder.group` | `fun group(block: …): KexpressoBuilder` | 🟢 | Non-capturing group. |
| `KexpressoBuilder.capture` (anonymous) | `fun capture(block: …): KexpressoBuilder` | 🟢 | Capturing group. |
| `KexpressoBuilder.capture` (named) | `fun capture(name: String, block: …): KexpressoBuilder` | 🟢 | Named capturing group. |
| `KexpressoBuilder.oneOf` | `fun oneOf(vararg blocks: KexpressoBuilder.() -> Unit): KexpressoBuilder` | 🟢 | Alternation; consistent with other names. |
| `KexpressoBuilder.followedBy` | `fun followedBy(block: …): KexpressoBuilder` | 🟢 | Lookahead. |
| `KexpressoBuilder.notFollowedBy` | `fun notFollowedBy(block: …): KexpressoBuilder` | 🟢 | Lookahead. |
| `KexpressoBuilder.precededBy` | `fun precededBy(block: …): KexpressoBuilder` | 🟢 | Lookbehind. |
| `KexpressoBuilder.notPrecededBy` | `fun notPrecededBy(block: …): KexpressoBuilder` | 🟢 | Lookbehind. |
| `KexpressoBuilder.raw` | `fun raw(pattern: String): KexpressoBuilder` | 🟢 | Intentional escape hatch; clearly documented as unsafe. |
| `KexpressoBuilder.backreference` (Int) | `fun backreference(n: Int): KexpressoBuilder` | 🟢 | Back-reference; validated at call site. |
| `KexpressoBuilder.backreference` (String) | `fun backreference(name: String): KexpressoBuilder` | 🟢 | Named back-reference. |
| `KexpressoBuilder.include` | `fun include(pattern: KexpressoPattern): KexpressoBuilder` | 🟢 | Pattern composition; well-documented. |

---

### `Ast.kt`

All AST types are declared `internal`. No public symbols leak from this file. This is correct: the
AST is an implementation detail.

| Symbol | Mark | Rationale |
|---|---|---|
| All (`RegexNode`, `SequenceNode`, `Token`, `Literal`, `Raw`, `QuantifierKind`, `Quantifier`, `GroupKind`, `Group`, `Alternation`, `LookaroundKind`, `Lookaround`, `Backreference`) | 🟠 (already internal) | Correctly `internal`. No action needed. |
| `escapeLiteral` | 🟠 (already internal) | `internal fun`; correct. |

> Note: `KexpressoPattern.ast` is declared `internal val ast: RegexNode`. The type `RegexNode` is
> `internal sealed interface`. So the internal AST does not form part of the public API surface —
> this is correct. No action needed here.

---

### `Analysis.kt`

| Symbol | Signature (abbreviated) | Mark | Rationale |
|---|---|---|
| `ReDoSSeverity` | `enum class ReDoSSeverity { WARNING }` | 🟡 | The enum has only one value today. The KDoc explicitly says "additional levels may be added." Freezing this as a stable API risks source compatibility when new values are added (exhaustive `when` in user code). Mark `@ExperimentalKexpressoApi`. |
| `ReDoSFinding` | `data class ReDoSFinding(message, index, severity)` | 🟡 | Data class; its shape may evolve as the heuristic matures (e.g., adding a `snippet` field, changing `index` semantics). Part of a best-effort feature. Mark `@ExperimentalKexpressoApi`. |
| `ReDoSReport` | `data class ReDoSReport(findings)` | 🟡 | Same reasoning as `ReDoSFinding`; also carries `isPotentiallyVulnerable`. Mark `@ExperimentalKexpressoApi`. |
| `KexpressoPattern.analyze` | `fun KexpressoPattern.analyze(): ReDoSReport` | 🟡 | Explicitly documented as "best-effort heuristic, not a proof." The analysis rules may change between minor versions. `@ExperimentalKexpressoApi` is the correct signal. |
| `KexpressoPattern.isPotentiallyVulnerable` | `val KexpressoPattern.isPotentiallyVulnerable: Boolean` | 🟡 | Convenience wrapper over `analyze()`; inherits the same best-effort caveat. Mark `@ExperimentalKexpressoApi`. |

---

### `Captures.kt`

| Symbol | Signature (abbreviated) | Mark | Rationale |
|---|---|---|
| `Captures` | `class Captures` | 🟢 | Typed access over `MatchResult`; well-designed. `internal constructor` prevents external instantiation — correct. |
| `Captures.string(name)` | `fun string(name: String): String?` | 🟢 | Core accessor. |
| `Captures.int(name)` | `fun int(name: String): Int?` | 🟢 | Core accessor. |
| `Captures.long(name)` | `fun long(name: String): Long?` | 🟢 | Core accessor. |
| `Captures.double(name)` | `fun double(name: String): Double?` | 🟢 | Core accessor. |
| `Captures.boolean(name)` | `fun boolean(name: String): Boolean?` | 🟢 | Core accessor. |
| `Captures.string(index)` | `fun string(index: Int): String?` | 🟢 | Core accessor. |
| `Captures.int(index)` | `fun int(index: Int): Int?` | 🟢 | Core accessor. |
| `Captures.long(index)` | `fun long(index: Int): Long?` | 🟢 | Core accessor. |
| `Captures.double(index)` | `fun double(index: Int): Double?` | 🟢 | Core accessor. |
| `Captures.boolean(index)` | `fun boolean(index: Int): Boolean?` | 🟢 | Core accessor. |
| `Captures.stringOrThrow(name)` | `fun stringOrThrow(name: String): String` | 🟢 | Throwing variant for named groups; idiomatic. |
| `Captures.intOrThrow(name)` | `fun intOrThrow(name: String): Int` | 🟢 | Throwing variant. |
| `Captures.longOrThrow(name)` | `fun longOrThrow(name: String): Long` | 🟢 | Throwing variant. |
| `Captures.doubleOrThrow(name)` | `fun doubleOrThrow(name: String): Double` | 🟢 | Throwing variant. |
| `Captures.booleanOrThrow(name)` | `fun booleanOrThrow(name: String): Boolean` | 🟢 | Throwing variant. |
| `MatchResult.captures` | `val MatchResult.captures: Captures` | 🟢 | Natural extension entry point. |

> Note: `OrThrow` variants exist only for named-group access, not for index-based access. This is
> an intentional gap (named groups are the recommended style) but worth flagging as a cross-cutting
> concern — see below.

---

### `Domains.kt`

All 16 extension functions follow the same pattern: `fun KexpressoBuilder.X(): KexpressoBuilder = append(rawRegex)`. They all use the `internal` `append()` shim, so they emit `Raw` nodes and are subject to the same "best-effort generation/describe" caveat as any `raw()` call.

| Symbol | Mark | Rationale |
|---|---|---|
| `ipv4`, `uuid`, `slug`, `hexColor`, `semanticVersion`, `isoDate`, `isoTime`, `integerNumber`, `decimalNumber`, `hashtag`, `mention`, `e164Phone`, `ipv6`, `macAddress`, `base64`, `jwt` | 🟡 | All 16 domain helpers emit `Raw` nodes, so `examples()`, `describe()`, and `toKexpressoCode()` degrade gracefully but with reduced fidelity. The regex bodies are non-trivial and may need maintenance (e.g., UUID version coverage, e-mail edge cases, base64 padding semantics). These are best treated as a versioned, opt-in utility belt rather than frozen primitives. Mark the entire group `@ExperimentalKexpressoApi`. |

---

### `Generate.kt`

| Symbol | Signature (abbreviated) | Mark | Rationale |
|---|---|---|
| `KexpressoPattern.examples` | `fun KexpressoPattern.examples(count: Int = 5, seed: Long = 0): List<String>` | 🟡 | KDoc clearly states it is best-effort for `Raw`, `Lookaround`, and `Backreference` nodes — i.e., for any pattern containing domain helpers (`ipv4()`, `email()`, …), lookarounds, or back-references the generated strings may NOT satisfy `matches()`. This is exactly the kind of feature that should be `@ExperimentalKexpressoApi` until a more complete generation strategy is in place. |

---

### `Reverse.kt`

| Symbol | Signature (abbreviated) | Mark | Rationale |
|---|---|---|
| `Kexpresso.from` | `fun Kexpresso.from(regex: String): KexpressoPattern` | 🟡 | Matching correctness is guaranteed; AST reconstruction is explicitly "best-effort." The parser does not cover atomic groups, inline flags, possessive quantifiers, or Unicode property escapes — these all degrade to `Raw`. Useful but inherently incomplete. Mark `@ExperimentalKexpressoApi`. |
| `KexpressoPattern.toKexpressoCode` | `fun KexpressoPattern.toKexpressoCode(): String` | 🟡 | Depends on AST quality. Patterns containing domain helpers (which always emit `Raw`) won't round-trip cleanly; patterns from `Kexpresso.from(regex)` round-trip for the parser's supported subset, with `Raw` fallback for unmodelled constructs (atomic groups, possessive quantifiers, inline flags). Mark `@ExperimentalKexpressoApi`. |
| `KexpressoCodeGenerator` | `internal object KexpressoCodeGenerator` | 🟠 (already internal) | Correctly `internal`. No action needed. |
| `KexpressoCodeGenerator.INDENT_UNIT` | `const val INDENT_UNIT: String` | 🟠 | `INDENT_UNIT` is `internal` on an `internal object`, so it is effectively non-public. Correct. |
| `RegexParser` | `internal class RegexParser` | 🟠 (already internal) | Correctly `internal`. No action needed. |

---

### `Text.kt`

| Symbol | Signature (abbreviated) | Mark | Rationale |
|---|---|---|
| `KexpressoBuilder.word` | `fun KexpressoBuilder.word(): KexpressoBuilder` | 🟡 | `word()` matches `[a-zA-Z0-9]+` — not what most readers associate with the English word "word" (which typically includes hyphens and apostrophes). Also clashes conceptually with `wordChar()` (which matches `\w`). The name is ambiguous enough to be a footgun. Mark `@ExperimentalKexpressoApi` and consider renaming to `alphanumericWord()` or `identifier()` before 1.0. |
| `KexpressoBuilder.pseudo` | `fun KexpressoBuilder.pseudo(): KexpressoBuilder` | 🔴 | The name `pseudo` is cryptic out of context — it refers to a "pseudo-identifier" (`[a-zA-Z0-9_-]+`) but nothing about the name conveys that to a new user. Rename to `slugWord()`, `handle()`, or `identifier()` before 1.0. This is the clearest rename candidate in the library. |
| `KexpressoBuilder.email` | `fun KexpressoBuilder.email(): KexpressoBuilder` | 🟡 | Same `Raw`-node caveat as all domain helpers; additionally, the docstring says "intentionally permissive and not RFC-5321-complete," which is exactly the signal for `@ExperimentalKexpressoApi`. |
| `KexpressoBuilder.url` | `fun KexpressoBuilder.url(): KexpressoBuilder` | 🟡 | Same `Raw`-node caveat. "Very long or exotic URLs may not match" is an honest limitation that warrants an opt-in annotation. |

---

### `Writing.kt`

| Symbol | Signature (abbreviated) | Mark | Rationale |
|---|---|---|
| `KexpressoBuilder.sentence` | `fun KexpressoBuilder.sentence(): KexpressoBuilder` | 🟡 | Builds on `word()` and `space()` (the deprecated alias), so it inherits their issues. Also depends on the specific ASCII-letter-based `letter()` semantics. Natural-language patterns are inherently best-effort. Mark `@ExperimentalKexpressoApi`. |
| `KexpressoBuilder.paragraph` | `fun KexpressoBuilder.paragraph(): KexpressoBuilder` | 🟡 | Builds on `sentence()`; same reasoning. Mark `@ExperimentalKexpressoApi`. |

> Note: `Writing.kt` uses `space()` (the deprecated alias) rather than `whitespace()`. This should
> be fixed as part of the `space()` deprecation to avoid churn.

---

### `Jvm.kt`

| Symbol | Signature (abbreviated) | Mark | Rationale |
|---|---|---|
| `KexpressoPattern.toPattern` | `fun KexpressoPattern.toPattern(): java.util.regex.Pattern` | 🟢 | JVM-only interop; straightforward delegation. Platform-specific placement is correct. |

---

## Cross-cutting concerns

- **`capitalLetter()` vs `lowercaseLetter()` asymmetry** (`Kexpresso.kt`): The two sibling
  methods use different naming conventions — `capitalLetter` (adjective-noun) vs `lowercaseLetter`
  (adjective-noun). The asymmetry is that `capital` and `lowercase`/`uppercase` are not parallel;
  the natural Kotlin pair would be `uppercaseLetter()` / `lowercaseLetter()`. Freezing
  `capitalLetter()` for 1.0 would commit the library to an odd API forever.

- **`space()` alias living alongside `whitespace()`** (`Kexpresso.kt`): Two public methods doing
  exactly the same thing. The KDoc says "kept for backward compatibility," which implies it was
  already a mistake. It must be deprecated with `@Deprecated(ReplaceWith("whitespace()"))` before
  the freeze. `Writing.kt`'s `sentence()` must be updated too (it currently calls `space()`
  internally).

- **`word()` name collision risk** (`Text.kt`): `word()` matches `[a-zA-Z0-9]+` while `wordChar()`
  matches `\w`. The names are close enough that users will confuse them; `wordChar()` is a
  single-character class, `word()` is a one-or-more quantified class. The distinction is not obvious
  from the names. Consider `alphanumericWord()` or a deprecation+rename before 1.0.

- **`pseudo()` is opaque** (`Text.kt`): Of all public names, `pseudo()` is the one most likely
  to cause a WTF reaction in a new user. It is not a standard term in the regex or web development
  world (the docstring explains it means "pseudo-identifier"), but the name alone gives no hint.
  This is the single highest-priority rename in the library.

- **`OrThrow` coverage asymmetry** (`Captures.kt`): The throwing variants (`stringOrThrow`,
  `intOrThrow`, etc.) exist only for named-group access. Index-based access has no `OrThrow`
  overloads. This is documented nowhere. Before 1.0, either add the index-based `OrThrow`
  variants for consistency, or document the intentional asymmetry ("named groups are the
  recommended style and provide the full API").

- **`@ExperimentalKexpressoApi` annotation does not yet exist**: The entire opt-in mechanism must
  be introduced as a new public annotation before the `@OptIn` marks can be applied. This is a
  pre-condition for all 🟡 actions.

- **Domain helpers (`Domains.kt`) all use `append(raw)` shim, losing AST fidelity**: Every
  domain helper degrades `describe()`, `toKexpressoCode()`, and `examples()`. This is a systemic
  architectural gap. The helpers could be rewritten to emit proper AST nodes, but that is a
  substantial refactor. For 1.0, marking them `@ExperimentalKexpressoApi` is the pragmatic choice;
  a richer AST encoding can be a 1.x improvement.

- **`KexpressoBuilder.build()` is public and the builder is mutable**: `build()` is called by
  both `kexpresso {}` and `Kexpresso.pattern {}` internally. It being public means advanced users
  can hold a builder, call `build()` once, add more primitives, and call `build()` again — getting
  two different patterns from the same builder instance. This is not documented and may be
  surprising. Either document it explicitly (and commit to it as a feature), or make `build()`
  `internal` and remove the escape hatch.

- **`Raw` nodes degrade introspection — but not uniformly**: Two distinct classes hit this,
  and conflating them would mislead the freeze plan.
  - **Always degraded:** every domain helper in `Domains.kt` calls `append(raw)`, so any
    pattern containing one carries `Raw` nodes regardless of complexity.
  - **Sometimes degraded:** `Kexpresso.from(regex)` runs `RegexParser(regex).parse()` first
    and **produces a real, structured AST when the parser understands the input** — which
    covers the common shapes (sequences, literals, character classes, anchors, quantifiers,
    groups, alternations). Only constructs the parser can't model — atomic groups, possessive
    quantifiers, inline flags, certain backreferences, Unicode property escapes — fall back
    to `Raw`. So most imported regexes describe and round-trip cleanly.

  `KexpressoPattern.describe()` returns `raw regex \`…\`` for `Raw` nodes, and `examples()`
  emits empty strings for them. Adding a note to the `describe()` and `examples()` KDocs
  that introspection quality depends on AST completeness — with a pointer to the two classes
  above — would set expectations correctly without overstating the degradation.

---

## Recommended next steps

1. **Introduce `@ExperimentalKexpressoApi`** — one `@RequiresOptIn` annotation. This is the
   pre-condition for all 🟡 actions.
2. **Apply the 🔴 renames before any other change** (lowest breakage risk at this stage):
   a. Deprecate `space()` with `@Deprecated(ReplaceWith("whitespace()"))`.
   b. Rename `capitalLetter()` to `uppercaseLetter()` (keep deprecated alias).
   c. Rename `pseudo()` to a clearer name (e.g., `handle()` or `slugWord()`).
   d. Evaluate `word()` rename (e.g., `alphanumericWord()`).
3. **Fix `Writing.kt`**: replace the `space()` call inside `sentence()` with `whitespace()`.
4. **Apply 🟡 `@ExperimentalKexpressoApi` annotations** to: `KexpressoPattern(source, regex)`
   constructor, `KexpressoBuilder.build`, the entire `ReDoS*` surface, `examples()`, `Kexpresso.from`,
   `toKexpressoCode()`, all 16 domain helpers in `Domains.kt`, `email()`, `url()`, `word()`,
   `sentence()`, `paragraph()`.
5. **Decide on `OrThrow` symmetry** in `Captures`: either add index-based `OrThrow` methods or
   add an explicit "named groups are primary" note to the class KDoc.
6. **Decide on `KexpressoBuilder.build` visibility**: if no legitimate public use case exists
   outside the entry-point functions, make it `internal`.
7. **Cut `0.9.0 = API freeze candidate`** once 🟠 (already done), 🔴, and 🟡 are applied.
8. **Soak 3–6 months** under real-world usage before cutting 1.0.0.

---

## Counts

| Mark | Count |
|---|---|
| 🟢 Keep | 63 |
| 🟡 @OptIn | 29 |
| 🟠 Internal (already internal, no action) | 14 |
| 🔴 Rename/refactor | 4 |
| **Total (public symbols requiring a decision)** | **96** |
