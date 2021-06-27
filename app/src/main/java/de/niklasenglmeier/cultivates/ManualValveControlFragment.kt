package de.niklasenglmeier.cultivates

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.android.volley.*
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import java.nio.charset.Charset


class ManualValveControlFragment : Fragment(), View.OnClickListener, View.OnLongClickListener {

    lateinit var buttonDecreaseDuration: Button
    lateinit var buttonIncreaseDuration: Button
    lateinit var textDuration: TextView
    var targetDuration = 0
    lateinit var buttonOpenValve: Button

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_command, container, false)

        buttonDecreaseDuration = v.findViewById(R.id.button_decreaseduration)
        buttonDecreaseDuration.setOnClickListener(this)
        buttonDecreaseDuration.setOnLongClickListener(this)

        buttonIncreaseDuration = v.findViewById(R.id.button_increaseduration)
        buttonIncreaseDuration.setOnClickListener(this)
        buttonIncreaseDuration.setOnLongClickListener(this)

        textDuration = v.findViewById(R.id.textview_duration)

        buttonOpenValve = v.findViewById(R.id.button_command_run)
        buttonOpenValve.setOnClickListener(this)

        updateFragment()

        return v
    }

    private fun updateFragment() {
        textDuration.text = targetDuration.toString() + if (targetDuration == 1) " second" else " seconds"
    }

    override fun onClick(v: View?) {
        when(v!!.id) {
            R.id.button_command_run -> {
                makeRequest(targetDuration)
            }

            R.id.button_increaseduration -> {
                targetDuration++
            }

            R.id.button_decreaseduration -> {
                targetDuration--
            }
        }

        updateFragment()
    }

    fun makeRequest(duration: Int) {
        val queue = Volley.newRequestQueue(activity)
        val url = "http://000raspberry.ddns.net/cultivates/api/valve?password=12345678"
        if (duration == 0)
            return

        val httpRequest = StringRequest(
                Request.Method.POST,
                url,
                {
                    //response -> chartFragment.setData(Gson().fromJson(response, Array<SensorValue>::class.java))
                },
                {}
        )

        val URL = ""
        val requestBody = "{\"id\": 1, \"schedule_watering\": true, \"schedule_watering_duration\": $duration}"

        val stringRequest: StringRequest = object : StringRequest(
                        Method.POST,
                        url,
                        Response.Listener { Toast.makeText(requireActivity().applicationContext, "Valve opening scheduled", Toast.LENGTH_LONG).show() },
                        Response.ErrorListener { error -> Toast.makeText(requireActivity().applicationContext, "Communication failed \n$error", Toast.LENGTH_LONG).show() }
                )
                {
                    override fun getBodyContentType(): String {
                        return "application/json; charset=utf-8"
                    }

                    @Throws(AuthFailureError::class)
                    override fun getBody(): ByteArray {
                        return requestBody.toByteArray(Charset.defaultCharset())
                    }

                    override fun parseNetworkResponse(response: NetworkResponse): Response<String> {
                        var responseString = ""
                        if (response != null) {
                            responseString = response.statusCode.toString()
                            // can get more details such as response.headers
                        }
                        return Response.success(responseString, HttpHeaderParser.parseCacheHeaders(response))
                    }
                }

        queue.add(stringRequest)
    }

    override fun onLongClick(v: View?): Boolean {
        when(v!!.id) {
            R.id.button_decreaseduration ->
            {
                targetDuration -= 10
            }

            R.id.button_increaseduration ->
            {
                targetDuration += 10
            }
        }

        if (targetDuration < 0)
            targetDuration = 0

        updateFragment()

        return true
    }
}