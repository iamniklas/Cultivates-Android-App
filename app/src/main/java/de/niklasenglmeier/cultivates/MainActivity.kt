package de.niklasenglmeier.cultivates

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import de.niklasenglmeier.cultivates.models.SensorData
import java.math.BigDecimal
import java.math.RoundingMode

class MainActivity : AppCompatActivity(), INFCCallbacks, SwipeRefreshLayout.OnRefreshListener {

    private val hostname = "http://000raspberry.ddns.net"
    private val httpParameters: LinkedHashMap<String, String> = linkedMapOf("password" to "12345678")

    private val collectionPath = "ChipData"
    private val documentPath = "Data"

    private lateinit var nfcController: NFCController

    lateinit var swipeRefreshLayout: SwipeRefreshLayout
    lateinit var chartFragment: SensorChartFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        nfcController = NFCController(this, this)

        swipeRefreshLayout = findViewById(R.id.swiperefreshlayout_main)
        swipeRefreshLayout.setOnRefreshListener(this)
        chartFragment = supportFragmentManager.findFragmentById(R.id.fragment_sensorchart) as SensorChartFragment

        //Request all sensor data via http
        val queue = Volley.newRequestQueue(this)
        val httpRequest = StringRequest(
                Request.Method.GET,
                "$hostname/cultivates/api/sensors?password=${httpParameters["password"]}",
                {
                    response -> chartFragment.setData(Gson().fromJson(response, Array<SensorData>::class.java))
                },
                { }
        )

        queue.add(httpRequest)
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
        val queue = Volley.newRequestQueue(this)
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

        //Make request and show dialog with target information
        val httpRequest = StringRequest(
                Request.Method.GET,
                url,
                { response -> run {
                    val dialogTitle = "NFC Action"
                    var dialogMessage = ""
                    when(endpointObject.target) {
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

                        //Valve
                        TargetDevice.Valve -> {
                            dialogMessage += "Valve will be opened"
                        }
                    }

                    val builder: AlertDialog.Builder = AlertDialog.Builder(this)
                    builder.setMessage(dialogMessage)
                    builder.setTitle(dialogTitle)
                    builder.setCancelable(false)
                    builder.setPositiveButton(getString(R.string.dialog_positive_button)) { dialog, _ -> dialog.cancel() }

                    val alert: AlertDialog = builder.create()
                    alert.show()
                } },
                { Toast.makeText(applicationContext, "API Reuqest failed\nFor details, check logs", Toast.LENGTH_LONG).show() })

        queue.add(httpRequest)
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
                {
                    response -> chartFragment.setData(Gson().fromJson(response, Array<SensorData>::class.java))
                    swipeRefreshLayout.isRefreshing = false
                },
                {}
        )

        queue.add(httpRequest)
    }
}