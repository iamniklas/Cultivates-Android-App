package de.niklasenglmeier.cultivates

import com.google.gson.annotations.SerializedName

class EndpointDefinition {
    var path: String = ""

    var target: TargetDevice = TargetDevice.Sensor

    @SerializedName("predefined_params")
    var predefinedParams: LinkedHashMap<String, String> = LinkedHashMap()
}