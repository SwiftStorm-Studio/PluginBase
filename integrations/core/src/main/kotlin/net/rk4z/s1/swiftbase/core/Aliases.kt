package net.rk4z.s1.swiftbase.core

import net.rk4z.s1.swiftbase.core.Core.Companion.logger as Logger
import net.rk4z.s1.swiftbase.core.LanguageManager.Companion.instance as LMB
import net.rk4z.s1.swiftbase.core.Core.Companion.instance as CB

val CB = CB
    get() {
        if (!Core.isInitialized()) {
            throw IllegalStateException("Core is not initialized yet")
        }
        return field
    }

val LMB = LMB
    get() {
        if (!LanguageManager.isInitialized()) {
            throw IllegalStateException("LanguageManager is not initialized yet")
        }
        return field
    }

val Logger = Logger