# Architecture

A map of the codebase for anyone who wants to read, fix, or extend kexpresso.
Everything below lives in `src/commonMain/kotlin/kexpresso/` unless stated otherwise.

## Bird's-eye view

```
   kexpresso { … }    Kexpresso.pattern { … }        Kexpresso.from("\\d+")
          │                    │                              │
          ▼                    ▼                              ▼
       KexpressoBuilder (Kexpresso.kt)              RegexParser (Reverse.kt)
          │  each DSL call appends a node             │  best-effort parse of an
          │                                           │  existing regex string
          └──────────────┬────────────────────────────┘
                         ▼
                RegexNode AST (Ast.kt)
                 │                │
        render() │                │ describe()
                 ▼                ▼
          source: String     English description
                 │
                 ▼
          kotlin.text.Regex   ← ALL matching is delegated here
                 │
                 ▼
          KexpressoPattern (Kexpresso.kt)
       ┌─────────────┼────────────────┬──────────────────────┐
       ▼             ▼                ▼                      ▼
  examples()     analyze()      toKexpressoCode()    find(…)?.captures
 (Generate.kt) (Analysis.kt)     (Reverse.kt)     (Captures.kt — extension
  walks the AST  scans `source`   walks the AST     on the MatchResult)
```

Two ideas carry the whole design:

1. **kexpresso never matches anything itself.** Every pattern compiles to a plain
   `kotlin.text.Regex`, and `matches`/`find`/`replaceAll`/… delegate to it. The library's
   job ends at *producing* (or *reading*) the regex string.
2. **The AST is the spine.** The builder does not concatenate strings; it accumulates
   `RegexNode`s. The same tree then powers four read-only features: `render()` (the
   `source` string), `describe()` (English), `examples()` (sample inputs), and
   `toKexpressoCode()` (DSL codegen).

## File map

| File | Role | Key types / entry points |
|---|---|---|
| `Kexpresso.kt` | Public surface: entry points, the compiled pattern, the builder | `kexpresso {}`, `Kexpresso.pattern {}`, `KexpressoPattern`, `KexpressoBuilder` |
| `Ast.kt` | Internal AST — every construct knows how to `render()` and `describe()` itself | `RegexNode`, `Token`, `Literal`, `Raw`, `Quantifier`, `Group`, `Alternation`, `Lookaround`, `Backreference`, `escapeLiteral()` |
| `Captures.kt` | Typed access to captured groups (`int("year")`, `stringOrThrow(…)`) | `Captures`, `MatchResult.captures` |
| `Text.kt` | Small text helpers | `word()`, `handle()`, `email()`, `url()` |
| `Writing.kt` | Natural-language helpers composed from primitives | `sentence()`, `paragraph()` |
| `Domains.kt` | Domain helpers backed by hardcoded regex fragments | `ipv4()`, `uuid()`, `isoDate()`, `jwt()`, … (16 in total) |
| `Analysis.kt` | Best-effort static ReDoS detection | `analyze()`, `ReDoSReport`, internal `ReDoSScanner` |
| `Generate.kt` | Produces strings that match a pattern, by walking the AST | `examples()` |
| `Reverse.kt` | The other direction: raw regex in → pattern + AST out, and AST → DSL source | `Kexpresso.from()`, `toKexpressoCode()`, internal `RegexParser` |
| `ExperimentalKexpressoApi.kt` | Opt-in marker for pre-1.0 evolving APIs | `@ExperimentalKexpressoApi` |
| `src/jvmMain/…/Jvm.kt` | JVM-only conveniences | `KexpressoPattern.toPattern()` |

## How a pattern is built

`kexpresso { digit(); oneOrMore { letter() } }` executes the block against a
`KexpressoBuilder`. Each DSL call appends one `RegexNode` to an internal list — `digit()`
appends `Token("\\d", "a digit")`, block-taking methods like `oneOrMore { … }` run the
block in a *child* builder and wrap its nodes. `build()` then wraps the list in a
`SequenceNode`, calls `render()` to get the `source` string, compiles it with
`Regex(source)`, and returns the immutable `KexpressoPattern(source, regex, ast)`.

