package net.rk4z.s1.swiftbase.core

interface S0Executor {
    fun execute(task: Runnable)

    fun <T> execute(task: () -> T): T?

    fun executeAsync(task: Runnable)

    fun <T> executeAsync(task: () -> T): T?

    fun executeLater(task: Runnable, delay: Long)

    fun <T> executeLater(task: () -> T, delay: Long): T?

    fun executeAsyncLater(task: Runnable, delay: Long)

    fun <T> executeAsyncLater(task: () -> T, delay: Long): T?

    fun executeTimer(task: Runnable, delay: Long, period: Long)

    fun <T> executeTimer(task: () -> T, delay: Long, period: Long): T?

    fun executeAsyncTimer(task: Runnable, delay: Long, period: Long)

    fun <T> executeAsyncTimer(task: () -> T, delay: Long, period: Long): T?

    fun shutdownCheck()

    fun shutdown()
}