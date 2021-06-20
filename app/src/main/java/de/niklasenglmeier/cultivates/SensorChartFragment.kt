package de.niklasenglmeier.cultivates

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.*
import de.niklasenglmeier.cultivates.models.SensorData

class SensorChartFragment : Fragment() {

    lateinit var chartView: BarChart
    private var entrys = mutableListOf<BarEntry>()
    private var data = listOf<SensorData>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sensor_chart, container, false)

        chartView = view.findViewById(R.id.bar_chart)

        return view
    }

    fun setData(_data: Array<SensorData>) {
        data = _data.toList()

        //Add bar entries from SensorValue Array
        for (i in 1 until _data.size) {
            val barEntry = BarEntry(i.toFloat(), _data[i].value.toFloat())
            entrys.add(barEntry)
        }

        chartView.description.isEnabled = false

        val bes = BarDataSet(entrys, "Moisture")

        chartView.data = BarData(bes)

        //User feedback
        Toast.makeText(activity?.applicationContext, "Sensor data fetched", Toast.LENGTH_SHORT).show()
    }
}