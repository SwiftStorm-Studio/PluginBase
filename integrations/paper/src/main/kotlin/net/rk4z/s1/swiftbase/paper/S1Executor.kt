package net.rk4z.s1.swiftbase.paper

import net.rk4z.s1.swiftbase.core.S0Executor
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.ConcurrentLinkedQueue

class S1Executor internal constructor(private val plugin: JavaPlugin) : S0Executor {
    private val isShutdown = AtomicBoolean(false)
    private val runningTasks = ConcurrentLinkedQueue<Future<*>>()

    override fun <T> submit(task: Callable<T>): Future<T> {
        checkShutdown()
        val future = Bukkit.getScheduler().callSyncMethod(plugin, task) // 同期タスク
        runningTasks.add(future)
        return future
    }

    override fun <T> submitAsync(task: Callable<T>): Future<T> {
        checkShutdown()
        val future = CompletableFuture.supplyAsync({
            task.call()
        }, BukkitSchedulerExecutor(plugin, Bukkit.getScheduler(), async = true))
        runningTasks.add(future)
        future.whenComplete { _, _ -> runningTasks.remove(future) }
        return future
    }

    override fun execute(task: Runnable) {
        checkShutdown()
        Bukkit.getScheduler().runTask(plugin, task) // 同期タスク
    }

    override fun executeAsync(task: Runnable) {
        checkShutdown()
        Bukkit.getScheduler().runTaskAsynchronously(plugin, task) // 非同期タスク
    }

    override fun executeTimer(task: Runnable, delay: Long, period: Long) {
        checkShutdown()
        val bukkitTask = Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period)
        runningTasks.add(BukkitTaskFuture(bukkitTask))
    }

    override fun executeAsyncTimer(task: Runnable, delay: Long, period: Long) {
        checkShutdown()
        val bukkitTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delay, period)
        runningTasks.add(BukkitTaskFuture(bukkitTask))
    }

    override fun schedule(task: Runnable, delay: Long) {
        checkShutdown()
        Bukkit.getScheduler().runTaskLater(plugin, task, delay) // 遅延タスク
    }

    override fun scheduleAtFixedRate(task: Runnable, delay: Long, period: Long) {
        checkShutdown()
        executeAsyncTimer(task, delay, period) // 定期実行
    }

    override fun <T> submitWithTimeout(task: Callable<T>, timeout: Long, timeUnit: TimeUnit): T? {
        checkShutdown()
        val future = Bukkit.getScheduler().callSyncMethod(plugin, task)
        runningTasks.add(future)
        return try {
            future.get(timeout, timeUnit)
        } catch (e: TimeoutException) {
            future.cancel(true)
            throw e
        } finally {
            runningTasks.remove(future)
        }
    }

    override fun <T> submitWithDelay(task: Callable<T>, delay: Long): Future<T> {
        checkShutdown()
        val future = CompletableFuture<T>()
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, Runnable {
            try {
                val result = task.call()
                future.complete(result)
            } catch (e: Exception) {
                future.completeExceptionally(e)
            } finally {
                runningTasks.remove(future)
            }
        }, delay)
        runningTasks.add(future)
        return future
    }

    override fun <T> invokeAll(tasks: Collection<Callable<T>>): List<Future<T>> {
        checkShutdown()
        return tasks.map { submit(it) }
    }

    override fun awaitTermination(timeout: Long, timeUnit: TimeUnit): Boolean {
        val endTime = System.nanoTime() + timeUnit.toNanos(timeout)
        while (System.nanoTime() < endTime) {
            if (runningTasks.all { it.isDone || it.isCancelled }) {
                return true
            }
            Thread.sleep(100)
        }
        return false
    }

    override fun cancelAll(mayInterruptIfRunning: Boolean) {
        runningTasks.forEach { it.cancel(mayInterruptIfRunning) }
        runningTasks.clear()
    }

    override fun isShutdown(): Boolean {
        return isShutdown.get()
    }

    override fun shutdown() {
        if (isShutdown.compareAndSet(false, true)) {
            cancelAll(true)
        }
    }

    override fun checkShutdown() {
        if (isShutdown.get()) {
            throw RejectedExecutionException("Executor has been shut down")
        }
    }

    private class BukkitTaskFuture(private val task: org.bukkit.scheduler.BukkitTask) : Future<Void> {
        override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
            task.cancel()
            return true
        }

        override fun isCancelled(): Boolean = task.isCancelled
        override fun isDone(): Boolean = task.isCancelled
        override fun get(): Void? = throw UnsupportedOperationException("Cannot wait for periodic task completion")
        override fun get(timeout: Long, unit: TimeUnit): Void? = throw UnsupportedOperationException("Cannot wait for periodic task completion")
    }
}
