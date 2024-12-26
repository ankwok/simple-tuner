package com.akwok.simpletuner.views

import android.os.Bundle
import android.text.InputType
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.*
import com.akwok.simpletuner.PreferencesService
import com.akwok.simpletuner.R

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        val preferencesService = PreferencesService(requireContext())

        val refAPref = findPreference<EditTextPreference>(getString(R.string.ref_A_pref))!!
        refAPref.summaryProvider = Preference.SummaryProvider<EditTextPreference> {
            val refFreq = preferencesService.getReferenceFreq()
            "$refFreq Hz"
        }
        refAPref.dialogTitle = getString(R.string.reference_A_dialog_title).format(minRefFreq, maxRefFreq)
        refAPref.text = preferencesService.getReferenceFreq().toString()
        refAPref.setOnBindEditTextListener { et ->
            et.inputType = InputType.TYPE_CLASS_NUMBER
        }
        refAPref.setOnPreferenceChangeListener { preference, newValue ->
            val intVal = newValue.toString().toIntOrNull() ?: -1
            intVal in minRefFreq..maxRefFreq
        }

        val noiseRejection =
            findPreference<SeekBarPreference>(getString(R.string.noise_rejection_pref))!!
        noiseRejection.max = noiseRejectionMaxValue

        val darkPref = findPreference<SwitchPreferenceCompat>(getString(R.string.dark_mode_pref))!!
        darkPref.setOnPreferenceChangeListener { _, newValue ->
            val isDark = newValue as Boolean
            if (isDark) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            true
        }

        val accuracyPref = findPreference<ListPreference>(getString(R.string.accuracy_pref))!!
        val accuracySetting = preferencesService.getAccuracyString()
        accuracyPref.summary = getString(R.string.accuracy_summary, accuracySetting)
        accuracyPref.setOnPreferenceChangeListener { _, newValue ->
            val setting = preferencesService.getAccuracyString(newValue as String)
            accuracyPref.summary = getString(R.string.accuracy_summary, setting)
            true
        }
    }

    companion object {
        const val noiseRejectionMaxValue = 20
        const val minRefFreq = 400
        const val maxRefFreq = 500
    }
}