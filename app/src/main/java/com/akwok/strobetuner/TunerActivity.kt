package com.akwok.strobetuner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.NumberPicker
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.preference.PreferenceManager
import com.akwok.strobetuner.models.TunerModel
import com.akwok.strobetuner.tuner.PitchDetector
import com.akwok.strobetuner.tuner.PitchError
import com.akwok.strobetuner.tuner.PitchHelper
import com.akwok.strobetuner.views.SettingsFragment
import com.akwok.strobetuner.views.StrobeView
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.math.roundToInt

class TunerActivity : AppCompatActivity() {
    private val REQUEST_MIC: Int = 0

    private var clickCount = 0
    private var clickStart = 0L
    private val clickStartTtl = 5L
    private val clicksToSample = 6

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tuner)

        getMicPermission()
        setupTextUpdater()
        setupRefPicker()
        setupStrobe()
    }

    override fun onResume() {
        super.onResume()

        if (!hasMicPermission()) {
            recreate() // TODO: This is bad from a UX point of view.
        }

        setupRefPicker()

        val model: TunerModel by viewModels()
        model.startRecording()

        setupThreshold()

        val strobe = findViewById<StrobeView>(R.id.strobe_view)
        strobe.isRunning = true
    }

    private fun setupThreshold() {
        val model: TunerModel by viewModels()
        val prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val savedThreshold = prefs
            .getInt(getString(R.string.noise_rejection_pref), -1)
            .toDouble()
        val convertedThreshold =
            if (savedThreshold > 0) savedThreshold * PitchDetector.maxDetectionThreshold / SettingsFragment.noiseRejectionMaxValue
            else PitchDetector.defaultDetectionThreshold
        model.detectionThreshold.postValue(convertedThreshold)
    }

    override fun onPause() {
        super.onPause()

        val model: TunerModel by viewModels()
        model.stopRecording()

        val strobe = findViewById<StrobeView>(R.id.strobe_view)
        strobe.isRunning = false
    }

    fun onTunerClick(view: View) {
        val now = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
        if (now - clickStart > clickStartTtl) {
            clickStart = now
            clickCount = 1
        } else {
            clickCount++
        }

        if (clickCount >= clicksToSample) {
            gotoSample()
        }
    }

    fun onSettingsClick(view: View) = SettingsActivity.gotoSettings(this)

    private fun setupTextUpdater() {
        val obs = Observer<PitchError?> { err -> if (err != null) textUpdater(err) }
        val model: TunerModel by viewModels()
        model.pitchError.observe(this, obs)

        val verticalBias =
            if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) 0.35f
            else 0.15f
        val noteName = findViewById<TextView>(R.id.note_name)
        val params = noteName.layoutParams as ConstraintLayout.LayoutParams
        params.verticalBias = verticalBias
        noteName.layoutParams = params
    }

    private fun textUpdater(pitchError: PitchError) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val noteName =
            when (prefs.getString(getString(R.string.note_name_pref), getString(R.string.note_name_default))) {
                "solfege" -> pitchError.expected.pitch.solfegeName()
                else -> pitchError.expected.pitch.englishName()
            }
        val noteView = findViewById<TextView>(R.id.note_name)
        noteView.text = getString(R.string.note_name,
            noteName, pitchError.expected.octave)

        val freq = findViewById<TextView>(R.id.frequency)
        freq.text = getString(R.string.note_freq, pitchError.actualFreq)

        val centsErr = findViewById<TextView>(R.id.cents_error)
        centsErr.text = getString(R.string.cents_err, pitchError.errorInCents.roundToInt())

        val strobe = findViewById<StrobeView>(R.id.strobe_view)
        strobe.numBands = 2 * (pitchError.expected.octave + 1)
    }

    private fun setupRefPicker() {
        val picker = findViewById<NumberPicker>(R.id.ref_picker)
        picker.minValue = 400
        picker.maxValue = 500

        val model: TunerModel by viewModels()

        val prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val savedRef = prefs
            .getString(getString(R.string.ref_A_pref), PitchHelper.defaultReference.toString())
            ?.toIntOrNull()
        picker.value = savedRef!!
        model.referenceA.postValue(savedRef)

        picker.setOnValueChangedListener { _, _, newVal ->
            model.referenceA.postValue(newVal)

            val editor = prefs.edit()
            editor.putString(getString(R.string.ref_A_pref), newVal.toString())
            editor.apply()
        }
    }

    private fun setupStrobe() {
        val strobe = findViewById<StrobeView>(R.id.strobe_view)

        val obs = Observer<PitchError?> { err ->
            strobe.errorInCents = err?.errorInCents?.toFloat() ?: 0f
        }

        val model: TunerModel by viewModels()
        model.pitchError.observe(this, obs)
    }

    private fun getMicPermission() {
        if (!hasMicPermission()) {
            val permissions = arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            ActivityCompat.requestPermissions(this, permissions, REQUEST_MIC)
        }
    }

    private fun hasMicPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    private fun gotoSample() {
        val intent = Intent(this, SampleActivity::class.java)
        startActivity(intent)
    }

}