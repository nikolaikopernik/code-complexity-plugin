package com.github.nikolaikopernik.codecomplexity.settings

import com.intellij.util.ui.ImageUtil
import org.apache.batik.transcoder.TranscoderInput
import org.apache.batik.transcoder.TranscoderOutput
import org.apache.batik.transcoder.image.ImageTranscoder
import org.apache.tools.ant.filters.StringInputStream
import java.awt.image.BufferedImage
import java.nio.charset.StandardCharsets
import javax.swing.Icon
import javax.swing.ImageIcon


class SVGTranscoder : ImageTranscoder() {
    private var image: BufferedImage? = null
    override fun createImage(w: Int, h: Int): BufferedImage {
        image = ImageUtil.createImage(w, h, BufferedImage.TYPE_INT_ARGB)
        return image!!
    }

    override fun writeImage(ing: BufferedImage?, out: TranscoderOutput?) {
        TODO("Not yet implemented")
    }

    fun getImage(): BufferedImage {
        return image!!
    }

    companion object
}

fun SVGTranscoder.Companion.loadAndSwitchColors(path: String,
                                                colorFrom: String,
                                                colorTo: String): Icon {
    val transcoder = SVGTranscoder()
//    val hints = TranscodingHints()
//    hints[ImageTranscoder.KEY_WIDTH] = 40
//    hints[ImageTranscoder.KEY_HEIGHT] = 40
//    transcoder.transcodingHints = hints
    val svgContent = SVGTranscoder::class.java.classLoader.getResourceAsStream(path)
        ?.bufferedReader(StandardCharsets.UTF_8).use { it?.readText() }
        ?.toString()
        ?.replace(colorFrom, colorTo) ?: throw RuntimeException("Icon file $path is not found")
    transcoder.transcode(TranscoderInput(StringInputStream(svgContent, "UTF-8")), null)
    return ImageIcon(transcoder.getImage())
}
