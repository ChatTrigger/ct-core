package com.chattriggers.ctjs.events

import com.google.common.collect.Maps
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import java.lang.reflect.Method

interface IEventListener {
    operator fun invoke(event: Event)
}

class ASMEventHandler @Throws(Exception::class)
constructor(target: Any, method: Method) : IEventListener {

    private val handler: IEventListener?
    private val readable: String

    init {
        handler = createWrapper(method).getConstructor(Any::class.java).newInstance(target) as IEventListener
        readable = "ASM: " + target + " " + method.name + Type.getMethodDescriptor(method)
    }

    override fun invoke(event: Event) {
        handler?.invoke(event)
    }

    private fun createWrapper(callback: Method): Class<*> {
        if (cache.containsKey(callback)) {
            return cache.getValue(callback)
        }

        val cw = ClassWriter(0)
        var mv: MethodVisitor

        val name = getUniqueName(callback)
        val desc = name.replace('.', '/')
        val instType = Type.getInternalName(callback.declaringClass)
        val eventType = Type.getInternalName(callback.parameterTypes[0])

        /*
        System.out.println("Name:     " + name);
        System.out.println("Desc:     " + desc);
        System.out.println("InstType: " + instType);
        System.out.println("Callback: " + callback.getName() + Type.getMethodDescriptor(callback));
        System.out.println("Event:    " + eventType);
        */

        cw.visit(V1_6, ACC_PUBLIC or ACC_SUPER, desc, null, "java/lang/Object", arrayOf(HANDLER_DESC))

        cw.visitSource(".dynamic", null)
        run { cw.visitField(ACC_PUBLIC, "instance", "Ljava/lang/Object;", null, null).visitEnd() }
        run {
            mv = cw.visitMethod(ACC_PUBLIC, "<init>", "(Ljava/lang/Object;)V", null, null)
            mv.visitCode()
            mv.visitVarInsn(ALOAD, 0)
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
            mv.visitVarInsn(ALOAD, 0)
            mv.visitVarInsn(ALOAD, 1)
            mv.visitFieldInsn(PUTFIELD, desc, "instance", "Ljava/lang/Object;")
            mv.visitInsn(RETURN)
            mv.visitMaxs(2, 2)
            mv.visitEnd()
        }
        run {
            mv = cw.visitMethod(ACC_PUBLIC, "invoke", HANDLER_FUNC_DESC, null, null)
            mv.visitCode()
            mv.visitVarInsn(ALOAD, 0)
            mv.visitFieldInsn(GETFIELD, desc, "instance", "Ljava/lang/Object;")
            mv.visitTypeInsn(CHECKCAST, instType)
            mv.visitVarInsn(ALOAD, 1)
            mv.visitTypeInsn(CHECKCAST, eventType)
            mv.visitMethodInsn(INVOKEVIRTUAL, instType, callback.name, Type.getMethodDescriptor(callback), false)
            mv.visitInsn(RETURN)
            mv.visitMaxs(2, 2)
            mv.visitEnd()
        }
        cw.visitEnd()
        val ret = LOADER.define(name, cw.toByteArray())
        cache[callback] = ret
        return ret
    }

    private fun getUniqueName(callback: Method): String {
        return String.format(
            "%s_%d_%s_%s_%s", javaClass.name, IDs++,
            callback.declaringClass.simpleName,
            callback.name,
            callback.parameterTypes[0].simpleName
        )
    }

    private class ASMClassLoader : ClassLoader(ASMClassLoader::class.java.classLoader) {

        fun define(name: String, data: ByteArray): Class<*> {
            return defineClass(name, data, 0, data.size)
        }
    }

    override fun toString(): String {
        return readable
    }

    companion object {
        private var IDs = 0
        private val HANDLER_DESC = Type.getInternalName(IEventListener::class.java)
        private val HANDLER_FUNC_DESC = Type.getMethodDescriptor(IEventListener::class.java.declaredMethods[0])
        private val LOADER = ASMClassLoader()
        private val cache = Maps.newHashMap<Method, Class<*>>()
    }
}