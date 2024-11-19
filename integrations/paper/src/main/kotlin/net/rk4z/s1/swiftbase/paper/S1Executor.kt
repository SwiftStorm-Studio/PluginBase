package net.rk4z.s1.swiftbase.paper

import net.rk4z.s1.swiftbase.core.S0Executor
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import java.util.concurrent.CountDownLatch

class S1Executor(private val plugin: Plugin) : S0Executor {
    override fun <T> execute(task: () -> T): T {
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
        Bukkit.getScheduler().runTask(plugin, task)
    }

    override fun executeAsync(task: Runnable) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, task)
    }

    override fun executeLater(task: Runnable, delay: Long) {
        Bukkit.getScheduler().runTaskLater(plugin, task, delay)
    }

    override fun <T> executeLater(task: () -> T, delay: Long): T {
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
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, delay)
    }

    override fun <T> executeAsyncLater(task: () -> T, delay: Long): T {
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
        Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period)
    }

    override fun <T> executeTimer(task: () -> T, delay: Long, period: Long): T {
        val resultHolder = arrayOfNulls<Any>(1)
        val latch = CountDownLatch(1)

        Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            try {
                resultHolder[0] = task()
            } finally {
                latch.countDown()
            }
        }, delay, period)

        latch.await()
        @Suppress("UNCHECKED_CAST")
        return resultHolder[0] as T
    }

    override fun executeAsyncTimer(task: Runnable, delay: Long, period: Long) {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delay, period)
    }

    override fun <T> executeAsyncTimer(task: () -> T, delay: Long, period: Long): T {
        val resultHolder = arrayOfNulls<Any>(1)
        val latch = CountDownLatch(1)

        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, Runnable {
            try {
                resultHolder[0] = task()
            } finally {
                latch.countDown()
            }
        }, delay, period)

        latch.await()
        @Suppress("UNCHECKED_CAST")
        return resultHolder[0] as T
    }
}
