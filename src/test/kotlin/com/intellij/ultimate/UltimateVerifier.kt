package com.intellij.ultimate

/**
 * A mock implementation of the `UltimateVerifier` class from `com.intellij.ultimate.UltimateVerifier`.
 * Used to enable index cache operations to execute in test mode.
 */
@Suppress("unused")
class UltimateVerifier {

    companion object {
        @JvmStatic
        fun getInstance(): UltimateVerifier = UltimateVerifier()
    }
}
