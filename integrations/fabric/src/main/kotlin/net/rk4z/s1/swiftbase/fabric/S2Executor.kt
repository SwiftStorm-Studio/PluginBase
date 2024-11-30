package net.rk4z.s1.swiftbase.fabric

import net.fabricmc.api.EnvType
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.rk4z.s1.swiftbase.core.S0Executor
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean

class S2Executor : S0Executor {
    private val isShutdown = AtomicBoolean(false)
    private val executor = Executors.newScheduledThreadPool(1)
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

    override fun <T> execute(task: () -> T): T? {
        shutdownCheck()

        // 結果を保持する変数
        var result: T? = null

        if (FabricLoader.getInstance().environmentType == EnvType.CLIENT) {
            val latch = CountDownLatch(1) // 同期制御用
            runOnClientMainThread {
                try {
                    result = task()
                } catch (ex: Exception) {
                    throw RuntimeException("Task execution failed", ex)
                } finally {
                    latch.countDown()
                }
            }
            latch.await() // クライアントのメインスレッドタスクが終わるまで待つ
        } else {
            val latch = CountDownLatch(1) // 同期制御用
            runOnServerMainThread {
                try {
                    result = task()
                } catch (ex: Exception) {
                    throw RuntimeException("Task execution failed", ex)
                } finally {
                    latch.countDown()
                }
            }
            latch.await() // サーバーのメインスレッドタスクが終わるまで待つ
        }

        // 結果を返す
        return result
    }

    override fun <T> executeAsync(task: () -> T): T? {
        shutdownCheck() // シャットダウン状態を確認

        val future = CompletableFuture<T>() // 非同期タスクの結果を保持するオブジェクト

        executor.submit {
            try {
                // タスクを実行し、結果を future に格納
                val result = task()
                future.complete(result)
            } catch (ex: Exception) {
                // 例外が発生した場合、 future にエラー情報を設定
                future.completeExceptionally(ex)
            }
        }

        // タスクの完了を待ち、結果を返す
        return try {
            future.get() // タスク完了までブロックして待つ
        } catch (ex: ExecutionException) {
            throw RuntimeException("Async task execution failed", ex.cause)
        } catch (ex: InterruptedException) {
            throw RuntimeException("Task was interrupted", ex)
        }
    }

    override fun execute(task: Runnable) {
        shutdownCheck()
        if (FabricLoader.getInstance().environmentType == EnvType.CLIENT) {
            runOnClientMainThread(task)
        } else {
            runOnServerMainThread(task)
        }
    }

    override fun executeAsync(task: Runnable) {
        shutdownCheck()
        val future = executor.submit(task)
        runningTasks.add(future)
    }

    override fun <T> executeLater(task: () -> T, delay: Long): T? {
        shutdownCheck()

        var result: T? = null
        val latch = CountDownLatch(1)

        if (FabricLoader.getInstance().environmentType == EnvType.CLIENT) {
            executor.schedule({
                runOnClientMainThread {
                    try {
                        result = task()
                    } catch (ex: Exception) {
                        throw RuntimeException("Task execution failed", ex)
                    } finally {
                        latch.countDown()
                    }
                }
            }, delay, TimeUnit.MILLISECONDS)
        } else {
            executor.schedule({
                runOnServerMainThread {
                    try {
                        result = task()
                    } catch (ex: Exception) {
                        throw RuntimeException("Task execution failed", ex)
                    } finally {
                        latch.countDown()
                    }
                }
            }, delay, TimeUnit.MILLISECONDS)
        }

        latch.await()
        return result
    }

    override fun executeLater(task: Runnable, delay: Long) {
        shutdownCheck()

        if (FabricLoader.getInstance().environmentType == EnvType.CLIENT) {
            // クライアント環境でメインスレッドで遅延実行
            executor.schedule({
                runOnClientMainThread {
                    try {
                        task.run()
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
            }, delay, TimeUnit.MILLISECONDS)
        } else {
            // サーバー環境でメインスレッドで遅延実行
            executor.schedule({
                runOnServerMainThread {
                    try {
                        task.run()
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
            }, delay, TimeUnit.MILLISECONDS)
        }
    }

    override fun <T> executeAsyncLater(task: () -> T, delay: Long): T? {
        shutdownCheck()

        val future = CompletableFuture<T>()

        executor.schedule({
            try {
                val result = task()
                future.complete(result)
            } catch (ex: Exception) {
                future.completeExceptionally(ex)
            }
        }, delay, TimeUnit.MILLISECONDS)

        // タスクの完了を待ち、結果を返す
        return try {
            future.get()
        } catch (ex: ExecutionException) {
            throw RuntimeException("Async delayed task execution failed", ex.cause)
        } catch (ex: InterruptedException) {
            throw RuntimeException("Task was interrupted", ex)
        }
    }

    override fun executeAsyncLater(task: Runnable, delay: Long) {
        shutdownCheck()

        executor.schedule({
            try {
                task.run()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }, delay, TimeUnit.MILLISECONDS)
    }

    override fun <T> executeTimer(task: () -> T, delay: Long, period: Long): T? {
        shutdownCheck()

        // タスクの実行結果を保持する変数
        var lastResult: T? = null
        val latch = CountDownLatch(1) // 初回タスクの完了を待つためのラッチ

        if (FabricLoader.getInstance().environmentType == EnvType.CLIENT) {
            executor.scheduleAtFixedRate({
                runOnClientMainThread {
                    try {
                        lastResult = task()
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    } finally {
                        latch.countDown() // 初回実行時にラッチを開放
                    }
                }
            }, delay, period, TimeUnit.MILLISECONDS)
        } else {
            executor.scheduleAtFixedRate({
                runOnServerMainThread {
                    try {
                        lastResult = task()
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    } finally {
                        latch.countDown() // 初回実行時にラッチを開放
                    }
                }
            }, delay, period, TimeUnit.MILLISECONDS)
        }

        try {
            latch.await()
        } catch (ex: InterruptedException) {
            throw RuntimeException("Task was interrupted", ex)
        }

        return lastResult
    }

    override fun executeTimer(task: Runnable, delay: Long, period: Long) {
        shutdownCheck()

        if (FabricLoader.getInstance().environmentType == EnvType.CLIENT) {
            executor.scheduleAtFixedRate({
                runOnClientMainThread {
                    try {
                        task.run()
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
            }, delay, period, TimeUnit.MILLISECONDS)
        } else {
            executor.scheduleAtFixedRate({
                runOnServerMainThread {
                    try {
                        task.run()
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
            }, delay, period, TimeUnit.MILLISECONDS)
        }
    }

    override fun <T> executeAsyncTimer(task: () -> T, delay: Long, period: Long): T? {
        throw UnsupportedOperationException("Async timer tasks are not supported")
    }

    override fun executeAsyncTimer(task: Runnable, delay: Long, period: Long) {
        shutdownCheck()

        executor.scheduleAtFixedRate({
            try {
                task.run()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }, delay, period, TimeUnit.MILLISECONDS)
    }

    override fun shutdown() {
        if (isShutdown.compareAndSet(false, true)) {
            runningTasks.forEach { it.cancel(true) }
            runningTasks.clear()
            executor.shutdown()
        }
    }

    override fun shutdownCheck() {
        if (isShutdown.get()) {
            throw RejectedExecutionException("Executor has been shut down")
        }
    }
}