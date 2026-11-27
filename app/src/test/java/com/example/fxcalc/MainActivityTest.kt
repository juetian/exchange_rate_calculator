package com.example.fxcalc

import android.graphics.Bitmap
import android.view.View
import android.widget.TextView
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "zh-rCN-w393dp-h852dp-xxhdpi")
class MainActivityTest {

    private val activity = run {
        MainActivity.autoRefreshOnStart = false
        Robolectric.buildActivity(MainActivity::class.java).setup().get()
    }

    private fun press(vararg ids: Int) = ids.forEach { activity.findViewById<View>(it).performClick() }
    private fun rowAmount(i: Int): String {
        val container = activity.findViewById<android.view.ViewGroup>(R.id.rowsContainer)
        return container.getChildAt(i).findViewById<TextView>(R.id.tvAmount).text.toString()
    }
    private fun rowSub(i: Int): TextView {
        val container = activity.findViewById<android.view.ViewGroup>(R.id.rowsContainer)
        return container.getChildAt(i).findViewById(R.id.tvSub)
    }

    private fun screenshot(name: String) {
        val root = activity.window.decorView
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        root.measure(
            View.MeasureSpec.makeMeasureSpec(root.width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(root.height, View.MeasureSpec.EXACTLY))
        root.layout(0, 0, root.width, root.height)
        val bmp = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        root.draw(android.graphics.Canvas(bmp))
        val dir = File("build/screenshots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun typeExpressionConvertsResult() {
        // 默认第 1 行 CNY；按 C 后输入 1,000 × 3 + 20
        press(R.id.keyClear, R.id.key1, R.id.key00, R.id.key0, R.id.keyTimes, R.id.key3,
            R.id.keyPlus, R.id.key2, R.id.key0)
        assertEquals("1,000×3+20", rowAmount(0))
        assertEquals("= 3,020", rowSub(0).text.toString())
        val usd = Currencies.FALLBACK_RATES["USD"]!! / Currencies.FALLBACK_RATES["CNY"]!! * 3020
        assertEquals(java.text.DecimalFormat("#,##0.##").format(usd), rowAmount(1))
        screenshot("expression")

        press(R.id.keyEquals)
        assertEquals("3,020", rowAmount(0))
        assertEquals(View.GONE, rowSub(0).visibility)
    }

    @Test fun tapConvertedRowAndContinueCalculating() {
        press(R.id.keyClear, R.id.key1, R.id.key0, R.id.key0)
        val usdShown = rowAmount(1)
        val container = activity.findViewById<android.view.ViewGroup>(R.id.rowsContainer)
        container.getChildAt(1).performClick()          // USD 行设为输入源
        assertEquals(usdShown, rowAmount(1))
        press(R.id.keyTimes, R.id.key2)                  // 在美元金额上 ×2
        assertEquals("$usdShown×2", rowAmount(1))
        press(R.id.key5)                                  // 继续输入不会替换
        assertEquals("$usdShown×25", rowAmount(1))
        screenshot("continue")

        press(R.id.keyEquals, R.id.key7)                 // = 之后输入数字则重新开始
        assertEquals("7", rowAmount(1))
    }

    @Test fun divisionByZeroShowsError() {
        press(R.id.keyClear, R.id.key5, R.id.keyDivide, R.id.key0)
        assertEquals("无法计算", rowSub(0).text.toString())
        assertEquals("", rowAmount(1))
    }

    @Test fun inputGuards() {
        press(R.id.keyClear, R.id.keyPlus)                // 空时 + 无效
        assertEquals("", rowAmount(0))
        press(R.id.keyDot, R.id.key5, R.id.keyPlus, R.id.keyTimes)  // 连续运算符替换
        assertEquals("0.5×", rowAmount(0))
        press(R.id.keyBack, R.id.keyDot, R.id.keyDot)      // 已有小数点，再按无效
        assertEquals("0.5", rowAmount(0))
        press(R.id.keyPercent)
        assertEquals("= 0.005", rowSub(0).text.toString())
    }
}
