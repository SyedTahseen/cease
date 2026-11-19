package com.cease.io

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

object Type {
    private val Font = FontFamily.SansSerif
    private fun style(size: Int, weight: FontWeight, line: Int, spacing: TextUnit = 0.sp) = TextStyle(
        fontFamily = Font, fontSize = size.sp, fontWeight = weight, lineHeight = line.sp, letterSpacing = spacing)

    val Headline = style(28, FontWeight.Bold, 34, 0.45.sp)
    val PageTitle = style(22, FontWeight.Bold, 28, 0.45.sp)
    val Heading = style(20, FontWeight.Bold, 24, 0.01f.em)
    val Subheadline = style(20, FontWeight.Normal, 24, 0.01f.em)
    val Body = style(16, FontWeight.Normal, 24, 0.03f.em)
    val BodyBold = style(16, FontWeight.Bold, 24, 0.03f.em)
    val BodyMedium = style(16, FontWeight.Medium, 24, 0.03f.em)
    val Small = style(14, FontWeight.Normal, 20, 0.02f.em)
    val SmallMedium = style(14, FontWeight.Medium, 20, 0.02f.em)
    val Caption = style(12, FontWeight.Normal, 16, 0.03f.em)
    val Label = style(12, FontWeight.Medium, 16, 0.03f.em)
    val ButtonSmall = style(16, FontWeight.Medium, 24, 0.03f.em)
    val Overline = style(10, FontWeight.Normal, 16, 0.03f.em)
}
