package com.firstmobileapp.lightswitch

import android.content.Context

/** Saved stars and settings (stored on the phone only). */
class Progress(context: Context) {
    private val prefs = context.getSharedPreferences("progress", Context.MODE_PRIVATE)

    private fun key(d: Difficulty, n: Int) = "s_${d.name}_$n"

    fun stars(d: Difficulty, n: Int) = prefs.getInt(key(d, n), 0)

    fun record(d: Difficulty, n: Int, stars: Int) {
        if (stars > stars(d, n)) prefs.edit().putInt(key(d, n), stars).apply()
    }

    fun totalStars(d: Difficulty) = (1..Levels.COUNT).sumOf { stars(d, it) }

    fun isUnlocked(d: Difficulty, n: Int) = n == 1 || stars(d, n - 1) > 0

    var soundOn: Boolean
        get() = prefs.getBoolean("sound", true)
        set(v) = prefs.edit().putBoolean("sound", v).apply()

    var seenTutorial: Boolean
        get() = prefs.getBoolean("tutorial", false)
        set(v) = prefs.edit().putBoolean("tutorial", v).apply()
}
