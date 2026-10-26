package com.kingplay.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadableTypographyTest {
    private val base = Typography(
        bodySmall = TextStyle(fontSize = 10.sp, lineHeight = 14.sp),
        bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
        labelSmall = TextStyle(fontSize = 11.sp),
    )

    @Test
    fun nothingEndsUpSmallerThanThirteen() {
        val t = base.readable()
        listOf(t.bodySmall, t.bodyMedium, t.labelSmall, t.labelMedium, t.labelLarge).forEach {
            if (it.fontSize.isSp) assertTrue("${it.fontSize}", it.fontSize.value >= 13f)
        }
    }

    @Test
    fun largerTextGrowsAboutTenPercent() {
        assertEquals(17.6f, base.readable().bodyLarge.fontSize.value, 0.01f)
    }

    @Test
    fun lineHeightKeepsRoomForTheBiggerText() {
        val s = base.readable().bodySmall
        assertTrue(s.lineHeight.value >= s.fontSize.value * 1.2f - 0.01f)
    }
}
