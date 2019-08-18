package com.chattriggers.ctjs.minecraft.libs.renderer

import com.chattriggers.ctjs.CTJS
import com.chattriggers.ctjs.events.EventBus
import com.chattriggers.ctjs.events.RenderGameOverlayEvent
import com.chattriggers.ctjs.events.Subscriber
import com.chattriggers.ctjs.utils.kotlin.External
import net.minecraft.client.renderer.texture.DynamicTexture
import java.awt.image.BufferedImage
import java.io.File
import java.net.URL
import javax.imageio.ImageIO

@External
class Image(var image: BufferedImage?) {
    private lateinit var texture: DynamicTexture
    private val textureWidth = image?.width ?: 0
    private val textureHeight = image?.height ?: 0

    init {
        EventBus.register(this)
    }

    @JvmOverloads
    constructor(name: String, url: String? = null) : this(getBufferedImage(name, url))

    fun getTextureWidth(): Int = this.textureWidth
    fun getTextureHeight(): Int = this.textureHeight
    fun getTexture(): DynamicTexture {
        if (!this::texture.isInitialized) {
            // We're trying to access the texture before initialization. Presumably, the game overlay render event
            // hasn't fired yet so we haven't loaded the texture. Let's hope this is a rendering context!
            try {
                texture = DynamicTexture(image)
                image = null

                EventBus.unregister(this)
            } catch (e: Exception) {
                // Unlucky. This probably wasn't a rendering context.
                println("Trying to bake texture in a non-rendering context.")

                throw e
            }
        }

        return this.texture
    }

    @Subscriber
    fun onRender(event: RenderGameOverlayEvent) {
        if (image != null) {
            texture = DynamicTexture(image)
            image = null

            EventBus.unregister(this)
        }
    }

    @JvmOverloads
    fun draw(x: Double, y: Double,
             width: Double = this.textureWidth.toDouble(),
             height: Double = this.textureHeight.toDouble()) = apply {
        if (image != null) return@apply

        Renderer.drawImage(this, x, y, width, height)
    }
}

private fun getBufferedImage(name: String, url: String? = null): BufferedImage? {
    val resourceFile = File(CTJS.assetsDir, name)

    if (resourceFile.exists()) {
        return ImageIO.read(resourceFile)
    }

    val image = ImageIO.read(URL(url))
    ImageIO.write(image, "png", resourceFile)
    return image
}