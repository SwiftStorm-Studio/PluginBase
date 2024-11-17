package net.rk4z.s1.swiftbase.paper

import net.rk4z.s1.swiftbase.core.S0Executor
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.ConcurrentLinkedQueue

@Suppress("DuplicatedCode")
class S1Executor internal constructor(private val plugin: JavaPlugin) : S0Executor {
    private val isShutdown = AtomicBoolean(false)
    private val runningTasks = ConcurrentLinkedQueue<Future<*>>() // スレッドセーフなリストに変更

    override fun <T> submit(task: Callable<T>): Future<T> {
        checkShutdown()
        val future = Bukkit.getScheduler().callSyncMethod(plugin, task) // 既存のFuture
        val completableFuture = CompletableFuture<T>() // CompletableFutureを作成

        // Futureの結果をポーリングしてCompletableFutureに渡す
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            try {
                val result = future.get() // Futureの結果を取得
                completableFuture.complete(result) // CompletableFutureを完了
            } catch (e: Exception) {
                completableFuture.completeExceptionally(e) // 例外発生時
            } finally {
                runningTasks.remove(completableFuture) // タスク終了時にリストから削除
            }
        })

        runningTasks.add(completableFuture)
        return completableFuture
    }


    override fun <T> submitAsync(task: Callable<T>): Future<T> {
        checkShutdown()

        // カスタムExecutorを使用
        val executor = BukkitSchedulerExecutor(plugin, Bukkit.getScheduler(), async = true)
        val future = CompletableFuture.supplyAsync({
            task.call()
        }, executor)

        runningTasks.add(future)
        future.whenComplete { _, _ -> runningTasks.remove(future) } // タスク終了時に削除
        return future
    }

    override fun execute(task: Runnable) {
        checkShutdown()
        val future = CompletableFuture.runAsync {
            Bukkit.getScheduler().runTask(plugin, task)
        }
        runningTasks.add(future)
        future.whenComplete { _, _ -> runningTasks.remove(future) } // タスク終了時に削除
    }

    override fun executeAsync(task: Runnable) {
        checkShutdown()
        val future = CompletableFuture.runAsync {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task)
        }
        runningTasks.add(future)
        future.whenComplete { _, _ -> runningTasks.remove(future) } // タスク終了時に削除
    }

    override fun executeTimer(task: Runnable, delay: Long, period: Long) {
        checkShutdown()
        val future = CompletableFuture.runAsync {
            Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period)
        }
        runningTasks.add(future)
        future.whenComplete { _, _ -> runningTasks.remove(future) } // タスク終了時に削除
    }

    override fun executeAsyncTimer(task: Runnable, delay: Long, period: Long) {
        checkShutdown()
        val future = CompletableFuture.runAsync {
            Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delay, period)
        }
        runningTasks.add(future)
        future.whenComplete { _, _ -> runningTasks.remove(future) } // タスク終了時に削除
    }

    override fun schedule(task: Runnable, delay: Long) {
        checkShutdown()
        val future = CompletableFuture.runAsync {
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, delay)
        }
        runningTasks.add(future)
        future.whenComplete { _, _ -> runningTasks.remove(future) } // タスク終了時に削除
    }

    override fun scheduleAtFixedRate(task: Runnable, delay: Long, period: Long) {
        checkShutdown()
        val future = CompletableFuture.runAsync {
            Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delay, period)
        }
        runningTasks.add(future)
        future.whenComplete { _, _ -> runningTasks.remove(future) } // タスク終了時に削除
    }

    @Throws(TimeoutException::class, InterruptedException::class, ExecutionException::class)
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
            runningTasks.remove(future) // 終了時に削除
        }
    }

    override fun <T> submitWithDelay(task: Callable<T>, delay: Long): Future<T> {
        checkShutdown()

        val future = CompletableFuture<T>()

        // BukkitSchedulerExecutorを使用して非同期タスクを実行
        val executor = BukkitSchedulerExecutor(plugin, Bukkit.getScheduler(), async = true)

        // 指定した遅延時間後にタスクを実行
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, Runnable {
            CompletableFuture.supplyAsync({
                try {
                    val result = task.call() // タスクを実行
                    future.complete(result) // 結果をCompletableFutureに設定
                } catch (e: Exception) {
                    future.completeExceptionally(e) // エラーの場合
                }
            }, executor).whenComplete { _, _ ->
                runningTasks.remove(future) // タスク終了時に削除
            }
        }, delay)

        runningTasks.add(future)
        return future
    }

    @Throws(InterruptedException::class)
    override fun <T> invokeAll(tasks: Collection<Callable<T>>): List<Future<T>> {
        checkShutdown()
        val futures = tasks.map { task -> submit(task) }
        return futures
    }

    @Throws(InterruptedException::class)
    override fun awaitTermination(timeout: Long, timeUnit: TimeUnit): Boolean {
        val endTime = System.nanoTime() + timeUnit.toNanos(timeout)
        while (System.nanoTime() < endTime) {
            if (runningTasks.all { it.isDone || it.isCancelled }) {
                return true
            }
            Thread.sleep(100) // Polling delay
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
            runningTasks.forEach { it.cancel(true) }
            runningTasks.clear()
            awaitTermination(10, TimeUnit.SECONDS) // Wait for all tasks to finish with a timeout
        }
    }

    override fun checkShutdown() {
        if (isShutdown.get()) {
            throw RejectedExecutionException("Executor has been shut down")
        }
    }
}
