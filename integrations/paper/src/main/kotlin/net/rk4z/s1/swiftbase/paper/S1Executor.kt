package net.rk4z.s1.swiftbase.paper

import net.rk4z.s1.swiftbase.core.S0Executor
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import java.util.concurrent.CountDownLatch

import java.util.concurrent.atomic.AtomicBoolean

class S1Executor(private val plugin: Plugin) : S0Executor {
    private val isShutdown = AtomicBoolean(false)

    override fun <T> execute(task: () -> T): T {
        shutdownCheck()
        val resultHolder = arrayOfNulls<Any>(1)
        val latch = CountDownLatch(1)

        Bukkit.getScheduler().runTask(plugin, Runnable {
            try {
                resultHolder[0] = task()
            } finally {
                latch.countDown()
            }
        })

        latch.await()
        @Suppress("UNCHECKED_CAST")
        return resultHolder[0] as T
    }

    override fun <T> executeAsync(task: () -> T): T {
        shutdownCheck()
        val resultHolder = arrayOfNulls<Any>(1)
        val latch = CountDownLatch(1)

        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            try {
                resultHolder[0] = task()
            } finally {
                latch.countDown()
            }
        })

        latch.await()
        @Suppress("UNCHECKED_CAST")
        return resultHolder[0] as T
    }

    override fun execute(task: Runnable) {
        shutdownCheck()
        Bukkit.getScheduler().runTask(plugin, task)
    }

    override fun executeAsync(task: Runnable) {
        shutdownCheck()
        Bukkit.getScheduler().runTaskAsynchronously(plugin, task)
    }

    override fun executeLater(task: Runnable, delay: Long) {
        shutdownCheck()
        Bukkit.getScheduler().runTaskLater(plugin, task, delay)
    }

    override fun <T> executeLater(task: () -> T, delay: Long): T {
        shutdownCheck()
        val resultHolder = arrayOfNulls<Any>(1)
        val latch = CountDownLatch(1)

        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            try {
                resultHolder[0] = task()
            } finally {
                latch.countDown()
            }
        }, delay)

        latch.await()
        @Suppress("UNCHECKED_CAST")
        return resultHolder[0] as T
    }

    override fun executeAsyncLater(task: Runnable, delay: Long) {
        shutdownCheck()
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, delay)
    }

    override fun <T> executeAsyncLater(task: () -> T, delay: Long): T {
        shutdownCheck()
        val resultHolder = arrayOfNulls<Any>(1)
        val latch = CountDownLatch(1)

        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, Runnable {
            try {
                resultHolder[0] = task()
            } finally {
                latch.countDown()
            }
        }, delay)

        latch.await()
        @Suppress("UNCHECKED_CAST")
        return resultHolder[0] as T
    }

    override fun executeTimer(task: Runnable, delay: Long, period: Long) {
        shutdownCheck()
        Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period)
    }

    override fun <T> executeTimer(task: () -> T, delay: Long, period: Long): T {
        shutdownCheck()
        throw UnsupportedOperationException("Timer tasks with return values are not supported.")
    }

    override fun executeAsyncTimer(task: Runnable, delay: Long, period: Long) {
        shutdownCheck()
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delay, period)
    }

    override fun <T> executeAsyncTimer(task: () -> T, delay: Long, period: Long): T {
        shutdownCheck()
        throw UnsupportedOperationException("Timer tasks with return values are not supported.")
    }

    override fun shutdownCheck() {
        if (isShutdown.get()) {
            throw IllegalStateException("S1Executor is shut down and cannot accept new tasks.")
        }
    }

    override fun shutdown() {
        isShutdown.set(true)
    }
}
