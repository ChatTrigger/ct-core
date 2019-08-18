package com.chattriggers.ctjs.launch.launcher

import net.minecraft.launchwrapper.ITweaker
import net.minecraft.launchwrapper.Launch
import net.minecraft.launchwrapper.LaunchClassLoader
import org.spongepowered.asm.launch.MixinBootstrap
import org.spongepowered.asm.mixin.MixinEnvironment
import org.spongepowered.asm.mixin.Mixins
import java.io.File
import java.net.MalformedURLException
import jdk.nashorn.internal.objects.NativeArray.forEach

class CTJSTweaker : ITweaker {
    init {
        System.setProperty(
            "java.net.preferIPv4Stack",
            "true"
        ) //Lets do this as early as possible. Vanilla does it in Main.main
    }

    val args = mutableListOf<String>()

    override fun acceptOptions(args: MutableList<String>, gameDir: File?, assetsDir: File?, profile: String?) {
        this.args.addAll(args);

        addArg("gameDir", gameDir);
        addArg("assetsDir", assetsDir);
        addArg("version", profile);
    }

    override fun injectIntoClassLoader(classLoader: LaunchClassLoader) {
        // Reobfuscate
        val deobf = classLoader.getClassBytes("net.minecraft.world.World") != null
        Launch.blackboard["deobfuscatedEnvironment"] = deobf

        if (!deobf) {
            classLoader.registerTransformer("com.chattriggers.ctjs.launch.launcher.DeobfuscationTransformer")
        }

        println("Setting up Mixins...")
        MixinBootstrap.init()
        Mixins.addConfiguration("mixins.ctjs.json")
        MixinEnvironment.getDefaultEnvironment().obfuscationContext = "searge"
        MixinEnvironment.getDefaultEnvironment().side = MixinEnvironment.Side.CLIENT

        try {
            classLoader.addURL(File(System.getProperty("java.home"), "lib/ext/nashorn.jar").toURI().toURL())
        } catch (e: MalformedURLException) {
            e.printStackTrace()
        }
    }

    override fun getLaunchTarget(): String {
        return "net.minecraft.client.main.Main"
    }

    override fun getLaunchArguments(): Array<String> {
        return args.toTypedArray()
    }

    private fun addArg(label: String, value: String?) {
        if (!this.args.contains("--$label") && value != null) {
            this.args.add("--$label")
            this.args.add(value)
        }
    }

    private fun addArg(args: String, file: File?) {
        if (file == null) {
            return
        }

        addArg(args, file.absolutePath)
    }

    private fun addArgs(args: Map<String, *>) {
        args.forEach { (label, value) ->
            if (value is String) {
                addArg(label, value)
            } else if (value is File) {
                addArg(label, value)
            }
        }
    }
}