package com.akwok.simpletuner.model

import com.akwok.simpletuner.models.Accuracy
import org.junit.Assert
import org.junit.Test

class AccuracyUnitTest {
    @Test
    fun sampleSizeIsPowerOf2() {
        Accuracy.values()
            .forEach { acc ->
                Assert.assertEquals(0, acc.sampleSize.and(acc.sampleSize - 1))
            }
    }
}