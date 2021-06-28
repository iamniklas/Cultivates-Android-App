package de.niklasenglmeier.cultivates

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.android.volley.*
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import de.niklasenglmeier.cultivates.models.EndpointDefinition
import de.niklasenglmeier.cultivates.models.SensorData
import de.niklasenglmeier.cultivates.models.ValveData
import org.json.JSONException
import java.math.BigDecimal
import java.math.RoundingMode

class MainActivity : AppCompatActivity(), INFCCallbacks, SwipeRefreshLayout.OnRefreshListener {

    private val hostname = "http://000raspberry.ddns.net"
    private val httpParameters: LinkedHashMap<String, String> = linkedMapOf("password" to "12345678")

    private val collectionPath = "ChipData"
    private val documentPath = "Data"

    private lateinit var nfcController: NFCController

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var chartFragment: SensorChartFragment
    private lateinit var nfcInteractionText: TextView
    private lateinit var lastTimeWateredText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        swipeRefreshLayout = findViewById(R.id.swiperefreshlayout_main)
        swipeRefreshLayout.setOnRefreshListener(this)
        chartFragment = supportFragmentManager.findFragmentById(R.id.fragment_sensorchart) as SensorChartFragment
        nfcInteractionText = findViewById(R.id.textview_nfcinfo)
        lastTimeWateredText = findViewById(R.id.textview_lasttimewateredvalue)

        nfcController = NFCController(this, this)

        //Request all sensor data via http
        val queue = Volley.newRequestQueue(this)
        val sensorRequest = StringRequest(
                Request.Method.GET,
                "$hostname/cultivates/api/sensors?password=${httpParameters["password"]}",
                { response ->
                    chartFragment.setData(Gson().fromJson(response, Array<SensorData>::class.java))
                },
                { }
        )
        val valveRequest = StringRequest(
                Request.Method.GET,
                "$hostname/cultivates/api/valve?password=${httpParameters["password"]}",
                { response ->
                    run {
                        val valveData = Gson().fromJson(response, Array<ValveData>::class.java)
                        val sdf = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss")
                        val date = java.util.Date(valveData.first().lastTimeWatered * 1000)
                        lastTimeWateredText.text = sdf.format(date)
                    }
                },
                { }
        )

