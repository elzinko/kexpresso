package kexpresso

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Verifies the [ExperimentalKexpressoApi] opt-in marker is correctly wired and usable
 * across all KMP targets.
 *
 * The annotation is the freeze-prep foundation for the 29 best-effort surface symbols
 * that will be marked experimental ahead of 1.0. This test is intentionally minimal —
 * it just proves the marker compiles, can be opted into, and can be propagated; semantic
 * correctness of individual annotated symbols stays with their own tests.
 */
@OptIn(ExperimentalKexpressoApi::class)
class ExperimentalKexpressoApiTest {

    @Test
    fun annotationIsDefinedAndUsable() {
        val marker = ExperimentalKexpressoApi::class
        assertTrue(marker.simpleName == "ExperimentalKexpressoApi")
    }

    @Test
    fun callSiteCanOptIn() {
        // The class-level @OptIn lets us call experimentalSample() directly here.
        assertTrue(experimentalSample() == 42)
    }

    @Test
    fun markerCanBePropagated() {
        // propagatingCaller is itself annotated @ExperimentalKexpressoApi, so it can
        // call other experimental APIs without an explicit @OptIn at the body.
        assertTrue(propagatingCaller() == 42)
    }

    @ExperimentalKexpressoApi
    private fun experimentalSample(): Int = 42

    @ExperimentalKexpressoApi
    private fun propagatingCaller(): Int = experimentalSample()
}
