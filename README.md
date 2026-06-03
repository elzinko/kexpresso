# Kexpresso ☕

**A fluent Kotlin DSL that makes regular expressions readable.**

[![CI](https://github.com/elzinko/kexpresso/actions/workflows/ci.yml/badge.svg)](https://github.com/elzinko/kexpresso/actions/workflows/ci.yml)
[![JitPack](https://jitpack.io/v/elzinko/kexpresso.svg)](https://jitpack.io/#elzinko/kexpresso)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.8.20-7F52FF?logo=kotlin)](https://kotlinlang.org)

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
- **Zero runtime overhead** — the DSL compiles to a plain `Regex` at construction time.

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

### Inspecting the pattern

```kotlin
val p = kexpresso { digit(); letter() }

p.source  // "\\d[a-zA-Z]"   — raw regex string
p.options // emptySet()       — Set<RegexOption>
```

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
