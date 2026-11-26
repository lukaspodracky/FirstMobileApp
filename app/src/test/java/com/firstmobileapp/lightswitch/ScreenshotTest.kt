package com.firstmobileapp.lightswitch

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Renders the main screens to PNG files (build/screenshots) for a visual check. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h860dp-xhdpi")
class ScreenshotTest {

    @Test
    fun renderScreens() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val act = controller.get()
        val out = File(System.getProperty("user.dir"), "build/screenshots").apply { mkdirs() }
        fun call(name: String, vararg args: Any) {
            val m = MainActivity::class.java.declaredMethods.first { it.name == name && it.parameterCount == args.size }
            m.isAccessible = true
            m.invoke(act, *args)
        }
        fun shot(name: String) {
            shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(2))
            val v = act.window.decorView
            val bmp = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
            v.draw(Canvas(bmp))
            File(out, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        shot("0_first_launch")
        call("showMenu"); shot("1_menu")
        call("showLevels", Difficulty.KID); shot("2_levels")
        call("startLevel", Difficulty.EXPERT, 5); shot("3_game")
        call("hint"); shot("4_hint")
        val lv = Levels.get(Difficulty.KID, 3)
        call("startLevel", Difficulty.KID, 3)
        Puzzle.solve(lv.start, 3)!!.forEach { call("onTap", it) }
        shot("5_win")
    }
}
