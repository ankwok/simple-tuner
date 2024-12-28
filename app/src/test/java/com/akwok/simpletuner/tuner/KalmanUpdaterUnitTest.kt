package com.akwok.simpletuner.tuner

import org.junit.Assert
import org.junit.Test

class KalmanUpdaterUnitTest {

    @Test
    fun estimateUnchangedForHugeMeasurementNoise() {
        val initState = KalmanState(42.0, 1.0)
        val updater = KalmanUpdater(initState, 1.0)
        updater.update(1000.0, 1e20)

        Assert.assertEquals(initState.x, updater.stateEstimate.x, 1e-4)
    }

    @Test
    fun estimateIsMeasurementForSmallMeasurementNoise() {
        val initState = KalmanState(42.0, 1.0)
        val updater = KalmanUpdater(initState, 1.0)
        updater.update(1000.0, 1e-20)

        Assert.assertEquals(1000.0, updater.stateEstimate.x, 1e-4)
    }

    @Test
    fun estimateUnchangedIfSameAsMeasurement() {
        val initState = KalmanState(42.0, 1.0)
        val updater = KalmanUpdater(initState, 1.0)
        updater.update(initState.x, 1.0)

        Assert.assertEquals(initState.x, updater.stateEstimate.x, 1e-4)
    }

    @Test
    fun uncertaintyDecreasesOnRepeatedMeasurements() {
        val initState = KalmanState(42.0, 1.0)
        val updater = KalmanUpdater(initState, 1e-10)
        updater.update(initState.x, 1.0)

        val var1 = updater.stateEstimate.P
        Assert.assertTrue(var1 < initState.P)

        updater.update(initState.x, 1.0)
        Assert.assertTrue(updater.stateEstimate.P < var1)
    }
}