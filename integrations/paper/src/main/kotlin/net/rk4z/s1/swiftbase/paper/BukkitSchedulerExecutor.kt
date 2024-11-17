package net.rk4z.s1.swiftbase.paper

import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitScheduler
import java.util.concurrent.Executor

class BukkitSchedulerExecutor(
    private val plugin: Plugin,
    private val scheduler: BukkitScheduler,
    private val async: Boolean
) : Executor {
    override fun execute(command: Runnable) {
        if (async) {
            scheduler.runTaskAsynchronously(plugin, command)
        } else {
            scheduler.runTask(plugin, command)
        }
    }
}
