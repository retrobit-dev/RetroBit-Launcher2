package com.retrobit.launcher.ui.theme

import androidx.compose.ui.graphics.Color

// Paper White (Kindle E-Ink)
val EInkPaperBg = Color(0xFFF0F2EB)
val EInkSurface = Color(0xFFE4E7DD)
val EInkSubtle = Color(0xFFD8DCD0)
val EInkInkPrimary = Color(0xFF111210)
val EInkInkSecondary = Color(0xFF484B45)
val EInkInkMuted = Color(0xFF797D74)

// Dark E-Ink / Slate Onyx
val SlateOnyxBg = Color(0xFF121415)
val SlateOnyxSurface = Color(0xFF1A1D1F)
val SlateOnyxInkPrimary = Color(0xFFF2F5ED)
val SlateOnyxInkSecondary = Color(0xFFABB0A3)
val SlateOnyxInkMuted = Color(0xFF6B7067)

// GameBoy Pocket 90s
val Pocket90sBg = Color(0xFFC7CEB2)
val Pocket90sSurface = Color(0xFFB5BD9D)
val Pocket90sInkPrimary = Color(0xFF1F2814)
val Pocket90sInkSecondary = Color(0xFF3B4728)

// Stark 1-Bit Noir
val Stark1BitBg = Color(0xFFFFFFFF)
val Stark1BitSurface = Color(0xFFFFFFFF)
val Stark1BitInk = Color(0xFF000000)

data class RetroBitColors(
    val background: Color,
    val surface: Color,
    val subtle: Color,
    val inkPrimary: Color,
    val inkSecondary: Color,
    val inkMuted: Color,
    val border: Color
)

val LightEInkPalette = RetroBitColors(
    background = EInkPaperBg,
    surface = EInkSurface,
    subtle = EInkSubtle,
    inkPrimary = EInkInkPrimary,
    inkSecondary = EInkInkSecondary,
    inkMuted = EInkInkMuted,
    border = EInkInkPrimary
)

val DarkEInkPalette = RetroBitColors(
    background = SlateOnyxBg,
    surface = SlateOnyxSurface,
    subtle = Color(0xFF252A2D),
    inkPrimary = SlateOnyxInkPrimary,
    inkSecondary = SlateOnyxInkSecondary,
    inkMuted = SlateOnyxInkMuted,
    border = SlateOnyxInkPrimary
)

val Pocket90sPalette = RetroBitColors(
    background = Pocket90sBg,
    surface = Pocket90sSurface,
    subtle = Color(0xFF9FA786),
    inkPrimary = Pocket90sInkPrimary,
    inkSecondary = Pocket90sInkSecondary,
    inkMuted = Color(0xFF5E6B47),
    border = Pocket90sInkPrimary
)

val Stark1BitPalette = RetroBitColors(
    background = Stark1BitBg,
    surface = Stark1BitSurface,
    subtle = Stark1BitSurface,
    inkPrimary = Stark1BitInk,
    inkSecondary = Stark1BitInk,
    inkMuted = Stark1BitInk,
    border = Stark1BitInk
)
