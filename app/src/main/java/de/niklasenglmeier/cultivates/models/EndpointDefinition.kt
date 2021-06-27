package de.niklasenglmeier.cultivates.models

import com.google.gson.annotations.SerializedName
import de.niklasenglmeier.cultivates.TargetDevice

class EndpointDefinition {
    var path: String = ""

    var target: TargetDevice = TargetDevice.Sensor

    @SerializedName("predefined_params")
    var predefinedParams: LinkedHashMap<String, String> = LinkedHashMap()
}