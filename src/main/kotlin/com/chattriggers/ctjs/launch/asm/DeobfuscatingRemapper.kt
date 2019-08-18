package com.chattriggers.ctjs.launch.asm

import com.chattriggers.ctjs.launch.log.RelaunchLog
import com.google.common.base.CharMatcher
import com.google.common.base.Charsets
import com.google.common.base.Splitter
import com.google.common.base.Strings
import com.google.common.collect.*
import com.google.common.io.Files
import net.minecraft.launchwrapper.LaunchClassLoader
import org.apache.commons.compress.compressors.lzma.LZMACompressorInputStream
import org.apache.logging.log4j.Level
import org.objectweb.asm.ClassReader
import org.objectweb.asm.commons.Remapper
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import java.io.File
import java.io.IOException
import java.util.*

object FMLDeobfuscatingRemapper : Remapper() {
    private val DEBUG_REMAPPING = java.lang.Boolean.parseBoolean(System.getProperty("fml.remappingDebug", "false"))
    private val DUMP_FIELD_MAPS = java.lang.Boolean.parseBoolean(
        System.getProperty(
            "fml.remappingDebug.dumpFieldMaps",
            "false"
        )
    ) && DEBUG_REMAPPING
    private val DUMP_METHOD_MAPS = java.lang.Boolean.parseBoolean(
        System.getProperty(
            "fml.remappingDebug.dumpMethodMaps",
            "false"
        )
    ) && DEBUG_REMAPPING

    private var classNameBiMap: BiMap<String, String> = ImmutableBiMap.of()

    private lateinit var rawFieldMaps: MutableMap<String, MutableMap<String, String>>
    private lateinit var rawMethodMaps: MutableMap<String, MutableMap<String, String>>

    private lateinit var fieldNameMaps: MutableMap<String, MutableMap<String, String>>
    private lateinit var methodNameMaps: MutableMap<String, MutableMap<String, String>>

    private lateinit var classLoader: LaunchClassLoader

    /*
     * Cache the field descriptions for classes so we don't repeatedly reload the same data again and again
     */
    private val fieldDescriptions = Maps.newHashMap<String, MutableMap<String, String>>()

    // Cache null values so we don't waste time trying to recompute classes with no field or method maps
    private val negativeCacheMethods = Sets.newHashSet<String>()
    private val negativeCacheFields = Sets.newHashSet<String>()

    val obfedClasses: Set<String>
        get() = ImmutableSet.copyOf(classNameBiMap.keys)

    fun setup(classLoader: LaunchClassLoader, deobfFileName: String) {
        this.classLoader = classLoader

        try {
            // get as a resource
            val classData = javaClass.getResourceAsStream(deobfFileName)

            val zis = LZMACompressorInputStream(classData)
            val srgSource = zis.bufferedReader()
            val srgList = srgSource.readLines()

            RelaunchLog.fine("Loading deobfuscation resource %s with %d records", deobfFileName, srgList.size)

            rawMethodMaps = Maps.newHashMap()
            rawFieldMaps = Maps.newHashMap()
            val builder = ImmutableBiMap.builder<String, String>()

            val splitter = Splitter.on(": ").omitEmptyStrings().trimResults()

            for (line in srgList) {
                val parts = Iterables.toArray(splitter.split(line), String::class.java)

                when (parts[0]) {
                    "CL" -> parseClass(builder, parts)
                    "MD" -> parseMethod(parts)
                    "FD" -> parseField(parts)
                }
            }
            classNameBiMap = builder.build()
        } catch (ioe: IOException) {
            RelaunchLog.log(Level.ERROR, ioe, "An error occurred loading the deobfuscation map data")
        }

        methodNameMaps = Maps.newHashMapWithExpectedSize(rawMethodMaps.size)
        fieldNameMaps = Maps.newHashMapWithExpectedSize(rawFieldMaps.size)
    }

    fun isRemappedClass(className: String): Boolean {
        return map(className) != className
    }

    private fun parseField(parts: Array<String>) {
        val oldSrg = parts[1]
        val lastOld = oldSrg.lastIndexOf('/')
        val cl = oldSrg.substring(0, lastOld)
        val oldName = oldSrg.substring(lastOld + 1)
        val newSrg = parts[2]
        val lastNew = newSrg.lastIndexOf('/')
        val newName = newSrg.substring(lastNew + 1)
        if (!rawFieldMaps.containsKey(cl)) {
            rawFieldMaps[cl] = Maps.newHashMap()
        }
        var fieldType = getFieldType(cl, oldName)
        // We might be in mcp named land, where in fact the name is "new"
        if (fieldType == null) fieldType = getFieldType(cl, newName)
        rawFieldMaps.getValue(cl)["$oldName:$fieldType"] = newName
        rawFieldMaps.getValue(cl)["$oldName:null"] = newName
    }

    private fun getFieldType(owner: String, name: String): String? {
        if (fieldDescriptions.containsKey(owner)) {
            return fieldDescriptions.getValue(owner)[name]
        }

        synchronized(fieldDescriptions) {
            try {
                val classBytes = classLoader.getClassBytes(map(owner)!!.replace('/', '.')) ?: return null

                val cr = ClassReader(classBytes)
                val classNode = ClassNode()
                cr.accept(classNode, ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)

                val resMap = Maps.newHashMap<String, String>()
                for (fieldNode in classNode.fields as List<FieldNode>) {
                    resMap[fieldNode.name] = fieldNode.desc
                }
                fieldDescriptions[owner] = resMap

                return resMap[name]
            } catch (e: IOException) {
                RelaunchLog.log(Level.ERROR, e, "A critical exception occurred reading a class file %s", owner)
            }

            return null
        }
    }