Argument problems (negative counts, empty character classes, invalid group names) are
rejected with `require(...)` at the call site, so users get a clear
`IllegalArgumentException` instead of an engine-specific syntax error later.

## The other direction: `Kexpresso.from()`

`Reverse.kt` reads an *existing* regex string. Matching correctness is guaranteed by
construction — the input string is compiled verbatim and kept as `source`. In parallel, a
small recursive-descent parser (`RegexParser`) rebuilds a best-effort AST used only by
`describe()` and `toKexpressoCode()`. Anything the parser cannot model (atomic groups,
inline flags, possessive quantifiers) degrades to a `Raw` node, never an error.

## The `Raw` node — the one known gap

`Raw` holds a regex fragment as an opaque string. It comes from three places:
`raw(...)` in the DSL, every helper in `Text.kt`/`Domains.kt` (they emit their fragment
through the internal `append(...)` shim), and unmodelled constructs in `RegexParser`.

A `Raw` node renders and matches perfectly, but the introspection features degrade on it:
`describe()` says ``raw regex `…` ``, `examples()` emits an empty string for the fragment,
and `toKexpressoCode()` falls back to `raw("…")`. This is why the domain helpers are
`@ExperimentalKexpressoApi`: the plan for 1.x is to re-encode them as structured AST.

## Multiplatform notes

- All logic lives in `commonMain`; the only platform source is `Jvm.kt` (9 lines).
- `escapeLiteral()` in `Ast.kt` is hand-written because `Regex.escape`/`\Q…\E` do not
  exist on Kotlin/JS. Generated sources must stay valid for both `java.util.regex` and
  ECMAScript engines.
- Some constructs compile on one engine only — e.g. the `\A`/`\z` anchors
  (`startOfText()`/`endOfText()`) are rejected by the JS engine. That is the main reason
  the test suite is split (see below).
- Targets are registered host-conditionally in `build.gradle.kts` (Apple targets need a
  full Xcode install; the release runs on macOS to publish every target at once).

## Adding a new primitive

A new builder primitive is more than one method. Checklist:

1. **`Kexpresso.kt`** — add the method on `KexpressoBuilder`, appending a `Token` (fixed
   fragment) or a new node type in `Ast.kt` (structured construct). Validate arguments
   with `require(...)`.
2. **`Reverse.kt`** — add the fragment to `KexpressoCodeGenerator.TOKEN_CALLS` so
   `toKexpressoCode()` emits your method instead of `raw("…")`; if it is an escape
   sequence, also add it to `RegexParser.PREDEFINED_ESCAPES` so `Kexpresso.from()`
   recognises it.
3. **`Generate.kt`** — add a generator to `TOKEN_GENERATORS` so `examples()` can produce
   matching strings (otherwise it falls back to a placeholder).
4. **Tests** — portable behaviour in `src/commonTest/`, JVM-only syntax in
   `src/jvmTest/`.
5. **README** — add a row to the DSL reference table.
6. **CHANGELOG** — an entry under `[Unreleased]`.

Forgetting steps 2–3 does not break anything — the features degrade to `raw("…")` /
placeholder output — but the primitive will feel second-class.

## Test layout

| Source set | What belongs there | Why |
|---|---|---|
| `src/commonTest/` | Portable behaviour that must hold on JVM, JS *and* Wasm (`CommonPortableTest` asserts the rendered `source` of the portable tokens) | Runs on every target in CI; cannot use `\A`/`\z` or other JVM-only syntax |
| `src/jvmTest/` | The bulk of the suite: builder details, domain helpers, ReDoS scanner, reverse parsing | Uses backtick test names and JVM-only regex behaviour freely |

The build enforces Detekt (per-source-set tasks) and a Kover line-coverage floor of 85%
(`./gradlew build` runs everything).

## Where to start reading

1. `Kexpresso.kt` top to bottom — entry points, `KexpressoPattern`, then the builder.
2. `Ast.kt` — small, and everything else makes sense once you have seen it.
3. Then whichever satellite you care about: `Reverse.kt` is the most intricate
   (parser + codegen), `Analysis.kt` is self-contained, `Generate.kt` is the simplest.
