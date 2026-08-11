package com.github.nikolaikopernik.codecomplexity

import com.github.nikolaikopernik.codecomplexity.core.HighCodeComplexityInspection
import com.github.nikolaikopernik.codecomplexity.settings.SettingsState
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * End-to-end characterization of [HighCodeComplexityInspection] through the highlighting fixture:
 * also the first coverage of visitFileFast, the languageInfoProvider EP lookup and getNameElementFor.
 */
class HighCodeComplexityInspectionTest : BasePlatformTestCase() {
    private lateinit var saved: SettingsState

    override fun setUp() {
        super.setUp()
        saved = SettingsState().apply { loadState(SettingsState.INSTANCE) }
        with(SettingsState.INSTANCE) {
            limitSimpleLessThan = 8
            limitVeryComplexMoreThan = 12
            usePlainComplexity = true
        }
        myFixture.enableInspections(HighCodeComplexityInspection())
    }

    override fun tearDown() {
        try {
            SettingsState.INSTANCE.loadState(saved)
        } finally {
            super.tearDown()
        }
    }

    fun testHardMethodIsFlaggedOnItsNameAndSimpleMethodIsNot() {
        // gnarly(): nested ifs score 1+2+3+4+5 = 15 > 12 -> HARD; simple(): 1 -> no warning
        myFixture.configureByText("Heavy.java", """
            class Heavy {
                void simple(boolean a) {
                    if (a) { }
                }

                void <warning descr="fun 'gnarly()' has complexity: 15">gnarly</warning>(boolean a) {
                    if (a) {
                        if (a) {
                            if (a) {
                                if (a) {
                                    if (a) { }
                                }
                            }
                        }
                    }
                }
            }
        """.trimIndent())
        myFixture.checkHighlighting()
    }
}
