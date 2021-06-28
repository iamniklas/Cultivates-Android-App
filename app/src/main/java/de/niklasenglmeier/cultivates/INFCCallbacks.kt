package de.niklasenglmeier.cultivates

interface INFCCallbacks {
    fun onNFCSupportNotGiven()
    fun onNFCRead(message: String)
}