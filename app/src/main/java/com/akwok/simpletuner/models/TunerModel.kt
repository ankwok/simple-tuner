package com.akwok.simpletuner.models

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akwok.simpletuner.io.MicReader
import com.akwok.simpletuner.tuner.PitchDetector
import com.akwok.simpletuner.tuner.PitchError
import com.akwok.simpletuner.tuner.PitchHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TunerModel : ViewModel() {

    fun startRecording() {
        isRecording.value = true
        run()
    }

    fun stopRecording() {
        isRecording.value = false
    }

    val pitchError: MutableLiveData<PitchError?> by lazy {
        MutableLiveData<PitchError?>()
    }

    val referenceA: MutableLiveData<Int> by lazy {
        MutableLiveData(PitchHelper.defaultReference)
    }

    val detectionThreshold: MutableLiveData<Double> by lazy {
        MutableLiveData(PitchDetector.defaultDetectionThreshold)
    }

    val sampleSize: MutableLiveData<Int> by lazy {
        MutableLiveData(DEFAULT_SAMPLE_SIZE)
    }

    private val isRecording: MutableLiveData<Boolean> by lazy { MutableLiveData(false) }

    private fun run() {
        viewModelScope.launch(Dispatchers.IO) {
            val micReader = MicReader()
            var tuner = PitchDetector(PitchHelper.defaultReference.toDouble())
            val audioData = micReader.getBufferInstance(sampleSize.value ?: DEFAULT_SAMPLE_SIZE)

            micReader.startRecording()

            while (isRecording.value == true) {
                val ref = (referenceA.value ?: PitchHelper.defaultReference).toDouble()
                val threshold = detectionThreshold.value ?: PitchDetector.defaultDetectionThreshold

                if (ref != tuner.ref) {
                    tuner = PitchDetector(ref, threshold)
                } else if (threshold != tuner.detectionThreshold) {
                    tuner = PitchDetector(ref, threshold)
                }

                pitchError.postValue(tuner.detect(micReader.read(audioData)))
            }

            micReader.stopRecording()
        }
    }

    companion object {
        private const val DEFAULT_SAMPLE_SIZE = 8192
    }
}