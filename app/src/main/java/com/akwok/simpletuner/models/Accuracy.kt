package com.akwok.simpletuner.models

enum class Accuracy(val sampleSize: Int) {
    LOW(4096),
    MEDIUM(8192),
    HIGH(16384)
}