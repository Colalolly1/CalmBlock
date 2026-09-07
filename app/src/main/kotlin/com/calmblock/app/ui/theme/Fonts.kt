package com.calmblock.app.ui.theme

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Same bundled faces as CalmMoney: Literata for titles and quotes, Atkinson for everything else. */
object Fonts {
    private var serifFamily: FontFamily = FontFamily.Serif
    private var sansFamily: FontFamily = FontFamily.SansSerif

    val serif: FontFamily get() = serifFamily
    val sans: FontFamily get() = sansFamily

    fun init(context: Context) {
        serifFamily = load(context, "literata", FontFamily.Serif)
        sansFamily = load(context, "atkinson", FontFamily.SansSerif)
    }

    private fun load(context: Context, prefix: String, fallback: FontFamily): FontFamily = try {
        val files = (context.assets.list("fonts") ?: emptyArray()).filter { it.startsWith(prefix) && it.endsWith(".ttf") }
        val fonts = files.mapNotNull { name ->
            val weight = if (name.contains("bold", true)) FontWeight.Bold else FontWeight.Normal
            runCatching { Font(path = "fonts/$name", assetManager = context.assets, weight = weight) }.getOrNull()
        }
        if (fonts.isEmpty()) fallback else FontFamily(fonts)
    } catch (_: Exception) {
        fallback
    }
}

object Type {
    private val grey = Color(0xFF555555)

    val screenTitle = TextStyle(fontFamily = Fonts.serif, fontSize = 24.sp, letterSpacing = (-0.3).sp)
    val cardTitle = TextStyle(fontFamily = Fonts.serif, fontSize = 19.sp)
    val hero = TextStyle(fontFamily = Fonts.serif, fontSize = 30.sp)
    val quote = TextStyle(fontFamily = Fonts.serif, fontSize = 20.sp, lineHeight = 30.sp)
    val sectionHeader = TextStyle(fontFamily = Fonts.sans, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.2.sp, color = grey)
    val row = TextStyle(fontFamily = Fonts.sans, fontSize = 16.sp)
    val meta = TextStyle(fontFamily = Fonts.sans, fontSize = 13.sp, color = grey)
    val body = TextStyle(fontFamily = Fonts.sans, fontSize = 15.sp, lineHeight = 22.sp)
    val button = TextStyle(fontFamily = Fonts.sans, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    val chip = TextStyle(fontFamily = Fonts.sans, fontSize = 14.sp)
    val tab = TextStyle(fontFamily = Fonts.sans, fontSize = 13.sp, letterSpacing = 0.3.sp)
    val fieldInput = TextStyle(fontFamily = Fonts.sans, fontSize = 17.sp, color = Color.Black)
    val figure = TextStyle(fontFamily = Fonts.sans, fontWeight = FontWeight.Bold, fontSize = 16.sp)
}
