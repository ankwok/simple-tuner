package com.akwok.simpletuner.views

import android.animation.TimeAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.core.graphics.alpha
import com.akwok.simpletuner.R
import kotlin.math.*

class GaugeView(context: Context, attrs: AttributeSet?) : TunerView(context, attrs) {
    constructor(context: Context) : this(context, null)

    private val tickColor = Paint().apply {
        isAntiAlias = true
        color = resources.getColor(R.color.tick_color, context.theme)
        style = Paint.Style.FILL_AND_STROKE
    }
    
    private var needleCenter: Float = -1f
    private var needleVelocity: Float = 0f

    override var octave: Int? = null

    override var errorInCents: Float? = null
        set(value) {
            if ((value != null) && (value != 0f)) {
                field = value
            }
        }

    private val widthPadding: Float
        get() = if (width > 0) paddingPct * width else 0f

    private val heightPadding: Float
        get() = if (height > 0) paddingPct * height else 0f

    private val animator = TimeAnimator()

    private var widthForTicks = width
    private var majorTickCenters = FloatArray(0)
    private var minorTickCenters = FloatArray(0)

    init {
        animator.setTimeListener { _, _, deltaTime ->
            val err = errorInCents
            if (err != null && width > 0) {
                val nextPos = computeNeedleCenter(err, width, widthPadding)
                val delta = nextPos - needleCenter

                needleVelocity += 0.001f * deltaTime * (kp * delta - damping * needleVelocity)
                needleCenter += 0.001f * deltaTime * needleVelocity

                postInvalidate()
            }
        }
    }

    override fun postInvalidate() {
        if (!needleCenter.isFinite()) {
            needleCenter = width.toFloat() / 2
        }

        super.postInvalidate()
    }

    override fun start() {
        if (animator.isPaused || !animator.isRunning || !animator.isStarted) {
            animator.start()
        }
    }

    override fun pause() {
        animator.pause()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // The view may not be completely initialized so only draw if there's a positive width
        if (width > 0) {
            canvas.apply {
                drawAndUpdateState(this)
            }
        }
    }

    private fun drawAndUpdateState(canvas: Canvas) {
        clearView(canvas)
        drawTicks(canvas)
        drawNeedle(canvas)
    }

    private fun clearView(canvas: Canvas) {
        val backgroundPaint = Paint().apply {
            isAntiAlias = true
            color = resources.getColor(R.color.tuner_background, context.theme)
            style = Paint.Style.FILL_AND_STROKE
        }

        canvas.drawRect(
            widthPadding,
            2 * heightPadding,
            width - widthPadding,
            height - 2 * heightPadding,
            backgroundPaint
        )
    }

    private fun drawTicks(canvas: Canvas) {
        ensureTickCenters()

        val majorTickTop = height.toFloat() / 2 - height.toFloat() / 8
        val majorTickBottom = height.toFloat() / 2 + height.toFloat() / 8
        val majorTickHalfWidth = majorTicksWidthPct * width / 2

        for (center in majorTickCenters) {
            canvas.drawRect(
                center - majorTickHalfWidth,
                majorTickTop,
                center + majorTickHalfWidth,
                majorTickBottom,
                tickColor
            )
        }

        val minorTickTop = height.toFloat() / 2 - height.toFloat() / 16
        val minorTickBottom = height.toFloat() / 2 + height.toFloat() / 16
        val minorTickHalfWidth = minorTicksWidthPct * width / 2

        for (center in minorTickCenters) {
            canvas.drawRect(
                center - minorTickHalfWidth,
                minorTickTop,
                center + minorTickHalfWidth,
                minorTickBottom,
                tickColor
            )
        }
    }

    private fun ensureTickCenters() {
        if (width == widthForTicks && majorTickCenters.isNotEmpty() && minorTickCenters.isNotEmpty()) {
            return
        }

        widthForTicks = width
        majorTickCenters = getTickCenters(majorTicks)
        minorTickCenters = getTickCenters(minorTicks)
    }

