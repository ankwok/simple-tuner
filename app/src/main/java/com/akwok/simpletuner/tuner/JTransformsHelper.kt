package com.akwok.simpletuner.tuner

/**
 * Only deal with the results of realForward() on even-sized arrays.  From JTransforms doc, the
 * data is stored as:
 *    a[2*k] = Re[k], 0<=k<n/2
 *    a[2*k+1] = Im[k], 0<k<n/2
 *    a[1] = Re[n/2]
 */
object JTransformsHelper {

    fun conj(arr: FloatArray) {
        require(arr.size % 2 == 0)

        val n = arr.size
        for (i in 1 until n / 2) {
            arr[2*i + 1] = -arr[2*i + 1]
        }
    }

    fun mult(arr1: FloatArray, arr2: FloatArray): FloatArray {
        require(arr1.size == arr2.size && arr1.size % 2 == 0)

        val res = FloatArray(arr1.size)
        res[0] = arr1[0] * arr2[0]
        res[1] = arr1[1] * arr2[1]

        for (i in 1 until arr1.size / 2) {
            res[2*i] = arr1[2*i] * arr2[2*i] - arr1[2*i + 1] * arr2[2*i + 1]
            res[2*i + 1] = arr1[2*i] * arr2[2*i + 1] + arr1[2*i + 1] * arr2[2*i]
        }

        return res
    }
}