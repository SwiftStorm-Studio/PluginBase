package net.rk4z.s1.swiftbase.fabric

import net.fabricmc.api.EnvType
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.rk4z.s1.swiftbase.core.S0Executor
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean

class S2Executor : S0Executor {
    private val isShutdown = AtomicBoolean(false)
    private val executor = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors())
    private val runningTasks = CopyOnWriteArrayList<Future<*>>()

    private fun runOnClientMainThread(task: Runnable) {
        // ModEntryを使っていて、かつクライアント環境かどうかをチェック
        ModEntry.get()?.let { sc ->
            sc.client?.let {
                // クライアント環境であれば、taskを実行
                execute(task)
            } ?: run {
                // この関数呼び出し時に、既にクライアント側であることは確定しているが、一応エルビス演算子を使う
                ClientModEntry.get()?.client?.execute(task)
            }
        }
    }

    private fun runOnServerMainThread(task: Runnable) {
        ServerTickEvents.END_SERVER_TICK.register(ServerTickEvents.EndTick {
            task.run()
        })
    }

    override fun <T> submit(task: Callable<T>): Future<T> {
        checkShutdown()
        val future = CompletableFuture<T>()
        if (FabricLoader.getInstance().environmentType == EnvType.CLIENT) {
            runOnClientMainThread {
                try {
                    future.complete(task.call())
                } catch (ex: Exception) {
                    future.completeExceptionally(ex)
                }
            }
        } else {
            runOnServerMainThread {
                try {
                    future.complete(task.call())
                } catch (ex: Exception) {
                    future.completeExceptionally(ex)
                }
            }
        }
        runningTasks.add(future)
        return future
    }

    override fun <T> submitAsync(task: Callable<T>): Future<T> {
        checkShutdown()
        val future = executor.submit(task)
        runningTasks.add(future)
        return future
    }

    override fun execute(task: Runnable) {
        checkShutdown()
        if (FabricLoader.getInstance().environmentType == EnvType.CLIENT) {
            runOnClientMainThread(task)
        } else {
            runOnServerMainThread(task)
        }
    }

    override fun executeAsync(task: Runnable) {
        checkShutdown()
        val future = executor.submit(task)
        runningTasks.add(future)
    }

    override fun executeTimer(task: Runnable, delay: Long, period: Long) {
        checkShutdown()
        val future = executor.scheduleAtFixedRate({
            execute(task)
        }, delay, period, TimeUnit.MILLISECONDS)
        runningTasks.add(future)
    }

    override fun executeAsyncTimer(task: Runnable, delay: Long, period: Long) {
        checkShutdown()
        val future = executor.scheduleAtFixedRate(task, delay, period, TimeUnit.MILLISECONDS)
        runningTasks.add(future)
    }

    override fun schedule(task: Runnable, delay: Long) {
        checkShutdown()
        val future = executor.schedule(task, delay, TimeUnit.MILLISECONDS)
        runningTasks.add(future)
    }

    override fun scheduleAtFixedRate(task: Runnable, delay: Long, period: Long) {
        checkShutdown()
        val future = executor.scheduleAtFixedRate(task, delay, period, TimeUnit.MILLISECONDS)
        runningTasks.add(future)
    }

    override fun <T> submitWithTimeout(task: Callable<T>, timeout: Long, timeUnit: TimeUnit): T? {
        checkShutdown()
        val future = executor.submit(task)
        runningTasks.add(future)
        return try {
            future.get(timeout, timeUnit)
        } catch (e: TimeoutException) {
            future.cancel(true)
            throw e
        }
    }

    override fun <T> submitWithDelay(task: Callable<T>, delay: Long): Future<T> {
        checkShutdown()
        val future = executor.schedule(task, delay, TimeUnit.MILLISECONDS)
        runningTasks.add(future)
        return future
    }

    override fun <T> invokeAll(tasks: Collection<Callable<T>>): List<Future<T>> {
        checkShutdown()
        return executor.invokeAll(tasks)
    }

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
            executor.shutdown()
        }
    }

    override fun checkShutdown() {
        if (isShutdown.get()) {
            throw RejectedExecutionException("Executor has been shut down")
        }
    }
}
