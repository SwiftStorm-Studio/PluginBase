package net.rk4z.s1.swiftbase.core.dummy

import net.rk4z.s1.swiftbase.core.S0Executor

class DummyExecutor internal constructor() : S0Executor {
    private fun throwError() {
        throw UnsupportedOperationException("This method is not supported by DummyExecutor")
    }

    override fun execute(task: Runnable) {
        throwError()
    }

    override fun <T> execute(task: () -> T): T? {
        throwError()
        return null
    }

    override fun executeAsync(task: Runnable) {
        throwError()
    }

    override fun <T> executeAsync(task: () -> T): T? {
        throwError()
        return null
    }

    override fun executeLater(task: Runnable, delay: Long) {
        throwError()
    }

    override fun <T> executeLater(task: () -> T, delay: Long): T? {
        throwError()
        return null
    }

    override fun executeAsyncLater(task: Runnable, delay: Long) {
        throwError()
    }

    override fun <T> executeAsyncLater(task: () -> T, delay: Long): T? {
        throwError()
        return null
    }

    override fun executeTimer(task: Runnable, delay: Long, period: Long) {
        throwError()
    }

    override fun <T> executeTimer(task: () -> T, delay: Long, period: Long): T? {
        throwError()
        return null
    }

    override fun executeAsyncTimer(task: Runnable, delay: Long, period: Long) {
        throwError()
    }

    override fun <T> executeAsyncTimer(task: () -> T, delay: Long, period: Long): T? {
        throwError()
        return null
    }

    override fun shutdownCheck() {
        throwError()
    }

    override fun shutdown() {
        throwError()
    }
}