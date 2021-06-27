package de.niklasenglmeier.cultivates.models

import com.google.gson.annotations.SerializedName

class ValveData {
    var id = 0

    @SerializedName("last_time_watered")
    var lastTimeWatered = 0L

    @SerializedName("schedule_watering")
    var scheduleWatering: Boolean? = null

    @SerializedName("schedule_watering_duration")
    var scheduleWateringDuration: Float? = 0.0f
}