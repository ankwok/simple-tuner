package com.akwok.simpletuner.views

import android.content.Context
import android.util.AttributeSet
import android.view.View

abstract class TunerView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    abstract var errorInCents: Float?
    abstract var octave: Int?
    abstract fun start()
    abstract fun pause()
}