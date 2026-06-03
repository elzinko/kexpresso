# Kexpresso ☕

**A fluent Kotlin DSL that makes regular expressions readable.**

[![CI](https://github.com/elzinko/kexpresso/actions/workflows/ci.yml/badge.svg)](https://github.com/elzinko/kexpresso/actions/workflows/ci.yml)
[![JitPack](https://jitpack.io/v/elzinko/kexpresso.svg)](https://jitpack.io/#elzinko/kexpresso)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.8.20-7F52FF?logo=kotlin)](https://kotlinlang.org)
[![API docs](https://img.shields.io/badge/API_docs-Dokka-blue)](https://elzinko.github.io/kexpresso/)

---

## Why kexpresso?

Raw regular expressions are write-only. A week after authoring one, even the writer
struggles to remember what it does:

```kotlin
// Raw regex — what does this match?
val emailRegex = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
```

With kexpresso the same constraint reads like English:

```kotlin
// kexpresso — self-documenting and composable
val emailPattern = kexpresso {
    email()
}
```

Or, for a richer pattern that you build up incrementally:

```kotlin
val strictEmail = kexpresso {
    startOfText()
    email()
    endOfText()
}
strictEmail.matches("barista@coffee.shop") // true
strictEmail.matches("not an email")        // false
```

Benefits at a glance:

- **Readable** — the DSL reads top-to-bottom like a description of what you want to match.
- **Type-safe** — the compiler catches typos that a raw string never would.
- **Composable** — build complex patterns from simple named primitives.
- **Zero runtime overhead** — the DSL compiles to a plain `Regex` at construction time
  (measured: **0 % match-time overhead** vs raw `Regex` — see [benchmarks](benchmarks/README.md)).

> **Is kexpresso right for your case?** We're honest about it: it's great for complex,
> maintained patterns and a poor fit for trivial ones. Read
> **[When to use kexpresso — and when not to](docs/WHEN-TO-USE.md)** before adopting.
> Where we're headed: the **[Roadmap](docs/ROADMAP.md)**.

---

## Install

Kexpresso is distributed via [JitPack](https://jitpack.io/#elzinko/kexpresso).
Any git tag or commit hash can be used as the version.

### Gradle (Kotlin DSL)

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://jitpack.io") }
    }
}

// build.gradle.kts
dependencies {
    implementation("com.github.elzinko:kexpresso:0.1.0")
}
```

### Maven

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.elzinko</groupId>
    <artifactId>kexpresso</artifactId>
    <version>0.1.0</version>
</dependency>
```

---

## Quickstart

### 1 — Compile a pattern and test a full match

```kotlin
val drinkName = kexpresso {
    capitalLetter()
    oneOrMore { letter() }
}

drinkName.matches("Espresso")   // true
drinkName.matches("espresso")   // false  (no capital first letter)
drinkName.matches("Espresso42") // false  (digit at the end)
```

### 2 — Extract all words from a coffee order

```kotlin
val wordPattern = kexpresso { word() }

val order = "Espresso Latte Cappuccino"
val drinks = wordPattern.findAll(order).map { it.value }.toList()
// ["Espresso", "Latte", "Cappuccino"]
```

### 3 — Validate an email address

```kotlin
val emailValidator = kexpresso {
    startOfText()
    email()
    endOfText()
}

emailValidator.matches("barista@coffee.shop")       // true
emailValidator.matches("barista@coffee.shop extra") // false
emailValidator.matches("not-an-email")              // false
```

### 4 — Match a well-formed sentence

```kotlin
val sentencePattern = kexpresso { sentence() }

sentencePattern.matches("Espresso is perfect!")       // true
sentencePattern.matches("espresso is lowercase.")     // false
sentencePattern.matches("No punctuation at the end")  // false
```

---

## DSL reference

### Primitives

| Method | Regex produced | Notes |
|---|---|---|
| `literal(text)` | `\Qtext\E` | Escapes all regex metacharacters |
| `char(c)` | escaped char | Escapes metacharacters |
| `digit()` | `\d` | Decimal digit 0–9 |
| `nonDigit()` | `\D` | Any non-digit |
| `whitespace()` | `\s` | Space, tab, newline, … |
| `space()` | `\s` | Alias for `whitespace()` |
| `nonWhitespace()` | `\S` | Any non-whitespace |
| `wordChar()` | `\w` | Letter, digit, or `_` |
| `nonWordChar()` | `\W` | Not a word character |
| `anyChar()` | `.` | Any character except newline |
| `letter()` | `[a-zA-Z]` | ASCII letters only |
| `capitalLetter()` | `[A-Z]` | ASCII uppercase letters |
| `lowercaseLetter()` | `[a-z]` | ASCII lowercase letters |
| `alphanumeric()` | `[a-zA-Z0-9]` | ASCII letter or digit |
| `tab()` | `\t` | Horizontal tab |
| `newline()` | `\n` | Newline |
| `carriageReturn()` | `\r` | Carriage return |
| `nonWordBoundary()` | `\B` | Non-word boundary position |
| `endPunctuation()` | `[.!?]` | Sentence-ending punctuation |

### Character classes

| Method | Regex produced | Notes |
|---|---|---|
| `anyOf(chars)` | `[chars]` | One character from the given set; metacharacters escaped |
| `noneOf(chars)` | `[^chars]` | One character NOT in the given set |
| `inRange(from, to)` | `[from-to]` | One character in the inclusive range |

### Anchors

| Method | Regex produced | Notes |
|---|---|---|
| `startOfLine()` | `^` | Use with `RegexOption.MULTILINE` for per-line anchoring |
| `endOfLine()` | `$` | Use with `RegexOption.MULTILINE` for per-line anchoring |
| `startOfText()` | `\A` | Anchors to the very beginning of the input |
| `endOfText()` | `\z` | Anchors to the very end of the input |
| `wordBoundary()` | `\b` | Transition between word and non-word character |

### Quantifiers

All quantifiers accept an optional `greedy: Boolean` parameter (default `true`).
Pass `greedy = false` to make the quantifier lazy (matches as few characters as possible).

| Method | Regex produced | Notes |
|---|---|---|
| `optional { }` | `(?:...)?` | Zero or one occurrence |
| `zeroOrMore { }` | `(?:...)*` | Zero or more occurrences |
| `oneOrMore { }` | `(?:...)+` | One or more occurrences |
| `exactly(n) { }` | `(?:...){n}` | Exactly n occurrences |
| `atLeast(n) { }` | `(?:...){n,}` | At least n occurrences |
| `between(min, max) { }` | `(?:...){min,max}` | Between min and max occurrences (inclusive) |

**Lazy example:**

```kotlin
val lazyDigits = kexpresso {
    startOfText()
    oneOrMore(greedy = false) { digit() }
    endOfText()
}
lazyDigits.matches("42") // true
```

### Grouping and alternation

| Method | Regex produced | Notes |
|---|---|---|
| `group { }` | `(?:...)` | Non-capturing group |
| `capture { }` | `(...)` | Numbered capturing group |
| `capture("name") { }` | `(?<name>...)` | Named capturing group |
| `oneOf({ }, { }, …)` | `(?:a\|b\|…)` | Alternation: matches any one of the given patterns |

**Named capture example:**

```kotlin
val orderPattern = kexpresso { literal(": "); capture("drink") { word() } }

val result = orderPattern.find("Order: Cappuccino please")
result?.groups?.get("drink")?.value // "Cappuccino"
```

**Alternation example:**

```kotlin
val drinkMenu = kexpresso {
    oneOf(
        { literal("Espresso") },
        { literal("Latte") },
        { literal("Cappuccino") },
    )
}

drinkMenu.matches("Latte")     // true
drinkMenu.matches("Americano") // false
```

### Lookarounds

Lookarounds assert a condition at the current position without consuming any characters.
They are zero-width: the matched text is not included in the result.

| Method | Regex produced | Notes |
|---|---|---|
| `followedBy { }` | `(?=...)` | Positive lookahead — position must be followed by the pattern |
| `notFollowedBy { }` | `(?!...)` | Negative lookahead — position must NOT be followed by the pattern |
| `precededBy { }` | `(?<=...)` | Positive lookbehind — position must be preceded by the pattern |
| `notPrecededBy { }` | `(?<!...)` | Negative lookbehind — position must NOT be preceded by the pattern |

**Example — extract the numeric part of a measurement:**

```kotlin
// Match digits only when immediately followed by "ml"
val mlAmount = kexpresso {
    oneOrMore { digit() }
    followedBy { literal("ml") }
}

mlAmount.find("250ml")?.value // "250"  (lookahead consumed nothing: "ml" stays in input)
mlAmount.find("250g")         // null   (not followed by "ml")
```

> **Note:** The JVM regex engine requires lookbehind patterns to be **bounded in length**.
> `precededBy { oneOrMore { digit() } }` (unbounded `+`) will throw a
> `PatternSyntaxException` at compile time. Use a bounded form instead:
> `precededBy { between(1, 10) { digit() } }`.

### Composition & escape hatch

| Method | Regex produced | Notes |
|---|---|---|
| `raw(pattern)` | `pattern` verbatim | **No escaping** — use only for raw regex fragments the DSL cannot yet express |
| `include(pattern)` | `(?:pattern.source)` | Embed a compiled [KexpressoPattern] as a non-capturing group |
| `backreference(n)` | `\n` | Numeric back-reference to the nth capturing group (n ≥ 1) |
| `backreference(name)` | `\k<name>` | Named back-reference; name must start with a letter and contain only letters or digits |

**`raw` example — inject a verbatim date fragment:**

```kotlin
val datePattern = kexpresso { raw("\\d{4}-\\d{2}-\\d{2}") }
datePattern.matches("2026-06-03") // true
```

**`include` example — compose a reusable octet pattern into an IP address:**

```kotlin
val octet = kexpresso { between(1, 3) { digit() } }
val ip = kexpresso {
    include(octet)
    exactly(3) { char('.'); include(octet) }
}
ip.matches("192.168.1.1") // true
```

**`backreference` example — detect repeated words:**

```kotlin
val repeated = kexpresso {
    capture { oneOrMore { wordChar() } }
    whitespace()
    backreference(1)
}
repeated.containsMatchIn("latte latte") // true
repeated.containsMatchIn("latte mocha") // false
```

### Domain helpers

These extension functions on `KexpressoBuilder` compose common real-world patterns from
the primitives above.

#### Text helpers (`Text.kt`)

| Method | Pattern | Matches |
|---|---|---|
| `word()` | `[a-zA-Z0-9]+` | One or more alphanumeric characters (e.g. `Espresso`, `Cappuccino42`) |
| `pseudo()` | `[a-zA-Z0-9_-]+` | Like `word()` but also allows `_` and `-` (e.g. `cold-brew_2024`) |
| `email()` | see source | A broadly valid email address (e.g. `barista@coffee.shop`) |
| `url()` | see source | An HTTP or HTTPS URL (e.g. `https://coffee.shop/menu`) |

> `email()` and `url()` are intentionally permissive. Pair with `startOfText()`/`endOfText()` for strict whole-string validation.

#### Writing helpers (`Writing.kt`)

| Method | What it matches |
|---|---|
| `sentence()` | A capital-letter-led sequence of words ending with `.`, `!`, or `?` |
| `paragraph()` | One or more sentences separated by single spaces |

```kotlin
val paragraphPattern = kexpresso { paragraph() }

paragraphPattern.matches("Latte is smooth. Espresso is bold!") // true
paragraphPattern.matches("latte is lowercase.")                 // false
```

> Note: `sentence()` builds the first word as `capitalLetter()` + `word()`, so the first
> word must be at least two characters long (one uppercase letter followed by at least one
> alphanumeric character).

### Ready-to-use patterns

These helpers in `Domains.kt` let you match common real-world formats in one call.
Pair with `startOfText()`/`endOfText()` for whole-string validation.

| Helper | Matches | Caveats |
|---|---|---|
| `ipv4()` | IPv4 address, e.g. `192.168.1.1` | Decimal only; no CIDR notation |
| `uuid()` | RFC 4122 UUID versions 1–5, e.g. `550e8400-e29b-41d4-a716-446655440000` | Nil UUID and versions 6+ rejected |
| `slug()` | URL/CMS slug, e.g. `cold-brew` | Lowercase only; no underscores |
| `hexColor()` | CSS hex color `#RGB`, `#RGBA`, `#RRGGBB`, `#RRGGBBAA`, e.g. `#1a2b3c` | 5- and 7-digit forms are invalid CSS and do not match |
| `semanticVersion()` | SemVer 2.0.0 string, e.g. `1.0.0-rc.1+build.42` | No leading `v`; partial forms like `1.0` rejected |
| `isoDate()` | ISO-8601 date `YYYY-MM-DD`, e.g. `2024-01-15` | Does NOT validate day-of-month (Feb 30 passes) |
| `isoTime()` | ISO-8601 time `HH:MM[:SS][Z\|±HH:MM]`, e.g. `14:30:00Z` | Leap seconds and fractional seconds not supported |
| `integerNumber()` | Signed/unsigned integer without leading zeros, e.g. `-7`, `42` | No upper bound on digit count |
| `decimalNumber()` | Decimal with optional fractional part, e.g. `3.14`, `-0.5` | Bare `.5` and scientific notation not supported |
| `hashtag()` | Social-media hashtag `#word`, e.g. `#Espresso` | First char after `#` must be a letter, not a digit |
| `mention()` | @mention (Twitter/X), 1–50 chars, e.g. `@barista` | Other platforms may allow longer names |
| `e164Phone()` | E.164 phone number, e.g. `+14155552671` | Compact form only — no separators; no country-code validation |

**Example — validate an IPv4 address:**

```kotlin
val ipValidator = kexpresso {
    startOfText()
    ipv4()
    endOfText()
}

ipValidator.matches("192.168.1.1") // true
ipValidator.matches("256.0.0.1")   // false — octet out of range
```

**Example — extract all hashtags from a post:**

```kotlin
val hashtagPattern = kexpresso { hashtag() }

val post = "Loving my #Espresso and #ColdBrew today! #Coffee"
val tags = hashtagPattern.findAll(post).map { it.value }.toList()
// ["#Espresso", "#ColdBrew", "#Coffee"]
```

---

## Working with results

`kexpresso { }` returns a `KexpressoPattern` — an immutable, thread-safe wrapper
around a compiled `Regex`.

### Matching

```kotlin
val p = kexpresso { oneOrMore { letter() } }

p.matches("Espresso")                       // true  — entire string must match
p.containsMatchIn("Order: Espresso please") // true  — match anywhere in the string
```

### Searching

```kotlin
val wordPattern = kexpresso { oneOrMore { letter() } }

// First match only
val first = wordPattern.find("Espresso Latte")
first?.value // "Espresso"

// Skip ahead with startIndex
val second = wordPattern.find("Espresso Latte", startIndex = 9)
second?.value // "Latte"

// All non-overlapping matches (returns a lazy Sequence)
val drinks = wordPattern.findAll("Espresso Latte Cappuccino").map { it.value }.toList()
// ["Espresso", "Latte", "Cappuccino"]
```

### String operations

`KexpressoPattern` exposes convenience methods that delegate to the underlying `Regex`:

**`replaceFirst` — replace the first match:**

```kotlin
val drink = kexpresso { oneOrMore { letter() } }
drink.replaceFirst("espresso latte", "ESPRESSO") // "ESPRESSO latte"
```

**`replaceAll` with a fixed string — replace every match:**

```kotlin
val drink = kexpresso { oneOrMore { letter() } }
drink.replaceAll("espresso latte", "brew") // "brew brew"
```

**`replaceAll` with a transform — compute the replacement per match:**

```kotlin
val drink = kexpresso { oneOrMore { letter() } }
drink.replaceAll("espresso latte") { it.value.uppercase() } // "ESPRESSO LATTE"
```

**`split` — split around matches:**

```kotlin
val sep = kexpresso { literal(", ") }
sep.split("Espresso, Latte, Cappuccino") // ["Espresso", "Latte", "Cappuccino"]
sep.split("Espresso, Latte, Cappuccino", limit = 2) // ["Espresso", "Latte, Cappuccino"]
```

**`matchEntire` — full-string match with group access:**

```kotlin
val drinkOrder = kexpresso {
    capture("drink") { oneOrMore { letter() } }
    whitespace()
    capture("size") { oneOrMore { letter() } }
}
val result = drinkOrder.matchEntire("Latte Large")
result?.groups?.get("drink")?.value // "Latte"
result?.groups?.get("size")?.value  // "Large"
```

### Inspecting the pattern

```kotlin
val p = kexpresso { digit(); letter() }

p.source  // "\\d[a-zA-Z]"   — raw regex string
p.options // emptySet()       — Set<RegexOption>
```

### Explain a pattern (`describe()`)

Every pattern can explain itself in plain English. `describe()` walks the internal AST
(the same representation that renders the regex) and returns a deterministic, comma-joined
phrase — handy for code review, logging, or learning what a pattern does:

```kotlin
val p = kexpresso { startOfText(); oneOrMore { digit() }; endOfText() }

p.source     // "\\A(?:\\d)+\\z"
p.describe() // "start of text, one or more of (a digit), end of text"
```

Domain helpers (e.g. `email()`) are emitted as raw fragments, so they describe as
``raw regex `…` `` rather than a fully decomposed phrase.

### Interoperability

```kotlin
val p = kexpresso { literal("Cappuccino") }

val kotlinRegex:   Regex                  = p.toRegex()
val javaPattern:   java.util.regex.Pattern = p.toPattern()
```

### RegexOption

Pass any number of `RegexOption` values to the `kexpresso { }` call or to
`Kexpresso.pattern { }`:

```kotlin
val caseInsensitive = kexpresso(RegexOption.IGNORE_CASE) {
    literal("espresso")
}
caseInsensitive.matches("ESPRESSO") // true
caseInsensitive.matches("Espresso") // true

val multiline = kexpresso(RegexOption.MULTILINE) {
    startOfLine()
    literal("Espresso")
    endOfLine()
}
multiline.containsMatchIn("Espresso\nCappuccino") // true
```

### Object-oriented entry point

If you prefer an object-oriented style, use `Kexpresso.pattern { }` — it is identical
to the top-level `kexpresso { }` function:

```kotlin
val p = Kexpresso.pattern(RegexOption.IGNORE_CASE) { literal("Ristretto") }
p.matches("ristretto") // true
```

---

## Safety: ReDoS analysis

Certain regex patterns can cause catastrophic backtracking — an attacker who controls
input can make the regex engine take exponential time. The classic shape is **nested
unbounded quantifiers** such as `(?:a+)+`.

Kexpresso provides a best-effort static analyzer to catch this shape at development time:

```kotlin
// DSL produces (?:(?:[a-zA-Z])+)+ — nested unbounded quantifiers
val risky = kexpresso { oneOrMore { oneOrMore { letter() } } }

val report = risky.analyze()
if (report.isPotentiallyVulnerable) {
    println("Findings:")
    report.findings.forEach { println("  [${it.severity}] ${it.message}") }
}
// Findings:
//   [WARNING] Nested unbounded quantifier at index 0: (?:(?:[a-zA-Z])+)+ …

// Convenience shorthand
if (risky.isPotentiallyVulnerable) { /* warn or reject */ }
```

**This is a best-effort heuristic, not a guarantee.** It detects the canonical "evil
regex" shape — a group with an outer unbounded quantifier (`*`, `+`, or `{n,}`) whose
body also contains an inner unbounded quantifier. It does NOT detect all ReDoS patterns
(e.g. alternation-based catastrophic backtracking), and a clean result does not prove
the pattern is safe. Use it as an early-warning signal alongside proper input constraints
and performance testing.

---

## Building and contributing

```bash
# Compile, run all tests, Detekt static analysis, and JaCoCo coverage report
./gradlew build

# Run tests only
./gradlew test

# Run Detekt only
./gradlew detekt
```

See [CONTRIBUTING.md](CONTRIBUTING.md) for the full contributor guide — including how to
add a new DSL primitive.

---

## License

[MIT](LICENSE) — Copyright (c) 2026 Thomas Couderc.
