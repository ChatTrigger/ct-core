package com.chattriggers.ctjs.events

import com.google.common.base.Throwables
import com.google.common.reflect.TypeToken
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

annotation class Subscriber

object EventBus {
    private val eventToListeners = hashMapOf<Class<*>, MutableList<IEventListener>>()
    private val listeners = ConcurrentHashMap<Any, MutableList<IEventListener>>()

    fun register(target: Any) {
        if (listeners.containsKey(target)) {
            return
        }

        val supers = TypeToken.of(target.javaClass).types.rawTypes()

        for (method in target.javaClass.methods) {
            for (cls in supers) {
                try {
                    val real = cls.getDeclaredMethod(method.name, *method.parameterTypes)
                    if (real.isAnnotationPresent(Subscriber::class.java)) {
                        val parameterTypes = method.parameterTypes
                        if (parameterTypes.size != 1) {
                            throw IllegalArgumentException(
                                "Method " + method + " has @SubscribeEvent annotation, but requires " + parameterTypes.size +
                                        " arguments.  Event handler methods must require a single argument."
                            )
                        }

                        val eventType = parameterTypes[0]

                        if (!Event::class.java.isAssignableFrom(eventType)) {
                            throw IllegalArgumentException("Method $method has @SubscribeEvent annotation, but takes a argument that is not an Event $eventType")
                        }

                        register(eventType, target, real)
                        break
                    }
                } catch (e: NoSuchMethodException) {
                }

            }
        }
    }

    private fun register(eventType: Class<*>, target: Any, method: Method) {
        try {
            val listener = ASMEventHandler(target, method)
            eventToListeners.getOrPut(eventType) { mutableListOf() }.add(listener)

            listeners.getOrPut(target) { mutableListOf() }.add(listener)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    fun unregister(obj: Any) {
        val list = listeners.remove(obj) ?: return

        list.forEach { listener ->
            eventToListeners.values.forEach { it.removeIf { it == listener } }
        }
    }

    fun post(event: Event): Boolean {
        try {
            eventToListeners[event::class.java]?.forEach {
                it.invoke(event)
            }
        } catch (throwable: Throwable) {
            Throwables.propagate(throwable)
        }

        return (if (event is CancellableEvent) event.isCancelled() else false)
    }
}