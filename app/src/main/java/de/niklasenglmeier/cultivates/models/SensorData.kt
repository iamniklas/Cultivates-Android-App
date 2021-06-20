package de.niklasenglmeier.cultivates.models

import com.google.gson.annotations.SerializedName

class SensorData {
    var id = 0

    @SerializedName("sensor_group")
    var sensorGroup = 0

    var value = 0

    @SerializedName("updated_at")
    var updatedAt = 0
}