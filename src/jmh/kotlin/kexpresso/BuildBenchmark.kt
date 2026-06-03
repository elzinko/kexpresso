package kexpresso

import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.Warmup
import org.openjdk.jmh.annotations.Measurement
import java.util.concurrent.TimeUnit

/**
 * Measures full construction cost: DSL building + Regex compilation.
 *
 * buildWithKexpresso   — runs the kexpresso { } block end-to-end (StringBuilder ops + Regex())
 * compileRawRegex      — runs Regex("<same source>") directly
 *
 * The raw-source string is the same pattern produced by the DSL block so that
 * the Regex compilation step is identical; the only extra cost in kexpresso is
 * the DSL builder traversal (StringBuilder appends).
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
open class BuildBenchmark {

    // The raw regex string that the DSL produces for the ISO-date pattern.
    // Captured here so compileRawRegex() compiles exactly the same source.
    private val rawSource: String = run {
        kexpresso {
            startOfText()
            exactly(4) { digit() }
            char('-')
            exactly(2) { digit() }
            char('-')
            exactly(2) { digit() }
            endOfText()
        }.source
    }

    @Benchmark
    fun buildWithKexpresso(): KexpressoPattern = kexpresso {
        startOfText()
        exactly(4) { digit() }
        char('-')
        exactly(2) { digit() }
        char('-')
        exactly(2) { digit() }
        endOfText()
    }

    @Benchmark
    fun compileRawRegex(): Regex = Regex(rawSource)
}
