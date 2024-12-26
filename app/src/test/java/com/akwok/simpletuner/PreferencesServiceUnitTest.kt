package com.akwok.simpletuner

import android.content.Context
import android.content.SharedPreferences
import com.akwok.simpletuner.models.Accuracy
import com.akwok.simpletuner.tuner.PitchDetector
import com.akwok.simpletuner.views.SettingsFragment
import org.junit.Assert
import org.junit.Test
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class PreferencesServiceUnitTest {
    @Test
    fun getStringPref() {
        val testCases = listOf(
            Triple("val", null, "val"),
            Triple("val", "foo", "foo"))

        testCases.forEach { case ->
            val defaultVal = case.first
            val retVal = case.second
            val expected = case.third

            val mockPrefs = mock<SharedPreferences>()
            whenever(mockPrefs.getString("key", defaultVal)).thenReturn(retVal)

            val actual = PreferencesService.getStringPref(mockPrefs, "key", defaultVal)
            Assert.assertEquals(expected, actual)
        }
    }

    @Test
    fun isDarkMode() {
        val cases = listOf(true, false)

        cases.forEach { expected ->
            val ctxMock = mockContext(R.string.dark_mode_pref, expected)
            val prefService = PreferencesService(ctxMock)

            val actual = prefService.isDarkMode()
            Assert.assertEquals(expected, actual)
        }
    }

    @Test
    fun shouldShowError() {
        val cases = listOf(true, false)

        cases.forEach { expected ->
            val ctxMock = mockContext(R.string.error_text_pref, expected)
            val prefService = PreferencesService(ctxMock)

            val actual = prefService.shouldShowErr()
            Assert.assertEquals(expected, actual)
        }
    }

    @Test
    fun getDetectionThreshold() {
        val cases = listOf(
            arrayOf(-1, PitchDetector.defaultDetectionThreshold),
            arrayOf(0, PitchDetector.defaultDetectionThreshold),
            arrayOf(SettingsFragment.noiseRejectionMaxValue, PitchDetector.maxDetectionThreshold),
            arrayOf(1, PitchDetector.maxDetectionThreshold / SettingsFragment.noiseRejectionMaxValue),
            arrayOf(3, 3 * PitchDetector.maxDetectionThreshold / SettingsFragment.noiseRejectionMaxValue),
        )

        cases.forEach { case ->
            val retVal = case[0] as Int
            val expected = case[1] as Double

            val ctxMock = mockContext(R.string.noise_rejection_pref, retVal)

            val prefs = PreferencesService(ctxMock)
            val actual = prefs.getDetectionThreshold()

            Assert.assertEquals(expected, actual, 1e-4)
        }
    }

    @Test
    fun getNoteConvention() {
        val cases = listOf(
            listOf("english", "english"),
            listOf("solfege", "solfege"),
            listOf(null, "english")
        )

        cases.forEach { case ->
            val retVal = case[0]
            val expected = case[1]

            val ctxMock = mockContext(R.string.note_name_pref, retVal)
            whenever(ctxMock.getString(R.string.note_name_default)).thenReturn("english")

            val prefs = PreferencesService(ctxMock)
            val actual = prefs.getNoteConvention()

            Assert.assertEquals(expected, actual)
        }
    }

    @Test
    fun getReferenceFreq() {
        val cases = listOf(
            listOf("440", 440),
            listOf(null, 440),
            listOf("442", 442)
        )

        cases.forEach { case ->
            val retVal = case[0] as String?
            val expected = case[1] as Int

            val ctxMock = mockContext(R.string.ref_A_pref, retVal)

            val prefs = PreferencesService(ctxMock)
            val actual = prefs.getReferenceFreq()

            Assert.assertEquals(expected, actual)
        }
    }

    @Test
    fun getAccuracy() {
        val cases = listOf(
            Pair("low", Accuracy.LOW),
            Pair("med", Accuracy.MEDIUM),
            Pair("high", Accuracy.HIGH)
        )

        cases.forEach { case ->
            val retVal = case.first
            val expected = case.second

            val ctxMock = mockContext(R.string.accuracy_pref, retVal)
            whenever(ctxMock.getString(R.string.accuracy_default)).thenReturn("med")

            val prefs = PreferencesService(ctxMock)
            val actual = prefs.getAccuracy()

            Assert.assertEquals(expected, actual)
        }
    }

    private inline fun <reified T: Any> mockContext(rVal: Int, retval: T?): Context {
        val key = "key"

        val prefsMock = mock<SharedPreferences>()
        when (T::class) {
            Boolean::class ->
                whenever(prefsMock.getBoolean(eq(key), anyBoolean())).thenReturn(retval as Boolean)
            Int::class ->
                whenever(prefsMock.getInt(eq(key), anyInt())).thenReturn(retval as Int)
            String::class ->
                whenever(prefsMock.getString(eq(key), anyString())).thenReturn(retval as String?)
        }

        val ctxMock = mock<Context>()
        whenever(ctxMock.getString(rVal)).thenReturn(key)
        whenever(ctxMock.getSharedPreferences(anyString(), anyInt())).thenReturn(prefsMock)

        return ctxMock
    }
}