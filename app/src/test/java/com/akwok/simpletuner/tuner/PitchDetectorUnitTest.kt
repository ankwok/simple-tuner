package com.akwok.simpletuner.tuner

import com.akwok.simpletuner.io.AudioData
import org.junit.Assert
import org.junit.Test
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

class PitchDetectorUnitTest {

    private val sampleRate = 44100
    private val pitches = PitchHelper.getFrequencies(440.0)

    private val findClosestPitchCases = listOf(
        arrayOf(439.0, Pitch(PitchName.A, 4, 440.0)),
        arrayOf(440.0, Pitch(PitchName.A, 4, 440.0)),
        arrayOf(441.0, Pitch(PitchName.A, 4, 440.0)),
        arrayOf(20000.0, pitches.last()),
        arrayOf(0.0, pitches.first()),
        arrayOf(0.5 * (pitches[10].freq + pitches[11].freq), pitches[11])   // Round up
    )

    @Test
    fun findClosestPitch() {
        val detector = PitchDetector(440.0, 1)

        findClosestPitchCases
            .forEach { case ->
                val freq = case[0] as Double
                val expected = case[1] as Pitch
                val actual = detector.findClosestPitch(freq)
                Assert.assertEquals("Test case ($freq, $expected)", expected, actual)
            }
    }

    private val detectCases = listOf(
        arrayOf(439.0, Pitch(PitchName.A, 4, 440.0)),
        arrayOf(440.0, Pitch(PitchName.A, 4, 440.0)),
        arrayOf(441.0, Pitch(PitchName.A, 4, 440.0)),
        arrayOf(10.0, null),
        arrayOf(20000.0, null)
    )

    private fun sineWave(freq: Double, offset: Double) = (0 until 4096)
        .map { i -> (sin(2 * PI * freq * i / sampleRate) + offset).toFloat() }
        .toFloatArray()

    private fun runDetection(sineGenerator: (Double) -> FloatArray) = detectCases
        .forEach { case ->
            val freq = case[0] as Double
            val expectedPitch = case[1] as Pitch?

            val audio = sineGenerator(freq)
            val detector = PitchDetector(440.0, audio.size)
            val pitchError = detector.detect(AudioData(audio, sampleRate))

            Assert.assertEquals(
                "Test case ($freq, $expectedPitch)",
                expectedPitch,
                pitchError?.expected
            )

            if (pitchError != null) {
                Assert.assertEquals(
                    "Test case ($freq, $expectedPitch)",
                    freq,
                    pitchError.actualFreq,
                    1e-3
                )
            }
        }

    @Test
    fun detectBasicSine() {
        val sineGenerator = { freq: Double -> sineWave(freq, 0.0) }
        runDetection(sineGenerator = sineGenerator)
    }

    @Test
    fun detectOffsetSine() {
        val sineGenerator = { freq: Double -> sineWave(freq, 0.2) }
        runDetection(sineGenerator = sineGenerator)
    }

    @Test
    fun `returns null if input too soft`() {
        val tooQuiet = sineWave(440.0, 0.0)
            .map { x -> 0.01f * x }
            .toFloatArray()
        val audioData = AudioData(tooQuiet, sampleRate)
        val detector = PitchDetector(440.0, audioData.dat.size)

        val err = detector.detect(audioData)

        Assert.assertNull(err)
    }

    @Test
    fun detectVoice() {
        val cases = listOf(
            "voice_96.24.csv" to 96.24,
            "voice_132.5.csv" to 132.5,
            "voice_144.0.csv" to 144.0,
            "voice_218.4.csv" to 218.4)

        cases.forEach { pair -> testDetect(pair.first, pair.second) }
    }

    @Test
    fun detectCello() {
        val cases = listOf(
            "cello_65.89.csv" to 65.89,
            "cello_98.73.csv" to 98.73,
            "cello_148.3.csv" to 148.3,
            "cello_222.1.csv" to 222.1)

        cases.forEach { pair -> testDetect(pair.first, pair.second) }
    }

    @Test
    fun `doesn't crash with bad input`() {
        val testCases = listOf(
            (0 until 4096).map { 1.42f }.toFloatArray(), // constant signal
        )

        testCases.forEach { case ->
            val audioData = AudioData(case, sampleRate)
            val detector = PitchDetector(440.0, audioData.dat.size)

            val err = detector.detect(audioData)

            Assert.assertNull(err)
        }

    }

    private fun testDetect(file: String, expectedFreq: Double) {
        val str = javaClass.getResourceAsStream(file)

        val reader = BufferedReader(InputStreamReader(str))
        val wav = reader
            .lineSequence()
            .map { line -> 10 * line.toFloat() } // Multiply by 10 to pass the silence threshold
            .toList()
            .toFloatArray()

        val detector = PitchDetector(440.0, wav.size)
        val err = detector.detect(AudioData(wav, sampleRate))
        Assert.assertEquals(expectedFreq, err!!.actualFreq, PitchHelper.centRatio.pow(10) * expectedFreq)
    }
}