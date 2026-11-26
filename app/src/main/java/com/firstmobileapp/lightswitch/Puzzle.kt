package com.firstmobileapp.lightswitch

import kotlin.math.abs
import kotlin.random.Random

/** Core rules: tapping a lamp switches it and its 4 neighbours. Goal: every lamp ON. */
object Puzzle {

    /** Bit mask of the cells switched when cell [i] is tapped on an [n]×[n] board. */
    fun toggleMask(n: Int, i: Int): Long {
        val r = i / n
        val c = i % n
        var m = 1L shl i
        if (r > 0) m = m or (1L shl (i - n))
        if (r < n - 1) m = m or (1L shl (i + n))
        if (c > 0) m = m or (1L shl (i - 1))
        if (c < n - 1) m = m or (1L shl (i + 1))
        return m
    }

    fun press(cells: BooleanArray, n: Int, i: Int) {
        val m = toggleMask(n, i)
        for (j in cells.indices) if ((m shr j) and 1L == 1L) cells[j] = !cells[j]
    }

    fun isSolved(cells: BooleanArray) = cells.all { it }

    /**
     * Shortest list of taps that lights every lamp, or null if impossible.
     * Gaussian elimination over GF(2), then the smallest solution among the null space.
     */
    fun solve(cells: BooleanArray, n: Int): List<Int>? {
        val size = n * n
        val rows = LongArray(size) { toggleMask(n, it) } // symmetric: row i = taps affecting i
        val rhs = BooleanArray(size) { !cells[it] }
        val pivotCol = IntArray(size) { -1 }
        val isPivot = BooleanArray(size)
        var r = 0
        for (c in 0 until size) {
            if (r == size) break
            var p = -1
            for (k in r until size) if ((rows[k] shr c) and 1L == 1L) { p = k; break }
            if (p < 0) continue
            val tr = rows[p]; rows[p] = rows[r]; rows[r] = tr
            val tb = rhs[p]; rhs[p] = rhs[r]; rhs[r] = tb
            for (k in 0 until size) {
                if (k != r && (rows[k] shr c) and 1L == 1L) {
                    rows[k] = rows[k] xor rows[r]
                    rhs[k] = rhs[k] xor rhs[r]
                }
            }
            pivotCol[r] = c
            isPivot[c] = true
            r++
        }
        for (k in r until size) if (rhs[k]) return null

        var particular = 0L
        for (k in 0 until r) if (rhs[k]) particular = particular or (1L shl pivotCol[k])

        val basis = ArrayList<Long>()
        for (f in 0 until size) {
            if (isPivot[f]) continue
            var v = 1L shl f
            for (k in 0 until r) if ((rows[k] shr f) and 1L == 1L) v = v or (1L shl pivotCol[k])
            basis.add(v)
        }

        var best = particular
        for (mask in 1 until (1 shl basis.size)) {
            var v = particular
            for (b in basis.indices) if ((mask shr b) and 1 == 1) v = v xor basis[b]
            if (java.lang.Long.bitCount(v) < java.lang.Long.bitCount(best)) best = v
        }
        return (0 until size).filter { (best shr it) and 1L == 1L }
    }
}

enum class Difficulty(
    val size: Int,
    val label: String,
    val color: Int,
    val minMoves: Int,
    val maxMoves: Int,
) {
    KID(3, "Kid", 0xFF43C463.toInt(), 1, 6),
    NORMAL(4, "Normal", 0xFF3D8BFD.toInt(), 2, 7),
    EXPERT(5, "Expert", 0xFFFF8A3D.toInt(), 3, 12),
    MASTER(6, "Master", 0xFFD94BD9.toInt(), 4, 16),
}

class Level(val difficulty: Difficulty, val number: Int, val start: BooleanArray, val par: Int)

object Levels {
    const val COUNT = 30

    private val cache = HashMap<String, Level>()

    fun get(d: Difficulty, number: Int): Level =
        cache.getOrPut("${d.name}_$number") { generate(d, number) }

    /** 3 stars = shortest solution, 2 = a few extra taps, 1 = solved. A hint caps it at 2. */
    fun stars(moves: Int, par: Int, usedHint: Boolean): Int {
        var s = when {
            moves <= par -> 3
            moves <= par + maxOf(2, par / 2) -> 2
            else -> 1
        }
        if (usedHint) s = minOf(s, 2)
        return s
    }

    /** Levels are generated from a fixed seed, so they are the same on every phone. */
    private fun generate(d: Difficulty, number: Int): Level {
        val n = d.size
        val size = n * n
        val target = d.minMoves +
            ((d.maxMoves - d.minMoves) * (number - 1) + (COUNT - 1) / 2) / (COUNT - 1)
        val rnd = Random(d.ordinal * 100_003L + number * 7_919L + 2026L)
        var best: BooleanArray? = null
        var bestPar = -1
        repeat(400) {
            val cells = BooleanArray(size) { true }
            for (i in (0 until size).shuffled(rnd).take(target)) Puzzle.press(cells, n, i)
            val par = Puzzle.solve(cells, n)!!.size
            if (par == target) return Level(d, number, cells, par)
            if (par > 0 && (best == null || abs(par - target) < abs(bestPar - target))) {
                best = cells
                bestPar = par
            }
        }
        return Level(d, number, best!!, bestPar)
    }
}
