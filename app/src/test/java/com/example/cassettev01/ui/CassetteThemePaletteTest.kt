package com.example.cassettev01.ui

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class CassetteThemePaletteTest {
    @Test
    fun retainsAllFiveOriginalPalettes() {
        assertEquals(5, CassetteThemePalette.entries.size)
        assertEquals(Color(0xFF151719), CassetteThemePalette.CARBON.bg)
        assertEquals(Color(0xFFD7DCE0), CassetteThemePalette.CARBON.fg)
        assertEquals(Color(0xFF141A1B), CassetteThemePalette.NEON.bg)
        assertEquals(Color(0xFFD2E0E1), CassetteThemePalette.NEON.fg)
        assertEquals(Color(0xFF141B17), CassetteThemePalette.JADE.bg)
        assertEquals(Color(0xFFD3E1D7), CassetteThemePalette.JADE.fg)
        assertEquals(Color(0xFF19161E), CassetteThemePalette.VIOLET.bg)
        assertEquals(Color(0xFFDED5E8), CassetteThemePalette.VIOLET.fg)
        assertEquals(Color(0xFF1B1913), CassetteThemePalette.GOLD.bg)
        assertEquals(Color(0xFFE4DDCA), CassetteThemePalette.GOLD.fg)
    }
}
