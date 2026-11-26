package com.firstmobileapp.lightswitch

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Draws the grid of lamps and reports taps. */
class BoardView(context: Context) : View(context) {

    private var n = 3
    private var cells = BooleanArray(9)
    private var bright = FloatArray(9)
    private var pop = FloatArray(9)
    private var lastFrame = 0L

    var hintCell = -1
        set(v) { field = v; invalidate() }
    var inputEnabled = true
    var onCellTap: ((Int) -> Unit)? = null

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    /** [cells] is shared with the game and changed in place. */
    fun bind(n: Int, cells: BooleanArray) {
        this.n = n
        this.cells = cells
        bright = FloatArray(n * n) { if (cells[it]) 1f else 0f }
        pop = FloatArray(n * n)
        hintCell = -1
        inputEnabled = true
        invalidate()
    }

    fun pulse(i: Int) {
        val m = Puzzle.toggleMask(n, i)
        for (j in 0 until n * n) if ((m shr j) and 1L == 1L) pop[j] = 1f
        invalidate()
    }

    fun pulseAll() {
        pop.fill(1f)
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = MeasureSpec.getSize(heightMeasureSpec)
        val s = if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED) w else min(w, h)
        setMeasuredDimension(s, s)
    }

    override fun onDraw(canvas: Canvas) {
        val now = SystemClock.uptimeMillis()
        val dt = if (lastFrame == 0L) 16f else (now - lastFrame).coerceIn(0, 50).toFloat()
        lastFrame = now
        var animating = false
        val cs = width.toFloat() / n

        for (i in 0 until n * n) {
            val target = if (cells[i]) 1f else 0f
            if (bright[i] != target) {
                val step = dt / 140f
                bright[i] = if (bright[i] < target) min(target, bright[i] + step) else max(target, bright[i] - step)
                animating = true
            }
            if (pop[i] > 0f) {
                pop[i] = max(0f, pop[i] - dt / 260f)
                animating = true
            }

            val col = i % n
            val row = i / n
            val cx = col * cs + cs / 2
            val b = bright[i]

            // Tile
            val inset = cs * 0.06f
            rect.set(col * cs + inset, row * cs + inset, (col + 1) * cs - inset, (row + 1) * cs - inset)
            paint.shader = null
            paint.style = Paint.Style.FILL
            paint.color = blend(TILE_OFF, TILE_ON, b)
            canvas.drawRoundRect(rect, cs * 0.2f, cs * 0.2f, paint)

            val bounce = 1f + 0.18f * sin(pop[i] * PI).toFloat()
            val r = cs * 0.25f * bounce
            val by = row * cs + cs / 2 - cs * 0.06f

            // Glow
            if (b > 0.01f) {
                paint.shader = RadialGradient(
                    cx, by, r * 1.7f,
                    intArrayOf(Color.argb((170 * b).toInt(), 255, 214, 80), Color.argb(0, 255, 214, 80)),
                    null, Shader.TileMode.CLAMP
                )
                canvas.drawCircle(cx, by, r * 1.7f, paint)
                paint.shader = null
            }

            // Screw cap
            paint.color = blend(CAP_OFF, CAP_ON, b)
            rect.set(cx - r * 0.45f, by + r * 0.7f, cx + r * 0.45f, by + r * 1.32f)
            canvas.drawRoundRect(rect, r * 0.15f, r * 0.15f, paint)

            // Bulb
            paint.color = blend(BULB_OFF, BULB_ON, b)
            canvas.drawCircle(cx, by, r, paint)

            // Shine
            paint.color = Color.argb((45 + 130 * b).toInt(), 255, 255, 255)
            canvas.drawCircle(cx - r * 0.35f, by - r * 0.35f, r * 0.22f, paint)

            // Hint ring
            if (i == hintCell) {
                val t = (sin(now / 160.0) * 0.5 + 0.5).toFloat()
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = cs * 0.055f
                paint.color = Color.argb((110 + 145 * t).toInt(), 80, 230, 255)
                canvas.drawRoundRect(rect.apply {
                    set(col * cs + inset * 1.6f, row * cs + inset * 1.6f, (col + 1) * cs - inset * 1.6f, (row + 1) * cs - inset * 1.6f)
                }, cs * 0.18f, cs * 0.18f, paint)
                paint.style = Paint.Style.FILL
                animating = true
            }
        }

        if (animating) postInvalidateOnAnimation() else lastFrame = 0L
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.actionMasked == MotionEvent.ACTION_DOWN) {
            if (inputEnabled && width > 0) {
                val cs = width.toFloat() / n
                val c = (e.x / cs).toInt()
                val r = (e.y / cs).toInt()
                if (c in 0 until n && r in 0 until n) {
                    performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    onCellTap?.invoke(r * n + c)
                }
            }
            return true
        }
        if (e.actionMasked == MotionEvent.ACTION_UP) performClick()
        return true
    }

    override fun performClick(): Boolean = super.performClick()

    companion object {
        private val TILE_OFF = 0xFF262B52.toInt()
        private val TILE_ON = 0xFF3A3466.toInt()
        private val BULB_OFF = 0xFF4A5080.toInt()
        private val BULB_ON = 0xFFFFD43B.toInt()
        private val CAP_OFF = 0xFF5D6290.toInt()
        private val CAP_ON = 0xFFB9BCD0.toInt()

        fun blend(a: Int, b: Int, t: Float): Int {
            val u = t.coerceIn(0f, 1f)
            fun ch(shift: Int): Int {
                val x = (a shr shift) and 0xFF
                val y = (b shr shift) and 0xFF
                return (x + (y - x) * u).toInt()
            }
            return Color.argb(ch(24), ch(16), ch(8), ch(0))
        }
    }
}
