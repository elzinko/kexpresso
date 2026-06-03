# Kexpresso JMH Benchmarks

Reproducible micro-benchmarks that quantify two performance claims:

1. **Match-time parity** — at call time, `KexpressoPattern.matches()` has ~0% overhead over a raw pre-compiled `Regex`, because a `KexpressoPattern` *is* a compiled `Regex` — the wrapper delegates directly.
2. **Construction cost is small and paid once** — building the DSL + compiling the regex takes ~0.3 µs; that cost disappears entirely when the pattern is reused across inputs (the normal usage pattern).

---

## What each benchmark measures

### `MatchBenchmark`

Both the `KexpressoPattern` and the equivalent `Regex` are pre-built in a `@Setup` method (scope: `Benchmark`). The `@Benchmark` methods do nothing but call `.matches(input)` on the same 10-character ISO-date input (`"2026-06-03"`), so construction cost is **excluded**.

| Method | What it calls |
|---|---|
| `kexpressoMatch` | `KexpressoPattern.matches(input)` |
| `rawRegexMatch`  | `Regex.matches(input)` on the same compiled engine |

### `BuildBenchmark`

Each `@Benchmark` invocation performs full construction from scratch. The pattern is an anchored ISO-8601 date (`\A\d{4}-\d{2}-\d{2}\z`).

| Method | What it does |
|---|---|
| `buildWithKexpresso` | Runs the full `kexpresso { }` block: StringBuilder appends + `Regex()` compile |
| `compileRawRegex` | Calls `Regex("<same source string>")` directly |

The delta between the two is the marginal cost of the DSL builder layer (StringBuilder operations) on top of regex compilation.

---

## How to run

```bash
./gradlew jmh
```

Results are written to `build/results/jmh/results.txt`.

JMH configuration (set in `build.gradle.kts`):

```
warmupIterations = 2 × 1 s
measurementIterations = 3 × 1 s
forks = 1
mode = AverageTime
```

For a more rigorous run increase forks to 3 and iterations to 5.

---

## Environment

| Property | Value |
|---|---|
| Date | 2026-06-03 |
| JDK | OpenJDK 17.0.17+10-LTS (Microsoft Build) |
| JVM | OpenJDK 64-Bit Server VM 17.0.17+10-LTS |
| OS | macOS Darwin 24.6.0 (Apple Silicon host) |
| JMH version | 1.36 |
| Blackhole mode | compiler (auto-detected) |
| Gradle | 8.7 |

---

## Results

```
Benchmark                          Mode  Cnt   Score   Error  Units
BuildBenchmark.buildWithKexpresso  avgt    3   0.290 ± 0.012  us/op
BuildBenchmark.compileRawRegex     avgt    3   0.185 ± 0.025  us/op
MatchBenchmark.kexpressoMatch      avgt    3  43.279 ± 1.365  ns/op
MatchBenchmark.rawRegexMatch       avgt    3  43.690 ± 4.118  ns/op
```

---

## Interpretation

### Match time: parity confirmed

`kexpressoMatch` (43.279 ns) and `rawRegexMatch` (43.690 ns) are statistically indistinguishable — the difference of ~0.4 ns is well within the measurement noise (error bands of ±1.4 ns and ±4.1 ns respectively overlap completely). This confirms that wrapping a `Regex` inside `KexpressoPattern` adds **zero overhead** at match time: the abstraction layer is purely a compile-time convenience.

### Construction cost: 0.29 µs, amortized immediately

Building the full DSL block (`buildWithKexpresso`: 0.290 µs) costs ~0.1 µs more than compiling the equivalent raw string (`compileRawRegex`: 0.185 µs). The overhead is the StringBuilder appends in `KexpressoBuilder`. At 0.105 µs of extra cost, that overhead is amortized after a single `.matches()` call (~43 ns) in fewer than 3 matches — or, more practically, it is negligible in any real application where patterns are compiled once and reused.

**Bottom line:** Use `kexpresso { }` freely. Compile the pattern once, store it, and call `.matches()` as many times as needed. The readability you gain costs nothing at runtime.
