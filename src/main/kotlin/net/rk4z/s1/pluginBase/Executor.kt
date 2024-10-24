package net.rk4z.s1.pluginBase

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean

class Executor(private val plugin: JavaPlugin) {
    private val isShutdown = AtomicBoolean(false)
    private val runningTasks = mutableListOf<Future<*>>()

    /**
     * Submits a Callable task for asynchronous execution and returns a Future representing the result.
     *
     * @param task The Callable task to be executed.
     * @return A Future representing the pending result of the task.
     * @throws RejectedExecutionException If the Executor has been shut down.
     */
    fun <T> submit(task: Callable<T>): Future<T> {
        checkShutdown()
        val future = Bukkit.getScheduler().callSyncMethod(plugin, task)
        runningTasks.add(future)
        return future
    }

    /**
     * Submits a Runnable task for asynchronous execution.
     *
     * @param task The Runnable task to be executed.
     * @throws RejectedExecutionException If the Executor has been shut down.
     */
    fun execute(task: Runnable) {
        checkShutdown()
        Bukkit.getScheduler().runTaskAsynchronously(plugin, task)
        runningTasks.add(CompletableFuture.runAsync(task))
    }

    /**
     * Schedules a Runnable task for asynchronous execution after a specified delay.
     *
     * @param task The Runnable task to be executed.
     * @param delay The delay (in ticks) before the task is executed.
     * @throws RejectedExecutionException If the Executor has been shut down.
     */
    fun schedule(task: Runnable, delay: Long) {
        checkShutdown()
        val future = Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, delay)
        runningTasks.add(CompletableFuture.runAsync(task))
    }

    /**
     * Schedules a Runnable task for repeated asynchronous execution at a fixed rate.
     *
     * @param task The Runnable task to be executed.
     * @param delay The initial delay (in ticks) before the task is first executed.
     * @param period The period (in ticks) between consecutive executions of the task.
     * @throws RejectedExecutionException If the Executor has been shut down.
     */
    fun scheduleAtFixedRate(task: Runnable, delay: Long, period: Long) {
        checkShutdown()
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delay, period)
    }

    /**
     * Submits a Callable task for asynchronous execution and waits for its result,
     * with the specified timeout. If the task does not complete within the timeout,
     * it will be canceled and a TimeoutException will be thrown.
     *
     * @param task The Callable task to be executed.
     * @param timeout The maximum time to wait for the task to complete.
     * @param timeUnit The time unit of the timeout argument.
     * @return The result of the Callable task.
     * @throws TimeoutException If the task does not complete within the specified timeout.
     * @throws InterruptedException If the current thread was interrupted while waiting.
     * @throws ExecutionException If the task threw an exception.
     * @throws RejectedExecutionException If the Executor has been shut down.
     */
    @Throws(TimeoutException::class, InterruptedException::class, ExecutionException::class)
    fun <T> submitWithTimeout(task: Callable<T>, timeout: Long, timeUnit: TimeUnit): T? {
        checkShutdown()
        val future = Bukkit.getScheduler().callSyncMethod(plugin, task)
        runningTasks.add(future)
        return try {
            future.get(timeout, timeUnit)
        } catch (e: TimeoutException) {
            future.cancel(true)
            throw e
        }
    }

    /**
     * Submits a Callable task for asynchronous execution after a specified delay.
     *
     * @param task The Callable task to be executed.
     * @param delay The delay (in ticks) before the task is executed.
     * @return A Future representing the pending result of the task.
     * @throws RejectedExecutionException If the Executor has been shut down.
     */
    fun <T> submitWithDelay(task: Callable<T>, delay: Long): Future<T> {
        checkShutdown()
        val future = Bukkit.getScheduler().callSyncMethod(plugin, task)
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, Runnable { runningTasks.add(future) }, delay)
        return future
    }

    /**
     * Submits a collection of Callable tasks for asynchronous execution.
     * This method will block until all tasks have completed.
     *
     * @param tasks The collection of Callable tasks to execute.
     * @return A list of Future objects representing the results of the tasks.
     * @throws InterruptedException If the current thread was interrupted while waiting.
     * @throws RejectedExecutionException If the Executor has been shut down.
     */
    @Throws(InterruptedException::class)
    fun <T> invokeAll(tasks: Collection<Callable<T>>): List<Future<T>> {
        checkShutdown()
        val futures = tasks.map { task -> submit(task) }
        return futures
    }

    /**
     * Waits for all running tasks to complete or until the timeout occurs.
     *
     * @param timeout The maximum time to wait.
     * @param timeUnit The time unit of the timeout argument.
     * @return `true` if all tasks completed before the timeout, otherwise `false`.
     * @throws InterruptedException If interrupted while waiting.
     */
    @Throws(InterruptedException::class)
    fun awaitTermination(timeout: Long, timeUnit: TimeUnit): Boolean {
        val endTime = System.nanoTime() + timeUnit.toNanos(timeout)
        while (System.nanoTime() < endTime) {
            if (runningTasks.all { it.isDone || it.isCancelled }) {
                return true
            }
            Thread.sleep(100) // Polling delay
        }
        return false
    }

    /**
     * Cancels all currently running or scheduled tasks.
     *
     * @param mayInterruptIfRunning If `true`, tasks that are currently running are interrupted.
     */
    fun cancelAll(mayInterruptIfRunning: Boolean) {
        runningTasks.forEach { it.cancel(mayInterruptIfRunning) }
    }

    /**
     * Checks if the Executor has been shut down.
     *
     * @return `true` if the Executor has been shut down, otherwise `false`.
     */
    fun isShutdown(): Boolean {
        return isShutdown.get()
    }

    /**
     * Initiates an orderly shutdown of the Executor.
     * This will cancel all pending tasks and no new tasks will be accepted.
     */
    fun shutdown() {
        if (isShutdown.compareAndSet(false, true)) {
            runningTasks.forEach { it.cancel(true) }
            runningTasks.clear()
        }
    }

    /**
     * Checks if the Executor has been shut down.
     * If it has, a RejectedExecutionException will be thrown.
     *
     * @throws RejectedExecutionException If the Executor has been shut down.
     */
    private fun checkShutdown() {
        if (isShutdown.get()) {
            throw RejectedExecutionException("Executor has been shut down")
        }
    }
}
