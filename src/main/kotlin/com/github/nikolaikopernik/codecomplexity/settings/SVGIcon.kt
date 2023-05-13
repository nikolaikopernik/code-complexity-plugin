package com.github.nikolaikopernik.codecomplexity.settings

import org.apache.batik.anim.dom.SAXSVGDocumentFactory
import org.apache.batik.bridge.BridgeContext
import org.apache.batik.bridge.DocumentLoader
import org.apache.batik.bridge.GVTBuilder
import org.apache.batik.bridge.UserAgent
import org.apache.batik.bridge.UserAgentAdapter
import org.apache.batik.gvt.GraphicsNode
import org.apache.batik.util.XMLResourceDescriptor
import org.apache.tools.ant.filters.StringInputStream
import java.awt.Component
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.geom.AffineTransform
import java.nio.charset.StandardCharsets
import javax.swing.Icon


class SVGIcon(private val url: String,
              val colorFrom: String? = null,
              val colorTo: String? = null) : Icon {
    private var svgIcon: GraphicsNode? = null

    /**
     * Method to fetch the SVG icon from a url
     *
     * @param url the url from which to fetch the SVG icon
     *
     * @return a graphics node object that can be used for painting
     */
    init {
        val svgContent = SVGTranscoder::class.java.classLoader.getResourceAsStream(url)
            ?.bufferedReader(StandardCharsets.UTF_8).use { it?.readText() }
            ?.toString()
            ?.let {
                if (colorFrom != null && colorTo != null) {
                    it.replace(colorFrom, colorTo)
                } else
                    it
            }

        val xmlParser = XMLResourceDescriptor.getXMLParserClassName()
        val df = SAXSVGDocumentFactory(xmlParser)
        val doc = df.createSVGDocument(url, StringInputStream(svgContent))
        val userAgent: UserAgent = UserAgentAdapter()
        val loader = DocumentLoader(userAgent)
        val ctx = BridgeContext(userAgent, loader)
        ctx.setDynamicState(BridgeContext.DYNAMIC)
        val builder = GVTBuilder()
        svgIcon = builder.build(ctx, doc)
    }

    /**
     * Method to paint the icon using Graphics2D. Note that the scaling factors have nothing to do with the zoom
     * operation, the scaling factors set the size your icon relative to the other objects on your canvas.
     *
     * @param g the graphics context used for drawing
     *
     * @param svgIcon the graphics node object that contains the SVG icon information
     *
     * @param x the X coordinate of the top left corner of the icon
     *
     * @param y the Y coordinate of the top left corner of the icon
     *
     * @param scaleX the X scaling to be applied to the icon before drawing
     *
     * @param scaleY the Y scaling to be applied to the icon before drawing
     */
    private fun paintSvgIcon(
        g: Graphics2D,
        x: Int, y: Int,
        scaleX: Double, scaleY: Double
    ) {
        val transform = AffineTransform(scaleX, 0.0, 0.0, scaleY, x.toDouble(), y.toDouble())
        svgIcon!!.transform = transform
        svgIcon!!.paint(g)
    }

    override fun getIconHeight(): Int {
        return svgIcon!!.primitiveBounds.height.toInt()
    }

    override fun getIconWidth(): Int {
        return svgIcon!!.primitiveBounds.width.toInt()
    }

    override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
        paintSvgIcon(g as Graphics2D, x, y, 2.0, 2.0)
    }
}