    private fun getTickCenters(ticks: FloatArray): FloatArray {
        val centers = FloatArray(ticks.size)
        for (i in ticks.indices) {
            centers[i] = computeNeedleCenter(ticks[i], width, widthPadding)
        }

        return centers
    }

    private fun drawNeedle(canvas: Canvas) {
        if (needleCenter <= 0) { return }

        val needleColor = computeNeedleColor(
            needleCenter,
            width.toFloat() / 2,
            computeNeedleCenter(mehError, width, widthPadding),
            computeNeedleCenter(okError, width, widthPadding),
            resources.getColor(R.color.needle_color, context.theme),
            resources.getColor(R.color.needle_color_ok, context.theme)
        )
        val needlePaint = Paint().apply {
            isAntiAlias = true
            color = needleColor
            style = Paint.Style.FILL_AND_STROKE
        }

        val needleWidth = width * needleWidthPct
        val halfWidth = needleWidth / 2
        canvas.drawRect(
            needleCenter - halfWidth,
            3 * heightPadding,
            needleCenter + halfWidth,
            height - 3 * heightPadding,
            needlePaint
        )
    }

    companion object {
        private const val paddingPct = 0.05f
        private const val needleWidthPct = 0.01f // percent of width
        private const val exponent = 1 / 1.75
        private const val kp = 36f
        private val damping = sqrt(4 * kp) // critically damped

        private val majorTicks = (-10 .. 10).map { i -> 5f * i }.toFloatArray()
        private const val majorTicksWidthPct = 0.01f
        private val minorTicks = (-50 .. 50)
            .map { x -> x.toFloat() }
            .filterNot { x -> majorTicks.any { tick -> x == tick } }
            .toFloatArray()
        private const val minorTicksWidthPct = 0.004f

        private const val mehError = 10f // cents
        private const val okError = 1f // cents

        fun computeNeedleCenter(errorInCents: Float, width: Int, padding: Float): Float {
            val mid = width / 2

            val halfWidth = width.toDouble() / 2 - padding
            val scaleFactor = halfWidth.pow(1 / exponent) / 50
            val offset = (abs(errorInCents) * scaleFactor).pow(exponent)

            return mid + errorInCents.sign * offset.toFloat()
        }

        fun computeNeedleColor(
            needleCenter: Float,
            center: Float,
            badPosition: Float,
            okPosition: Float,
            @ColorInt badColor: Int,
            @ColorInt okColor: Int
        ): Int {
            val absErr = abs(needleCenter - center)
            val badErr = badPosition - center
            val okErr = okPosition - center
            if (absErr > badErr) {
                return badColor
            } else if (absErr <= okErr) {
                return okColor
            }

            val t = (absErr - okErr) / (badErr - okErr)
            val color = interpHsv(okColor, badColor, t)
            return color
        }

        fun interpHsv(@ColorInt color1: Int, @ColorInt color2: Int, t: Float): Int {
            val hsv1 = FloatArray(3)
            Color.colorToHSV(color1, hsv1)

            val hsv2 = FloatArray(3)
            Color.colorToHSV(color2, hsv2)

            val alpha = interp(color1.alpha, color2.alpha, t).roundToInt()
            val interpHsv = FloatArray(3)

            interpHsv[0] = interpAngle(hsv1[0], hsv2[0], t)
            interpHsv[1] = interp(hsv1[1], hsv2[1], t)
            interpHsv[2] = interp(hsv1[2], hsv2[2], t)

            return Color.HSVToColor(alpha, interpHsv)
        }

        fun interpAngle(theta1: Float, theta2: Float, t: Float): Float {
            var delta = theta2 - theta1
            while (delta > 180) {
                delta -= 360
            }
            while (delta <= -180) {
                delta += 360
            }

            var interp = theta1 + t * delta
            while (interp < 0) {
                interp += 360
            }
            while (interp >= 360) {
                interp -= 360
            }

            return interp
        }

        private fun interp(x: Int, y: Int, t: Float): Float = t * y + (1 - t) * x
        private fun interp(x: Float, y: Float, t: Float): Float = t * y + (1 - t) * x
    }
}