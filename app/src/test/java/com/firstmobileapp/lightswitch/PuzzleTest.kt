package com.firstmobileapp.lightswitch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PuzzleTest {

    @Test
    fun everyLevelIsSolvableInParMoves() {
        for (d in Difficulty.values()) {
            val pars = ArrayList<Int>()
            for (i in 1..Levels.COUNT) {
                val lv = Levels.get(d, i)
                assertFalse("${d.name} $i starts solved", Puzzle.isSolved(lv.start))
                val sol = Puzzle.solve(lv.start, d.size)
                assertNotNull(sol)
                assertEquals(lv.par, sol!!.size)
                val cells = lv.start.copyOf()
                sol.forEach { Puzzle.press(cells, d.size, it) }
                assertTrue("${d.name} $i solution works", Puzzle.isSolved(cells))
                pars.add(lv.par)
            }
            println("${d.name}: ${pars.joinToString()}")
        }
    }

    @Test
    fun levelsAreStable() {
        val a = Levels.get(Difficulty.EXPERT, 7).start.toList()
        assertEquals(a, Levels.get(Difficulty.EXPERT, 7).start.toList())
    }

    @Test
    fun starsRule() {
        assertEquals(3, Levels.stars(4, 4, false))
        assertEquals(2, Levels.stars(4, 4, true))
        assertEquals(2, Levels.stars(6, 4, false))
        assertEquals(1, Levels.stars(20, 4, false))
    }
}
