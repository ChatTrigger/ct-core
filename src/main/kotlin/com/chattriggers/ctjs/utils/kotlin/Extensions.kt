package com.chattriggers.ctjs.utils.kotlin

import net.minecraft.client.renderer.Tessellator

fun ITextComponent.getStyling(): TextStyle =
        //#if MC<=10809
        this.chatStyle!!
        //#else
        //$$ this.style
        //#endif
fun TextStyle.getClick(): TextClickEvent? =
        //#if MC<=10809
        chatClickEvent
        //#else
        //$$ clickEvent
        //#endif

fun TextStyle.getHover(): TextHoverEvent? =
        //#if MC<=10809
        chatHoverEvent
        //#else
        //$$ hoverEvent
        //#endif

fun Tessellator.getRenderer(): WorldRenderer =
        //#if MC<=10809
        worldRenderer
        //#else
        //$$ buffer
        //#endif

operator fun String.times(times: Number): String {
    val stringBuilder = StringBuilder()

    for (i in 0..times.toInt()) {
        stringBuilder.append(this)
    }

    return stringBuilder.toString()
}

fun <T : Any, R> T.getPrivateValue(vararg names: String): R {
    return this::class.java.fields.find { it.name in names }!!.get(this) as R
}

fun <T : Any, R> T.setPrivateValue(value: R, vararg names: String) {
    return this::class.java.fields.find { it.name in names }!!.set(this, value)
}