package com.chattriggers.ctjs.launch.launcher

import com.chattriggers.ctjs.launch.asm.FMLDeobfuscatingRemapper
import net.minecraft.launchwrapper.IClassNameTransformer
import net.minecraft.launchwrapper.IClassTransformer
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter

class DeobfuscationTransformer : IClassTransformer, IClassNameTransformer {
    override fun transform(name: String, transformedName: String, bytes: ByteArray?): ByteArray? {
        if (bytes == null) {
            return null
        }
        val classReader = ClassReader(bytes)
        val classWriter = ClassWriter(WRITER_FLAGS)
        val remapAdapter = RemappingAdapter(classWriter)
        classReader.accept(remapAdapter,
            READER_FLAGS
        )
        return classWriter.toByteArray()
    }

    override fun remapClassName(name: String): String {
        return FMLDeobfuscatingRemapper.map(name.replace('.', '/')).replace('/', '.')
    }

    override fun unmapClassName(name: String): String {
        return FMLDeobfuscatingRemapper.unmap(name.replace('.', '/')).replace('/', '.')
    }

    companion object {
        private val RECALC_FRAMES =
            java.lang.Boolean.parseBoolean(System.getProperty("FORGE_FORCE_FRAME_RECALC", "false"))
        private val WRITER_FLAGS = ClassWriter.COMPUTE_MAXS or if (RECALC_FRAMES) ClassWriter.COMPUTE_FRAMES else 0
        private val READER_FLAGS = if (RECALC_FRAMES) ClassReader.SKIP_FRAMES else ClassReader.EXPAND_FRAMES
        // COMPUTE_FRAMES causes classes to be loaded, which could cause issues if the classes do not exist.
        // However in testing this has not happened. {As we run post SideTransfromer}
        // If reported we need to add a custom implementation of ClassWriter.getCommonSuperClass
        // that does not cause class loading.
    }

}