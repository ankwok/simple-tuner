package com.akwok.simpletuner

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.akwok.simpletuner.models.Accuracy
import com.akwok.simpletuner.tuner.PitchDetector
import com.akwok.simpletuner.tuner.PitchHelper
import com.akwok.simpletuner.views.SettingsFragment

class PreferencesService(private val ctx: Context) {

    fun isDarkMode(): Boolean = getPrefs().getBoolean(ctx.getString(R.string.dark_mode_pref), false)

    fun shouldShowErr(): Boolean =
        getPrefs().getBoolean(ctx.getString(R.string.error_text_pref), errorTextDefault)

    fun getDetectionThreshold(): Double {
        val threshold =
            getPrefs().getInt(ctx.getString(R.string.noise_rejection_pref), -1).toDouble()
        val stepSize = PitchDetector.maxDetectionThreshold / SettingsFragment.noiseRejectionMaxValue

        return if (threshold > 0) threshold * stepSize
        else PitchDetector.defaultDetectionThreshold
    }

    fun getNoteConvention(): String {
        val default = ctx.getString(R.string.note_name_default)
        return getStringPref(getPrefs(), ctx.getString(R.string.note_name_pref), default)
    }

    fun getReferenceFreq(): Int {
        val prefStr = getStringPref(
            getPrefs(),
            ctx.getString(R.string.ref_A_pref),
            PitchHelper.defaultReference.toString()
        )

        return prefStr.toIntOrNull() ?: PitchHelper.defaultReference
    }

    fun setReferenceFreq(freq: Int) {
        val editor = getPrefs().edit()
        editor.putString(ctx.getString(R.string.ref_A_pref), freq.toString())
        editor.apply()
    }

    fun getAccuracy(): Accuracy {
        val accuracyStr = getStringPref(getPrefs(), ctx.getString(R.string.accuracy_pref), ctx.getString(R.string.accuracy_default))
        return when(accuracyStr) {
            "low" -> Accuracy.LOW
            "med" -> Accuracy.MEDIUM
            "high" -> Accuracy.HIGH
            else -> throw Exception("Unknown accuracy value: $accuracyStr")
        }
    }

    fun getAccuracyString(): String {
        val accuracyStr = getStringPref(getPrefs(), ctx.getString(R.string.accuracy_pref), ctx.getString(R.string.accuracy_default))
        return getAccuracyString(accuracyStr)
    }

    fun getAccuracyString(value: String): String {
        val accuracyValues = ctx.resources.getStringArray(R.array.accuracy_values)
        val accuracyEntries = ctx.resources.getStringArray(R.array.accuracy_entries)
        return accuracyValues.zip(accuracyEntries)
            .first { pair -> pair.first == value }
            .second
    }

    private fun getPrefs(): SharedPreferences = PreferenceManager.getDefaultSharedPreferences(ctx)

    companion object {
        const val errorTextDefault = true

        fun getStringPref(prefs: SharedPreferences, key: String, default: String): String {
            val pref = prefs.getString(key, default)
            return pref ?: default
        }
    }
}