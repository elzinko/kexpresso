# When to use kexpresso — and when not to

This page is deliberately honest. kexpresso is **not** the right tool for every regex,
and pretending otherwise would waste your time. Here is the straight story, backed by
measured numbers, so you can decide.

## The one thing to understand first

A kexpresso pattern **compiles to a plain `java.util.regex.Pattern`**. At match time it
*is* a regular expression — there is no interpreter, no wrapper cost, no magic. So the
question is never *"is kexpresso fast enough?"* (it's identical to raw regex). The only
real question is: **does the DSL make this particular pattern easier to write, read,
review, and keep correct than the raw string would?** Sometimes yes. Often no. Read on.

## ✅ Use kexpresso when…

- **The pattern is complex or long-lived** — something you'll come back to, that a
  teammate will review, that you'd hesitate to refactor as a raw string.
- **You assemble it from reusable parts** — define a sub-pattern once and reuse it with
  `include(...)` instead of copy-pasting regex fragments.
- **You want literals matched safely** — `literal("a.b")` auto-escapes; you can't forget
  to escape a `.` and silently match `axb`.
- **You need a common format** — `email()`, `ipv4()`, `uuid()`, `isoDate()`,
  `semanticVersion()`, `slug()`, … are tested, often *shorter* than the hand-written
  regex, and frequently *more correct* (e.g. `isoDate()` constrains the month to `01–12`).
- **Reviewability matters** — a kexpresso diff reads like prose; a raw-regex diff is a
  wall of backslashes.

## ❌ Don't bother with kexpresso when…

- **The pattern is trivial** — for `\d+`, `\s*`, or a simple split, raw `Regex` is shorter
  and every Kotlin developer reads it instantly. The DSL is **3–7× more verbose** here
  (see the table). Use the standard library.
- **You're writing a throwaway one-liner** in a script — the dependency isn't worth it.
- **You need a regex feature the DSL doesn't expose yet** — though `raw("…")` is an escape
  hatch, at that point you're writing regex anyway.
- **You expect it to validate *meaning*** — `isoDate()` matches the *shape* `YYYY-MM-DD`;
  it will happily match `2024-02-30`. Structural ≠ semantic validation.

## The numbers (measured, reproducible via `./gradlew jmh`)

Environment: OpenJDK 17 (Temurin/MS build), macOS, JMH 1.36. Pattern under test: an
anchored ISO-8601 date. Reproduce with `./gradlew jmh` (see [`benchmarks/`](../benchmarks/README.md)).

### Match-time cost — *parity*

| Benchmark | Time | |
|---|---|---|
| `KexpressoPattern.matches()` | **43.3 ns/op** | |
| Raw pre-compiled `Regex.matches()` | **43.7 ns/op** | within noise → **0 % overhead** |

> kexpresso adds **no measurable cost at match time**. It is the same compiled pattern.

### Construction cost — *paid once, then amortized*

| Benchmark | Time | |
|---|---|---|
| `kexpresso { … }` (build + compile) | **0.29 µs/op** | |
| Raw `Regex("…")` (compile only) | **0.19 µs/op** | ~0.1 µs extra for the builder |

> The ~0.1 µs DSL overhead is paid **once** when you build the pattern. Build patterns
> once and reuse them (as you should with any regex) and it disappears into the noise
> after the first match.

### Footprint

| | |
|---|---|
| Jar size | **~13 KB** |
| Transitive runtime dependencies | **0** (only `kotlin-stdlib`, which you already have) |

### Verbosity — the honest trade-off (character counts)

| Pattern | Raw regex | kexpresso | Winner |
|---|---:|---:|---|
| One-or-more digits | `\d+` → **3** | `oneOrMore { digit() }` → 21 | 🔴 **Regex** (×7 shorter) |
| `key: "value"` with captures | **18** | ~130 | 🔴 **Regex** (×7 shorter) |
| ISO-8601 date | 17 | `isoDate()` → **9** *(and more correct)* | 🟢 **kexpresso** |
| Anchored email | 48 | `startOfText(); email(); endOfText()` → **36** | 🟢 **kexpresso** |

**The pattern in the data:** kexpresso's brevity wins come from the **ready-made helpers**,
never from the low-level builder. For raw patterns you'd hand-write, kexpresso is *more*
verbose — it trades characters for readability and safety. That trade is worth it for
complex, maintained patterns and a bad deal for trivial ones.

## What it brings vs. what it doesn't — at a glance

| Brings | Doesn't bring |
|---|---|
| Readability for complex patterns | Brevity for simple patterns (it's longer) |
| Auto-escaped literals (a real bug class gone) | Faster authoring of a quick `\d+` |
| Structurally balanced groups (can't be unbalanced) | Semantic validation (shape only) |
| Eager group-name validation (clear errors) | ReDoS protection *(planned for 0.3.x)* |
| Composition & reuse (`include`) | A multiplatform build *(JVM-only today)* |
| 12 tested domain helpers | A replacement for understanding regex |
| 0 % runtime cost, ~13 KB, 0 deps | |

## Verdict by persona

**The Kotlin purist.** You're right to reach for the standard library on simple patterns —
kexpresso is *longer* there and you read regex fine. Where kexpresso earns its place in
your codebase is **the pattern you're afraid to refactor**: the 80-character monster in a
validator. There, auto-escaping, balanced groups, reusable sub-patterns, and (soon) ReDoS
safety buy you confidence the raw string can't. Take `kexpresso-core` (13 KB, zero deps)
and ignore the rest.

**The pragmatic team that needs regex.** This is kexpresso's sweet spot. You get common
formats right on the first try (`email`, `ipv4`, `uuid`, `isoDate`…), your code reviews
become readable, your literals are escaped for you, and you ship fewer regex bugs — at
zero runtime cost. Use the helpers liberally; drop to the builder for the bespoke parts.

## Rule of thumb

> **If the pattern fits in your head and you'd write it in five seconds, use `Regex`.
> If you'd hesitate to refactor it in a code review, use kexpresso.**
