package com.akwok.simpletuner.tuner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PitchHelperUnitTest {

    @Test
    fun referencePitchInPitches() {
        val pitches = PitchHelper.getFrequencies(440.0)
        assertTrue(pitches.contains(Pitch(PitchName.A, 4, 440.0)))
    }

    @Test
    fun pitchesAreOrdered() {
        val pitches = PitchHelper.getFrequencies(440.0)
        assertEquals(pitches.sortedBy { p -> p.freq }, pitches)
    }

    @Test
    fun containsOnlyPianoNotes() {
        val pitches = PitchHelper.getFrequencies(440.0)

        assertEquals(88, pitches.size)

        val first = pitches.first()
        assertEquals(PitchName.A, first.pitch)
        assertEquals(0, first.octave)
        assertEquals(440.0 / 16, first.freq, 1e-2)

        val last = pitches.last()
        assertEquals(PitchName.C, last.pitch)
        assertEquals(8, last.octave)
        assertEquals(4186.0, last.freq, 1e-2)
    }
}