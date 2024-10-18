package net.rk4z.s1.pluginBase.events

import net.rk4z.beacon.Event

class PluginLoadEvent {
    class Pre : Event() {
        companion object {
            private val instance = Pre()

            fun get(): Pre {
                return instance
            }
        }
    }
    class Post : Event() {
        companion object {
            private val instance = Post()

            fun get(): Post {
                return instance
            }
        }
    }
}

class PluginEnableEvent {
    class Pre : Event() {
        companion object {
            private val instance = Pre()

            fun get(): Pre {
                return instance
            }
        }
    }
    class Post : Event() {
        companion object {
            private val instance = Post()

            fun get(): Post {
                return instance
            }
        }
    }
}

class PluginDisableEvent {
    class Pre : Event() {
        companion object {
            private val instance = Pre()

            fun get(): Pre {
                return instance
            }
        }
    }
    class Post : Event() {
        companion object {
            private val instance = Post()

            fun get(): Post {
                return instance
            }
        }
    }
}