package com.firstmobileapp.lightswitch

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Build
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowInsets
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {

    private enum class Screen { MENU, LEVELS, GAME }

    private lateinit var root: FrameLayout
    private lateinit var progress: Progress
    private lateinit var sound: SoundFx
    private var screen = Screen.MENU
    private var difficulty = Difficulty.KID

    // Game state
    private var level: Level? = null
    private var cells = BooleanArray(0)
    private val history = ArrayList<Int>()
    private var usedHint = false
    private var solved = false
    private var board: BoardView? = null
    private var movesText: TextView? = null
    private var tipText: TextView? = null
    private var confetti: ConfettiView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        progress = Progress(this)
        sound = SoundFx().also { it.enabled = progress.soundOn }

        root = FrameLayout(this)
        root.background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(BG_TOP, BG_BOTTOM)
        )
        if (Build.VERSION.SDK_INT >= 35) {
            // Android 15+ draws the app behind the status/navigation bars; keep content clear of them.
            root.setOnApplyWindowInsetsListener { v, insets ->
                val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
                insets
            }
        } else {
            @Suppress("DEPRECATION")
            window.statusBarColor = BG_TOP
            @Suppress("DEPRECATION")
            window.navigationBarColor = BG_BOTTOM
        }
        setContentView(root)
        showMenu()
        if (!progress.seenTutorial) {
            progress.seenTutorial = true
            showHowTo()
        }
    }

    override fun onDestroy() {
        sound.release()
        super.onDestroy()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        when (screen) {
            Screen.MENU -> @Suppress("DEPRECATION") super.onBackPressed()
            Screen.LEVELS -> showMenu()
            Screen.GAME -> showLevels(difficulty)
        }
    }

    // ---------------------------------------------------------------- Menu

    private fun showMenu() {
        screen = Screen.MENU
        val col = vertical().apply {
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(32), dp(24), dp(24))
        }
        col.addView(text("💡", 64f), lp(WRAP_CONTENT, WRAP_CONTENT))
        col.addView(text("Light Switch", 40f, bold = true), lp(WRAP_CONTENT, WRAP_CONTENT))
        col.addView(text("Light up every lamp!", 18f, SUBTEXT), lp(WRAP_CONTENT, WRAP_CONTENT, bottom = 28))

        for (d in Difficulty.values()) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(22), dp(16), dp(22), dp(16))
                background = pressable(d.color, 22f)
                isClickable = true
                setOnClickListener { sound.play("click"); showLevels(d) }
            }
            val name = text(d.label, 24f, bold = true).apply { gravity = Gravity.START }
            val sizeLabel = text("   ${d.size}×${d.size}", 18f, 0xDDFFFFFF.toInt()).apply { gravity = Gravity.START }
            val stars = text("★ ${progress.totalStars(d)}/${Levels.COUNT * 3}", 17f, bold = true)
            row.addView(name)
            row.addView(sizeLabel, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
            row.addView(stars)
            col.addView(row, lp(MATCH_PARENT, WRAP_CONTENT, bottom = 14))
        }

        val bottom = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        bottom.addView(
            smallButton("How to play") { showHowTo() },
            LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { rightMargin = dp(7) }
        )
        lateinit var soundBtn: TextView
        soundBtn = smallButton(soundLabel()) {
            progress.soundOn = !progress.soundOn
            sound.enabled = progress.soundOn
            soundBtn.text = soundLabel()
        }
        bottom.addView(soundBtn, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { leftMargin = dp(7) })
        col.addView(bottom, lp(MATCH_PARENT, WRAP_CONTENT, top = 14))

        setScreen(ScrollView(this).apply { addView(col) })
    }

    private fun soundLabel() = if (progress.soundOn) "🔊 Sound on" else "🔇 Sound off"

    private fun showHowTo() {
        AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle("How to play")
            .setMessage(
                "Tap a lamp to switch it on or off.\n\n" +
                    "But watch out: the lamps above, below, left and right of it switch too!\n\n" +
                    "Goal: light up EVERY lamp 💡\n\n" +
                    "Finish in the fewest moves to earn ★★★.\n" +
                    "Stuck? Tap Hint, or Undo a move."
            )
            .setPositiveButton("Let's play!", null)
            .show()
    }

    // ---------------------------------------------------------------- Level select

    private fun showLevels(d: Difficulty) {
        screen = Screen.LEVELS
        difficulty = d
        val col = vertical().apply { setPadding(dp(16), dp(12), dp(16), dp(24)) }
        col.addView(header("${d.label}  ·  ${d.size}×${d.size}", "★ ${progress.totalStars(d)}/${Levels.COUNT * 3}") { showMenu() })

        val grid = GridLayout(this).apply { columnCount = 5 }
        for (i in 1..Levels.COUNT) {
            val unlocked = progress.isUnlocked(d, i)
            val stars = progress.stars(d, i)
            val tile = vertical().apply {
                gravity = Gravity.CENTER
                background = if (unlocked) pressable(d.color, 16f) else rounded(LOCKED, 16f)
                setPadding(0, dp(10), 0, dp(10))
                if (unlocked) {
                    isClickable = true
                    setOnClickListener { sound.play("click"); startLevel(d, i) }
                }
            }
            tile.addView(text(if (unlocked) "$i" else "🔒", 22f, if (unlocked) Color.WHITE else SUBTEXT, bold = true))
            tile.addView(text(starString(stars, if (unlocked) 0x66FFFFFF else 0x00000000), 14f))
            grid.addView(tile, GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED), GridLayout.spec(GridLayout.UNDEFINED, 1f)
            ).apply {
                width = 0
                setMargins(dp(5), dp(5), dp(5), dp(5))
            })
        }
        col.addView(grid, lp(MATCH_PARENT, WRAP_CONTENT, top = 8))
        setScreen(ScrollView(this).apply { addView(col) })
    }

    // ---------------------------------------------------------------- Game

    private fun startLevel(d: Difficulty, number: Int) {
        screen = Screen.GAME
        difficulty = d
        val lv = Levels.get(d, number)
        level = lv
        cells = lv.start.copyOf()
        history.clear()
        usedHint = false
        solved = false

        val frame = FrameLayout(this)
        val col = vertical().apply { setPadding(dp(16), dp(12), dp(16), dp(16)) }
        col.addView(header("Level $number", d.label) { showLevels(d) })

        movesText = text("", 18f, SUBTEXT, bold = true)
        col.addView(movesText, lp(MATCH_PARENT, WRAP_CONTENT, top = 6))

        val boardHolder = FrameLayout(this)
        val b = BoardView(this).apply {
            bind(d.size, cells)
            onCellTap = { onTap(it) }
        }
        board = b
        boardHolder.addView(b, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT, Gravity.CENTER))
        col.addView(boardHolder, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f).apply { topMargin = dp(12) })

        tipText = text("Tap a lamp. Its neighbours switch too!", 16f, SUBTEXT)
        col.addView(tipText, lp(MATCH_PARENT, WRAP_CONTENT, top = 10, bottom = 12))

        val buttons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val bp = { LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { setMargins(dp(5), 0, dp(5), 0) } }
        buttons.addView(smallButton("↩ Undo") { undo() }, bp())
        buttons.addView(smallButton("⟳ Restart") { restart() }, bp())
        buttons.addView(smallButton("💡 Hint", HINT) { hint() }, bp())
        col.addView(buttons, lp(MATCH_PARENT, WRAP_CONTENT))

        frame.addView(col, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        confetti = ConfettiView(this)
        frame.addView(confetti, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        setScreen(frame)
        updateMoves()
    }

    private fun onTap(i: Int) {
        val lv = level ?: return
        if (solved) return
        Puzzle.press(cells, lv.difficulty.size, i)
        history.add(i)
        board?.hintCell = -1
        board?.pulse(i)
        sound.play(if (cells[i]) "on" else "off")
        updateMoves()
        if (Puzzle.isSolved(cells)) win()
    }

    private fun undo() {
        val lv = level ?: return
        if (solved || history.isEmpty()) return
        val i = history.removeAt(history.lastIndex)
        Puzzle.press(cells, lv.difficulty.size, i)
        board?.hintCell = -1
        board?.pulse(i)
        sound.play("off")
        updateMoves()
    }

    private fun restart() {
        val lv = level ?: return
        if (solved) return
        System.arraycopy(lv.start, 0, cells, 0, cells.size)
        history.clear()
        board?.hintCell = -1
        board?.pulseAll()
        updateMoves()
    }

    private fun hint() {
        val lv = level ?: return
        if (solved) return
        val sol = Puzzle.solve(cells, lv.difficulty.size) ?: return
        if (sol.isEmpty()) return
        usedHint = true
        board?.hintCell = sol.first()
        sound.play("hint")
        tipText?.text = "Tap the lamp with the blue ring!"
    }

    private fun updateMoves() {
        val lv = level ?: return
        movesText?.text = "Moves: ${history.size}      Target: ${lv.par}"
    }

    private fun win() {
        val lv = level ?: return
        solved = true
        board?.inputEnabled = false
        val stars = Levels.stars(history.size, lv.par, usedHint)
        progress.record(lv.difficulty, lv.number, stars)
        sound.play("win")
        confetti?.burst()
        root.postDelayed({
            if (screen == Screen.GAME && level === lv && solved) showWinPanel(lv, stars)
        }, 750)
    }

    private fun showWinPanel(lv: Level, stars: Int) {
        val frame = root.getChildAt(0) as? FrameLayout ?: return
        val dim = FrameLayout(this).apply {
            setBackgroundColor(0xAA0B0D20.toInt())
            isClickable = true
        }
        val card = vertical().apply {
            gravity = Gravity.CENTER_HORIZONTAL
            background = rounded(CARD, 28f)
            setPadding(dp(24), dp(24), dp(24), dp(20))
        }
        val title = when (stars) {
            3 -> "Perfect! 🌟"
            2 -> "Great job! 🎉"
            else -> "You did it! 👏"
        }
        card.addView(text(title, 30f, bold = true))
        card.addView(text(starString(stars, 0x55FFFFFF), 52f), lp(WRAP_CONTENT, WRAP_CONTENT, top = 4))
        val moves = history.size
        val word = if (moves == 1) "move" else "moves"
        val detail = if (moves <= lv.par) "Solved in $moves $word, the best possible!"
        else "Solved in $moves $word.\nCan you do it in ${lv.par}?"
        card.addView(text(detail, 17f, SUBTEXT), lp(WRAP_CONTENT, WRAP_CONTENT, top = 4, bottom = 18))

        val d = lv.difficulty
        if (lv.number < Levels.COUNT) {
            card.addView(bigButton("Next level ›", d.color) { startLevel(d, lv.number + 1) }, lp(MATCH_PARENT, WRAP_CONTENT, bottom = 10))
        } else {
            card.addView(text("You finished all ${d.label} levels! 🏆", 18f, GOLD, bold = true), lp(WRAP_CONTENT, WRAP_CONTENT, bottom = 12))
        }
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(smallButton("⟳ Again", LOCKED) { startLevel(d, lv.number) },
            LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { rightMargin = dp(5) })
        row.addView(smallButton("Levels", LOCKED) { showLevels(d) },
            LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { leftMargin = dp(5) })
        card.addView(row, lp(MATCH_PARENT, WRAP_CONTENT))

        dim.addView(card, FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT, Gravity.CENTER).apply {
            setMargins(dp(28), 0, dp(28), 0)
        })
        frame.addView(dim, frame.childCount - 1) // keep confetti on top
        card.scaleX = 0.7f; card.scaleY = 0.7f; card.alpha = 0f
        card.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(280).start()
    }

    // ---------------------------------------------------------------- UI helpers

    private fun setScreen(v: View) {
        root.removeAllViews()
        root.addView(v, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
    }

    private fun dp(v: Number) = (v.toFloat() * resources.displayMetrics.density + 0.5f).toInt()

    private fun vertical() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

    private fun lp(w: Int, h: Int, top: Int = 0, bottom: Int = 0) =
        LinearLayout.LayoutParams(w, h).apply { topMargin = dp(top); bottomMargin = dp(bottom) }

    private fun text(s: CharSequence, sp: Float, color: Int = Color.WHITE, bold: Boolean = false) =
        TextView(this).apply {
            text = s
            textSize = sp
            setTextColor(color)
            gravity = Gravity.CENTER
            if (bold) typeface = Typeface.DEFAULT_BOLD
        }

    private fun starString(stars: Int, emptyColor: Int): CharSequence {
        val sb = SpannableStringBuilder("★★★")
        sb.setSpan(ForegroundColorSpan(GOLD), 0, stars, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        sb.setSpan(ForegroundColorSpan(emptyColor), stars, 3, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        return sb
    }

    private fun rounded(color: Int, radiusDp: Float) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radiusDp).toFloat()
    }

    private fun pressable(color: Int, radiusDp: Float) = StateListDrawable().apply {
        addState(intArrayOf(android.R.attr.state_pressed), rounded(BoardView.blend(color, Color.BLACK, 0.25f), radiusDp))
        addState(intArrayOf(), rounded(color, radiusDp))
    }

    private fun bigButton(label: String, color: Int, onClick: () -> Unit) =
        text(label, 22f, bold = true).apply {
            background = pressable(color, 20f)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            isClickable = true
            setOnClickListener { sound.play("click"); onClick() }
        }

    private fun smallButton(label: String, color: Int = CARD, onClick: () -> Unit) =
        text(label, 16f, bold = true).apply {
            background = pressable(color, 16f)
            setPadding(dp(8), dp(13), dp(8), dp(13))
            isClickable = true
            setOnClickListener { sound.play("click"); onClick() }
        }

    private fun header(title: String, right: CharSequence, onBack: () -> Unit): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        row.addView(text("‹", 34f, bold = true).apply {
            background = pressable(CARD, 14f)
            setPadding(dp(16), 0, dp(16), dp(4))
            isClickable = true
            setOnClickListener { sound.play("click"); onBack() }
        }, LinearLayout.LayoutParams(WRAP_CONTENT, dp(48)))
        row.addView(text(title, 24f, bold = true), LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        row.addView(text(right, 17f, SUBTEXT, bold = true).apply { minWidth = dp(64) })
        return row
    }

    companion object {
        private val BG_TOP = 0xFF1B1F3B.toInt()
        private val BG_BOTTOM = 0xFF2D1B4E.toInt()
        private val CARD = 0xFF30366A.toInt()
        private val LOCKED = 0xFF22264A.toInt()
        private val HINT = 0xFF1E8FA8.toInt()
        private val SUBTEXT = 0xFFB8C0FF.toInt()
        private val GOLD = 0xFFFFD43B.toInt()
    }
}
