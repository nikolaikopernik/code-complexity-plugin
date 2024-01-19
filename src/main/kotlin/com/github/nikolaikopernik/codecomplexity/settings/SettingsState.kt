package com.github.nikolaikopernik.codecomplexity.settings

import com.github.nikolaikopernik.codecomplexity.bundle.SettingsBundle
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

const val DEFAULT_LIMIT_SIMPLE: Int = 8
const val DEFAULT_LIMIT_VERY_COMPLEX: Int = 12

@State(name = "com.github.nikolaikopernik.codecomplexity.settings.SettingsState",
       storages = [Storage("code-complexity-settings.xml")])
class SettingsState : PersistentStateComponent<SettingsState> {
    var useDefaults: Boolean = true
    var usePlainComplexity: Boolean = false
    var showIcon: Boolean = true
    var limitSimpleLessThan: Int = DEFAULT_LIMIT_SIMPLE
    var limitVeryComplexMoreThan: Int = DEFAULT_LIMIT_VERY_COMPLEX
    var simpleComplexText: String = SettingsBundle.message("simpleComplexDefaultText")
    var mildlyComplexText: String = SettingsBundle.message("mildlyComplexDefaultText")
    var veryComplexText: String = SettingsBundle.message("veryComplexDefaultText")
    var templateText: String = SettingsBundle.message("customTemplateDefaultText", "{0}", "{1}")
    override fun getState(): SettingsState {
        return this
    }

    override fun loadState(state: SettingsState) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        val INSTANCE: SettingsState
            get() = ApplicationManager.getApplication().getService(SettingsState::class.java)
    }
}
