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
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset


class CommandFragment : Fragment(), View.OnClickListener {
    lateinit var spinner: Spinner
    lateinit var editTextId: EditText
    lateinit var editTextParam: EditText
    lateinit var buttonRun: Button

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_command, container, false)

        val colors = arrayOf("Sensor", "Valve")
        val spinnerArrayAdapter: ArrayAdapter<String> = ArrayAdapter<String>(requireActivity().applicationContext, android.R.layout.simple_spinner_dropdown_item, colors)
        spinner = v.findViewById(R.id.spinner_command_type)
        spinner.adapter = spinnerArrayAdapter

        editTextId = v.findViewById(R.id.edittext_command_id)

        editTextParam = v.findViewById(R.id.edittext_command_param)

        buttonRun = v.findViewById(R.id.button_command_run)
        buttonRun.setOnClickListener(this)

        return v
    }

    override fun onClick(v: View?) {
        when(v!!.id) {
            R.id.button_command_run -> {
                makeRequest(spinner.selectedItem as String,
                        editTextId.editableText.toString().toInt(),
                        editTextParam.editableText.toString().toInt())
            }
        }
    }

    fun makeRequest(target: String, id: Int = 0, param: Int = 1) {
        when(target) {
            "Sensor" -> {
                val queue = Volley.newRequestQueue(activity)
                var url = "http://000raspberry.ddns.net/cultivates/api/sensors?password=12345678"
                if (id != 0)
                    url += "&id=$id"
                val httpRequest = StringRequest(
                        Request.Method.GET,
                        url,
                        {
                            //response -> chartFragment.setData(Gson().fromJson(response, Array<SensorValue>::class.java))
                        },
                        {}
                )
                queue.add(httpRequest)
            }

            "Valve" -> {
                val queue = Volley.newRequestQueue(activity)
                var url = "http://000raspberry.ddns.net/cultivates/api/valve?password=12345678"
                if (id != 0)
                    url += "&id=$id"

                url += "&duration=$param"

                val httpRequest = StringRequest(
                        Request.Method.POST,
                        url,
                        {
                            //response -> chartFragment.setData(Gson().fromJson(response, Array<SensorValue>::class.java))
                        },
                        {}
                )

                val URL = ""
                val requestBody = ""

                val stringRequest: StringRequest = object : StringRequest(
                                Method.POST,
                                URL,
                                Response.Listener { response -> Log.i("VOLLEY", response!!) },
                                Response.ErrorListener { error -> Log.e("VOLLEY", error.toString()) }
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

                queue.add(httpRequest)
            }
        }
    }
}