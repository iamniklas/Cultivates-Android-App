package de.niklasenglmeier.cultivates

import java.util.*

class RandomValue {
    companion object {
        fun between(min: Int, max: Int): Int {
            return Random().nextInt(max - min) + min
        }
    }
}