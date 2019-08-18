package com.chattriggers.ctjs.launch.launcher

import com.chattriggers.ctjs.launch.asm.FMLDeobfuscatingRemapper
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.Remapper
import org.objectweb.asm.commons.RemappingClassAdapter
import org.objectweb.asm.commons.RemappingMethodAdapter

class RemappingAdapter(cv: ClassVisitor) : RemappingClassAdapter(cv, FMLDeobfuscatingRemapper) {

    override fun visit(
        version: Int,
        access: Int,
        name: String,
        signature: String,
        superName: String,
        interfaces: Array<String>?
    ) {
        val intf = interfaces ?: arrayOf()

        FMLDeobfuscatingRemapper.mergeSuperMaps(name, superName, intf)
        super.visit(version, access, name, signature, superName, intf)
    }

    override fun createRemappingMethodAdapter(access: Int, newDesc: String, mv: MethodVisitor): MethodVisitor {
        return StaticFixingMethodVisitor(
            access,
            newDesc,
            mv,
            remapper
        )
    }

    private class StaticFixingMethodVisitor(access: Int, desc: String, mv: MethodVisitor, remapper: Remapper) :
        RemappingMethodAdapter(access, desc, mv, remapper) {

        override fun visitFieldInsn(opcode: Int, originalType: String, originalName: String, desc: String) {
            // This method solves the problem of a static field reference changing type. In all probability it is a
            // compatible change, however we need to fix up the desc to point at the new type
            val type = remapper.mapType(originalType)
            val fieldName = remapper.mapFieldName(originalType, originalName, desc)
            var newDesc = remapper.mapDesc(desc)
            if (opcode == Opcodes.GETSTATIC && type.startsWith("net/minecraft/") && newDesc.startsWith("Lnet/minecraft/")) {
                val replDesc = FMLDeobfuscatingRemapper.getStaticFieldType(originalType, originalName, type, fieldName)
                if (replDesc != null) {
                    newDesc = remapper.mapDesc(replDesc)
                }
            }
            // super.super
            if (mv != null) {
                mv.visitFieldInsn(opcode, type, fieldName, newDesc)
            }
        }
    }
}