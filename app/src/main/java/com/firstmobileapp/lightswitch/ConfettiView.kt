package com.firstmobileapp.lightswitch

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.SystemClock
import android.view.View
import kotlin.random.Random

/** Celebration confetti drawn over the screen. Does not block touches. */
class ConfettiView(context: Context) : View(context) {

    private class P(
        var x: Float, var y: Float, var vx: Float, var vy: Float,
        var rot: Float, val vr: Float, val color: Int, val w: Float, val h: Float,
    )

    private val parts = ArrayList<P>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var last = 0L
    private val colors = intArrayOf(
        0xFFFFD43B.toInt(), 0xFF43C463.toInt(), 0xFF3D8BFD.toInt(),
        0xFFFF8A3D.toInt(), 0xFFD94BD9.toInt(), 0xFF50E6FF.toInt(), 0xFFFF5470.toInt(),
    )

    fun burst() {
        val d = resources.displayMetrics.density
        repeat(150) {
            parts.add(
                P(
                    x = width * (0.3f + 0.4f * Random.nextFloat()),
                    y = height * 0.45f,
                    vx = (Random.nextFloat() - 0.5f) * 900f * d,
                    vy = -(450f + Random.nextFloat() * 750f) * d,
                    rot = Random.nextFloat() * 360f,
                    vr = (Random.nextFloat() - 0.5f) * 720f,
                    color = colors[Random.nextInt(colors.size)],
                    w = (5f + Random.nextFloat() * 6f) * d,
                    h = (3f + Random.nextFloat() * 4f) * d,
                )
            )
        }
        last = 0L
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        if (parts.isEmpty()) return
        val now = SystemClock.uptimeMillis()
        val dt = if (last == 0L) 0.016f else ((now - last).coerceIn(0, 50)) / 1000f
        last = now
        val g = 1100f * resources.displayMetrics.density
        val it = parts.iterator()
        while (it.hasNext()) {
            val p = it.next()
            p.vy += g * dt
            p.vx *= 0.99f
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.rot += p.vr * dt
            if (p.y > height + 40) { it.remove(); continue }
            paint.color = p.color
            canvas.save()
            canvas.rotate(p.rot, p.x, p.y)
            canvas.drawRect(p.x - p.w / 2, p.y - p.h / 2, p.x + p.w / 2, p.y + p.h / 2, paint)
            canvas.restore()
        }
        if (parts.isNotEmpty()) postInvalidateOnAnimation()
    }
}