    private fun parseClass(builder: ImmutableBiMap.Builder<String, String>, parts: Array<String>) {
        builder.put(parts[1], parts[2])
    }

    private fun parseMethod(parts: Array<String>) {
        val oldSrg = parts[1]
        val lastOld = oldSrg.lastIndexOf('/')
        val cl = oldSrg.substring(0, lastOld)
        val oldName = oldSrg.substring(lastOld + 1)
        val sig = parts[2]
        val newSrg = parts[3]
        val lastNew = newSrg.lastIndexOf('/')
        val newName = newSrg.substring(lastNew + 1)
        if (!rawMethodMaps.containsKey(cl)) {
            rawMethodMaps[cl] = Maps.newHashMap()
        }
        rawMethodMaps.getValue(cl)[oldName + sig] = newName
    }

    override fun mapFieldName(owner: String, name: String, desc: String?): String {
        if (classNameBiMap.isEmpty()) {
            return name
        }

        return getFieldMap(owner)?.get("$name:$desc") ?: name
    }

    override fun map(typeName: String): String {
        if (classNameBiMap.isEmpty()) {
            return typeName
        }

        if (classNameBiMap.containsKey(typeName)) {
            return classNameBiMap.getValue(typeName)
        }

        val dollarIdx = typeName.lastIndexOf('$')
        return if (dollarIdx > -1) {
            map(typeName.substring(0, dollarIdx)) + "$" + typeName.substring(dollarIdx + 1)
        } else typeName
    }

    fun unmap(typeName: String): String {
        if (classNameBiMap.isEmpty()) {
            return typeName
        }

        if (classNameBiMap.containsValue(typeName)) {
            return classNameBiMap.inverse().getValue(typeName)
        }
        val dollarIdx = typeName.lastIndexOf('$')
        return if (dollarIdx > -1) {
            unmap(typeName.substring(0, dollarIdx)) + "$" + typeName.substring(dollarIdx + 1)
        } else typeName
    }


    override fun mapMethodName(owner: String, name: String, desc: String?): String {
        if (classNameBiMap.isEmpty()) {
            return name
        }
        val methodMap = getMethodMap(owner)
        val methodDescriptor = name + desc!!
        return methodMap?.get(methodDescriptor) ?: name
    }

    private fun getFieldMap(className: String): Map<String, String>? {
        if (!fieldNameMaps.containsKey(className) && !negativeCacheFields.contains(className)) {
            findAndMergeSuperMaps(className)
            if (!fieldNameMaps.containsKey(className)) {
                negativeCacheFields.add(className)
            }

            if (DUMP_FIELD_MAPS) {
                RelaunchLog.finer("Field map for %s : %s", className, fieldNameMaps[className])
            }
        }
        return fieldNameMaps[className]
    }

    private fun getMethodMap(className: String): Map<String, String>? {
        if (!methodNameMaps.containsKey(className) && !negativeCacheMethods.contains(className)) {
            findAndMergeSuperMaps(className)
            if (!methodNameMaps.containsKey(className)) {
                negativeCacheMethods.add(className)
            }
            if (DUMP_METHOD_MAPS) {
                RelaunchLog.finer("Method map for %s : %s", className, methodNameMaps[className])
            }

        }
        return methodNameMaps[className]
    }

    private fun findAndMergeSuperMaps(name: String) {
        try {
            var superName: String? = null
            var interfaces = arrayOfNulls<String>(0)
            val classBytes = classLoader.getClassBytes(map(name))
            if (classBytes != null) {
                val cr = ClassReader(classBytes)
                superName = cr.superName
                interfaces = cr.interfaces
            }
            mergeSuperMaps(name, superName, interfaces.requireNoNulls())
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    fun mergeSuperMaps(name: String, superName: String?, interfaces: Array<String>) {
        if (classNameBiMap.isEmpty()) {
            return
        }
        // Skip Object
        if (Strings.isNullOrEmpty(superName)) {
            return
        }

        val allParents = ImmutableList.builder<String>().add(superName!!).addAll(interfaces.toList()).build()
        // generate maps for all parent objects
        for (parentThing in allParents) {
            if (!methodNameMaps.containsKey(parentThing)) {
                findAndMergeSuperMaps(parentThing)
            }
        }
        val methodMap = Maps.newHashMap<String, String>()
        val fieldMap = Maps.newHashMap<String, String>()
        for (parentThing in allParents) {
            if (methodNameMaps.containsKey(parentThing)) {
                methodMap.putAll(methodNameMaps.getValue(parentThing))
            }
            if (fieldNameMaps.containsKey(parentThing)) {
                fieldMap.putAll(fieldNameMaps.getValue(parentThing))
            }
        }
        if (rawMethodMaps.containsKey(name)) {
            methodMap.putAll(rawMethodMaps.getValue(name))
        }
        if (rawFieldMaps.containsKey(name)) {
            fieldMap.putAll(rawFieldMaps.getValue(name))
        }
        methodNameMaps[name] = ImmutableMap.copyOf(methodMap)
        fieldNameMaps[name] = ImmutableMap.copyOf(fieldMap)
    }

    fun getStaticFieldType(oldType: String, oldName: String, newType: String, newName: String): String? {
        val fType = getFieldType(oldType, oldName)
        if (oldType == newType) {
            return fType
        }
        val newClassMap = fieldDescriptions.getOrPut(newType) { Maps.newHashMap() }

        newClassMap[newName] = fType!!
        return fType
    }
}