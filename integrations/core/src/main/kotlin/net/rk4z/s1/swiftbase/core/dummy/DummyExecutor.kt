package net.rk4z.s1.swiftbase.core.dummy

import net.rk4z.s1.swiftbase.core.S0Executor
import java.util.concurrent.Callable
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

class DummyExecutor internal constructor() : S0Executor {
    override fun <T> submit(task: Callable<T>): Future<T>? {
        throwError()
        return null
    }

    override fun <T> submitAsync(task: Callable<T>): Future<T>? {
        throwError()
        return null
    }

    override fun execute(task: Runnable) {
        throwError()
    }

    override fun executeAsync(task: Runnable) {
        throwError()
    }

    override fun executeTimer(task: Runnable, delay: Long, period: Long) {
        throwError()
    }

    override fun executeAsyncTimer(task: Runnable, delay: Long, period: Long) {
        throwError()
    }

    override fun schedule(task: Runnable, delay: Long) {
        throwError()
    }

    override fun scheduleAtFixedRate(task: Runnable, delay: Long, period: Long) {
        throwError()
    }

    override fun <T> submitWithTimeout(
        task: Callable<T>,
        timeout: Long,
        timeUnit: TimeUnit
    ): T? {
        throwError()
        return null
    }

    override fun <T> submitWithDelay(task: Callable<T>, delay: Long): Future<T>? {
        throwError()
        return null
    }

    override fun <T> invokeAll(tasks: Collection<Callable<T>>): List<Future<T>>? {
        throwError()
        return null
    }

    override fun awaitTermination(timeout: Long, timeUnit: TimeUnit): Boolean? {
        throwError()
        return null
    }

    override fun cancelAll(mayInterruptIfRunning: Boolean) {
        throwError()
    }

    override fun isShutdown(): Boolean? {
        throwError()
        return null
    }

    override fun shutdown() {
        throwError()
    }

    override fun checkShutdown() {
        throwError()
    }

    private fun throwError() {
        throw UnsupportedOperationException("This method is not supported by DummyExecutor")
    }
}