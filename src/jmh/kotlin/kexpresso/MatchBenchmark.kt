package kexpresso

import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.Warmup
import org.openjdk.jmh.annotations.Measurement
import java.util.concurrent.TimeUnit

/**
 * Measures match-time overhead of KexpressoPattern versus a raw pre-compiled Regex.
 *
 * Both patterns are built once in @Setup (Scope.Benchmark) so the comparison is
 * purely about the cost of calling .matches() — construction is excluded.
 *
 * Pattern: anchored ISO-8601 date  \A\d{4}-\d{2}-\d{2}\z
 * Input:   a valid date string that fully matches.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
open class MatchBenchmark {

    private lateinit var kexpressoPattern: KexpressoPattern
    private lateinit var rawRegex: Regex
    private val input: String = "2026-06-03"

    @Setup
    fun setup() {
        // Build the KexpressoPattern once
        kexpressoPattern = kexpresso {
            startOfText()
            exactly(4) { digit() }
            char('-')
            exactly(2) { digit() }
            char('-')
            exactly(2) { digit() }
            endOfText()
        }

        // Build an equivalent raw Regex from the same source string
        rawRegex = Regex(kexpressoPattern.source)
    }

    @Benchmark
    fun kexpressoMatch(): Boolean = kexpressoPattern.matches(input)

    @Benchmark
    fun rawRegexMatch(): Boolean = rawRegex.matches(input)
}
