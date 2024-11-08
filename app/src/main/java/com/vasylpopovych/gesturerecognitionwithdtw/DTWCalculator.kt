package com.vasylpopovych.gesturerecognitionwithdtw

import kotlin.math.sqrt
import kotlin.math.pow

class DTWCalculator {

    fun calculateDTW(x1 : List<Float>, y1: List<Float>, z1: List<Float>, gesture2: Gesture): Double {
        val (x2, y2, z2) = gesture2.getData()

        val n = x1.size
        val m = x2.size

        val dtwMatrix = Array(n + 1) { DoubleArray(m + 1) { Double.POSITIVE_INFINITY } }

        dtwMatrix[0][0] = 0.0

        for (i in 1..n) {
            for (j in 1..m) {
                val cost = calculateDistance(x1[i - 1], y1[i - 1], z1[i - 1], x2[j - 1], y2[j - 1], z2[j - 1])
                dtwMatrix[i][j] = cost + minOf(
                    dtwMatrix[i - 1][j],
                    dtwMatrix[i][j - 1],
                    dtwMatrix[i - 1][j - 1]
                )
            }
        }

        return dtwMatrix[n][m]
    }

    private fun calculateDistance(x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float): Double {
        return sqrt(((x2 - x1).toDouble().pow(2.0) + (y2 - y1).toDouble().pow(2.0) + (z2 - z1).toDouble().pow(2.0)))
    }
}
