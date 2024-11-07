package net.rk4z.s1.swiftbase.core

import java.util.concurrent.*

interface S0Executor {
    fun <T> submit(task: Callable<T>): Future<T>
    fun <T> submitAsync(task: Callable<T>): Future<T>

    fun execute(task: Runnable)
    fun executeAsync(task: Runnable)

    fun executeTimer(task: Runnable, delay: Long, period: Long)
    fun executeAsyncTimer(task: Runnable, delay: Long, period: Long)

    fun schedule(task: Runnable, delay: Long)
    fun scheduleAtFixedRate(task: Runnable, delay: Long, period: Long)

    fun <T> submitWithTimeout(task: Callable<T>, timeout: Long, timeUnit: TimeUnit): T?

    fun <T> submitWithDelay(task: Callable<T>, delay: Long): Future<T>

    fun <T> invokeAll(tasks: Collection<Callable<T>>): List<Future<T>>

    fun awaitTermination(timeout: Long, timeUnit: TimeUnit): Boolean

    fun cancelAll(mayInterruptIfRunning: Boolean)

    fun isShutdown(): Boolean

    fun shutdown()

    fun checkShutdown()
}