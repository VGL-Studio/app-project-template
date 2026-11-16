package com.vglstudio.template.core.ads

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The three heights of the native ads that follow the screen (KB-32 criterion 3, design T-064, plan T-065): linear
 * between two marks, the floor and the ceiling kept outside them, rounded to the nearest whole dp like the numbers of
 * the design. One function each for the drawn ad, its placeholder and the space kept by the bottom bar.
 */
class NativeAdDimensTest {
    private fun assertDp(what: String, expected: Dp, actual: Dp) = assertEquals(what, expected.value, actual.value, 0f)

    @Test
    fun bottomSlotHeight_interpolatesAndClamps() {
        mapOf(300 to 90, 320 to 90, 360 to 106, 411 to 126, 600 to 200, 673 to 200).forEach { (width, height) ->
            assertDp("bottom slot at $width dp", height.dp, NativeAdDimens.bottomSlotHeight(width.dp))
        }
    }

    @Test
    fun collapsibleMediaAreaHeight_interpolatesAndClamps() {
        mapOf(320 to 160, 360 to 166, 411 to 173, 600 to 200, 673 to 200).forEach { (width, height) ->
            assertDp("media area at $width dp", height.dp, NativeAdDimens.collapsibleMediaAreaHeight(width.dp))
        }
    }

    @Test
    fun mediumHeight_interpolatesAndClamps() {
        mapOf(600 to 320, 640 to 320, 841 to 384, 891 to 400, 1000 to 400).forEach { (screenHeight, height) ->
            assertDp("medium at $screenHeight dp", height.dp, NativeAdDimens.mediumHeight(screenHeight.dp))
        }
    }

    /** KB-2, KB-32, question 7 of the plan: the low frame of medium is a card below 350 dp. */
    @Test
    fun mediumIsCompact_belowCard350dp() {
        // Control: the two screens on each side of the threshold give 349 and 350 dp.
        assertDp("medium at 732 dp", 349.dp, NativeAdDimens.mediumHeight(732.dp))
        assertDp("medium at 733 dp", 350.dp, NativeAdDimens.mediumHeight(733.dp))
        listOf(600, 640, 732).forEach {
            assertTrue("$it dp is the low frame", NativeAdDimens.mediumIsCompact(it.dp))
        }
        listOf(733, 841, 891).forEach {
            assertFalse("$it dp is not the low frame", NativeAdDimens.mediumIsCompact(it.dp))
        }
    }

    @Test
    fun heights_areWholeDp() {
        // Control: 411 dp gives 126 dp, not the 125.75 dp of the line itself.
        assertDp("bottom slot at 411 dp", 126.dp, NativeAdDimens.bottomSlotHeight(411.dp))
        (300..700).forEach { size ->
            listOf(
                "bottom slot" to NativeAdDimens.bottomSlotHeight(size.dp),
                "media area" to NativeAdDimens.collapsibleMediaAreaHeight(size.dp),
                "medium" to NativeAdDimens.mediumHeight(size.dp),
            ).forEach { (what, height) ->
                assertEquals("$what at $size dp", height.value.roundToInt().toFloat(), height.value, 0f)
            }
        }
    }
}
