package com.github.nikolaikopernik.codecomplexity

import com.github.nikolaikopernik.codecomplexity.core.ComplexitySink
import com.github.nikolaikopernik.codecomplexity.core.PointType
import com.github.nikolaikopernik.codecomplexity.settings.ComplexityLevel
import com.github.nikolaikopernik.codecomplexity.settings.SettingsState
import com.github.nikolaikopernik.codecomplexity.settings.getConfiguredLevel
import com.github.nikolaikopernik.codecomplexity.settings.getConfiguredText
import com.github.nikolaikopernik.codecomplexity.settings.getValueToShow
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Characterization tests for the settings math in ComplexitySettings.kt: they pin current
 * behaviour, quirks included, ahead of refactors. Deliberate behaviour changes update them.
 */
class ComplexitySettingsTest : BasePlatformTestCase() {
    private lateinit var saved: SettingsState

    override fun setUp() {
        super.setUp()
        saved = SettingsState().apply { loadState(SettingsState.INSTANCE) }
        with(SettingsState.INSTANCE) {
            limitSimpleLessThan = 8
            limitVeryComplexMoreThan = 12
            usePlainComplexity = false
        }
    }

    override fun tearDown() {
        try {
            SettingsState.INSTANCE.loadState(saved)
        } finally {
            super.tearDown()
        }
    }

    fun testLevelBoundaries() {
        assertEquals(ComplexityLevel.SIMPLE, methodSink(7).getConfiguredLevel())
        // == limitSimpleLessThan is already MIDDLE
        assertEquals(ComplexityLevel.MIDDLE, methodSink(8).getConfiguredLevel())
        // == limitVeryComplexMoreThan is still MIDDLE, despite the "MoreThan" name
        assertEquals(ComplexityLevel.MIDDLE, methodSink(12).getConfiguredLevel())
        assertEquals(ComplexityLevel.HARD, methodSink(13).getConfiguredLevel())
    }

    fun testLevelIgnoresClassScalingButTextAppliesIt() {
        with(SettingsState.INSTANCE) {
            usePlainComplexity = true
            hintTextSimpleComplex = "simple {score}"
        }
        val sink = classSink(20)
        // getConfiguredLevel never applies the x4 class scaling (20 > 12 -> HARD),
        // while the hint text for the same sink does (20 < 8*4 -> simple template).
        assertEquals(ComplexityLevel.HARD, sink.getConfiguredLevel())
        assertEquals("simple 20", sink.getConfiguredText())
    }

    fun testValueToShowPlain() {
        SettingsState.INSTANCE.usePlainComplexity = true
        assertEquals("20", methodSink(20).getValueToShow())
    }

    fun testValueToShowAsPercentageOfSimpleLimit() {
        // integer division: 7*100/8
        assertEquals("87%", methodSink(7).getValueToShow())
        // class sinks measure against 4x the simple limit: 16*100/32
        assertEquals("50%", classSink(16).getValueToShow())
    }

    fun testValueToShowThrowsOnZeroSimpleLimit() {
        SettingsState.INSTANCE.limitSimpleLessThan = 0
        try {
            methodSink(5).getValueToShow()
            fail("expected ArithmeticException: percentage mode divides by limitSimpleLessThan")
        } catch (_: ArithmeticException) {
        }
    }

    fun testHintTemplateScoreSubstitution() {
        with(SettingsState.INSTANCE) {
            usePlainComplexity = true
            hintTextSimpleComplex = "ok: {score}"
            hintTextMildlyComplex = "meh: {score} ({score})"
            hintTextVeryComplex = ""
        }
        assertEquals("ok: 3", methodSink(3).getConfiguredText())
        assertEquals("meh: 9 (9)", methodSink(9).getConfiguredText())
        assertEquals("", methodSink(13).getConfiguredText())
    }

    private fun methodSink(total: Int) = ComplexitySink().apply { increaseComplexity(total, PointType.IF) }

    private fun classSink(total: Int) = ComplexitySink().apply { increaseComplexity(total, PointType.METHOD) }
}
