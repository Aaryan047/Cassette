package com.example.cassettev01.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.cassettev01.R

val EBGaramond = FontFamily(Font(R.font.eb_garamond))
val SpaceGrotesk = FontFamily(Font(R.font.space_grotesk))

enum class ThemeAppearance { DARK, LIGHT }

// Visual styles available for the deck display.
enum class WaveformStyle(val title: String, val description: String) {
    THICK_BARS("Thick bars", "Rounded bars that bounce with the music"),
    THIN_BARS("Fine bars", "A lighter, more detailed equalizer"),
    SQUIGGLE("Squiggle", "A continuous oscillating line"),
    MIRROR("Mirror", "A filled, symmetrical waveform"),
    DOTS("Dots", "Compact points that rise and fall");

    companion object {
        fun fromString(value: String?): WaveformStyle =
            entries.firstOrNull { it.name == value } ?: THICK_BARS
    }
}

// Five colour palettes, each with separate light and dark accents.
enum class CassetteThemePalette(
    val title: String,
    val bg: Color,
    val fg: Color,
    val darkAccent: Color,
    val lightAccent: Color
) {
    CARBON(
        "Cassette Carbon", Color(0xFF151719), Color(0xFFD7DCE0),
        Color(0xFFB4C7D1), Color(0xFF435F6D)
    ),
    NEON(
        "Cassette Neon", Color(0xFF141A1B), Color(0xFFD2E0E1),
        Color(0xFF63C8C0), Color(0xFF087A78)
    ),
    JADE(
        "Cassette Jade", Color(0xFF141B17), Color(0xFFD3E1D7),
        Color(0xFF78D39A), Color(0xFF226F49)
    ),
    VIOLET(
        "Cassette Violet", Color(0xFF19161E), Color(0xFFDED5E8),
        Color(0xFFC3A8E7), Color(0xFF69478F)
    ),
    GOLD(
        "Cassette Gold", Color(0xFF1B1913), Color(0xFFE4DDCA),
        Color(0xFFE0BD70), Color(0xFF86611F)
    );

    companion object {
        fun fromString(name: String?): CassetteThemePalette = entries.firstOrNull { it.name == name } ?: CARBON
    }
}

private fun Color.mix(other: Color, amount: Float): Color = Color(
    red = red + (other.red - red) * amount,
    green = green + (other.green - green) * amount,
    blue = blue + (other.blue - blue) * amount,
    alpha = 1f
)

@Composable
fun CassetteTheme(
    palette: CassetteThemePalette,
    appearance: ThemeAppearance,
    content: @Composable () -> Unit
) {
    val scheme = if (appearance == ThemeAppearance.DARK) {
        val card = palette.bg.mix(palette.fg, 0.055f)
        val raised = palette.bg.mix(palette.darkAccent, 0.12f)
        darkColorScheme(
            primary = palette.darkAccent,
            onPrimary = Color(0xFF101214),
            secondary = palette.darkAccent,
            onSecondary = Color(0xFF101214),
            tertiary = palette.darkAccent,
            background = palette.bg,
            onBackground = palette.fg,
            surface = card,
            onSurface = palette.fg,
            surfaceVariant = raised,
            onSurfaceVariant = palette.fg.copy(alpha = 0.76f),
            outline = palette.darkAccent.copy(alpha = 0.48f),
            outlineVariant = palette.darkAccent.copy(alpha = 0.25f),
            inverseSurface = palette.fg,
            inverseOnSurface = palette.bg
        )
    } else {
        val background = Color(0xFFF7F7F4).mix(palette.lightAccent, 0.025f)
        val card = background.mix(palette.lightAccent, 0.045f)
        val raised = background.mix(palette.lightAccent, 0.12f)
        lightColorScheme(
            primary = palette.lightAccent,
            onPrimary = Color.White,
            secondary = palette.lightAccent,
            onSecondary = Color.White,
            tertiary = palette.lightAccent,
            background = background,
            onBackground = Color(0xFF202122),
            surface = card,
            onSurface = Color(0xFF202122),
            surfaceVariant = raised,
            onSurfaceVariant = Color(0xFF5A5C5E),
            outline = palette.lightAccent.copy(alpha = 0.48f),
            outlineVariant = palette.lightAccent.copy(alpha = 0.24f),
            inverseSurface = Color(0xFF292B2D),
            inverseOnSurface = Color.White
        )
    }
    val typography = Typography(
        displayLarge = TextStyle(fontFamily = EBGaramond, fontWeight = FontWeight.Bold, fontSize = 42.sp, lineHeight = 46.sp),
        displayMedium = TextStyle(fontFamily = EBGaramond, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 38.sp),
        headlineLarge = TextStyle(fontFamily = EBGaramond, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 34.sp),
        headlineMedium = TextStyle(fontFamily = EBGaramond, fontWeight = FontWeight.SemiBold, fontSize = 25.sp, lineHeight = 29.sp),
        titleLarge = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 25.sp),
        titleMedium = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 21.sp),
        bodyLarge = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
        bodyMedium = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 19.sp),
        labelLarge = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 17.sp),
        labelMedium = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp)
    )
    MaterialTheme(colorScheme = scheme, typography = typography, content = content)
}