        queue.add(sensorRequest)
        queue.add(valveRequest)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        nfcController.readFromIntent(intent!!)
    }

    private fun doFirestoreRequest(key: String) {
        val db = FirebaseFirestore.getInstance()

        db.collection(collectionPath)
                .document(documentPath)
                .get()
                .addOnCompleteListener { task ->
                    val dbResult = task.result?.get(key) as String

                    val obj = Gson().fromJson(dbResult, EndpointDefinition::class.java)
                    makeRequest(obj, obj.predefinedParams)
                }
    }

    private fun makeRequest(endpointObject: EndpointDefinition, params: LinkedHashMap<String, String>) {
        //Add Hostname and in cloud defined path to url
        var url = "$hostname${endpointObject.path}"
        //Combine cloud defined parameters and parameters defined in activity
        val requestParams = (params.asSequence() + httpParameters.asSequence())
                .distinct()
                .groupBy({ it.key }, { it.value })

        //Add parameters to url
        for (i in 0 until requestParams.size) {
            val key = requestParams.keys.elementAt(i)

            url += if (i == 0) {
                "?$key=${requestParams[key]?.get(0)}"
            } else {
                "&$key=${requestParams[key]?.get(0)}"
            }
        }

        when(endpointObject.target) {
            TargetDevice.Sensor,
            TargetDevice.SensorSingle -> {
                makeSensorRequest(url, endpointObject)
            }

            TargetDevice.Valve -> {
                makeValveRequest(url, endpointObject)
            }
        }
    }

    fun makeSensorRequest(url: String, endpointObject: EndpointDefinition) {
        val queue = Volley.newRequestQueue(this)

        val httpRequest = StringRequest(
                Request.Method.GET,
                url,
                { response ->
                    run {
                        val dialogTitle = "NFC Action"
                        var dialogMessage = ""
                        when (endpointObject.target) {
                            //Multi Sensor
                            TargetDevice.Sensor -> {
                                val responseObject = Gson().fromJson(response, Array<SensorData>::class.java)
                                dialogMessage += "Sensors report the following values:\n"
                                for (i in responseObject.indices) {
                                    dialogMessage += "Sensor ${responseObject[i].id}: ${BigDecimal(((responseObject[i].value / 1023.0f).coerceAtMost(1.0f) * 100.0f).toDouble()).setScale(1, RoundingMode.CEILING)}%\n"
                                }
                            }

                            //Single Sensor
                            TargetDevice.SensorSingle -> {
                                val responseObject = Gson().fromJson(response, Array<SensorData>::class.java)
                                dialogMessage += "Sensor ${responseObject[0].id} reports a moisture of ${BigDecimal(((responseObject[0].value / 1023.0f).coerceAtMost(1.0f) * 100.0f).toDouble()).setScale(1, RoundingMode.CEILING)}%."
                            }
                        }

                        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
                        builder.setMessage(dialogMessage)
                        builder.setTitle(dialogTitle)
                        builder.setCancelable(false)
                        builder.setPositiveButton(getString(R.string.dialog_positive_button)) { dialog, _ -> dialog.cancel() }

                        val alert: AlertDialog = builder.create()
                        alert.show()
                    }
                },
                { Toast.makeText(applicationContext, "API Reuqest failed\nFor details, check logs", Toast.LENGTH_LONG).show() })

        queue.add(httpRequest)
    }

    fun makeValveRequest(url: String, endpointObject: EndpointDefinition) {
        try {
            val requestQueue = Volley.newRequestQueue(this)
            val URL = url
            val valveData = ValveData()
            valveData.apply {
                id = 1
                lastTimeWatered = System.currentTimeMillis() / 1000L
                scheduleWatering = true
                scheduleWateringDuration = endpointObject.predefinedParams["duration"]!!.toFloat()
            }
            val requestBody = Gson().toJson(valveData)
            val stringRequest: StringRequest = object : StringRequest(
                    Method.POST,
                    URL,
                    Response.Listener
                    {
                        run {
                            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
                            builder.setMessage("Valve will be opened for ${valveData.scheduleWateringDuration!!.toInt()} seconds.")
                            builder.setTitle("Data transmitted")
                            builder.setCancelable(false)
                            builder.setPositiveButton(getString(R.string.dialog_positive_button)) { dialog, _ -> dialog.cancel() }

                            val alert: AlertDialog = builder.create()
                            alert.show()
                        }
                    },

                    Response.ErrorListener
                    {
                        error -> Log.e("VOLLEY", error.toString())
                    }
            )
            {
                override fun getBodyContentType(): String {
                    return "application/json; charset=utf-8"
                }

                @Throws(AuthFailureError::class)
                override fun getBody(): ByteArray {
                    return requestBody.toByteArray(charset("utf-8"))
                }

                override fun parseNetworkResponse(response: NetworkResponse): Response<String> {
                    var responseString = ""
                    if (response != null) {
                        responseString = response.statusCode.toString()
                    }
                    return Response.success(responseString, HttpHeaderParser.parseCacheHeaders(response))
                }
            }
            requestQueue.add(stringRequest)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    //Gets called if phone does not support NFC
    override fun onNFCSupportNotGiven() {
        Toast.makeText(applicationContext, getString(R.string.notification_no_nfc_support), Toast.LENGTH_SHORT).show()
        nfcInteractionText.visibility = View.GONE
    }

    //Callback for NFC Controller
    override fun onNFCRead(message: String) {
        doFirestoreRequest(message)
    }

    override fun onRefresh() {
        //Do http request for sensor data on refresh
        val queue = Volley.newRequestQueue(this)
        val httpRequest = StringRequest(
                Request.Method.GET,
                "$hostname/cultivates/api/sensors?password=${httpParameters["password"]}",
                { response ->
                    chartFragment.setData(Gson().fromJson(response, Array<SensorData>::class.java))
                    swipeRefreshLayout.isRefreshing = false
                },
                {}
        )

        queue.add(httpRequest)
    }
}