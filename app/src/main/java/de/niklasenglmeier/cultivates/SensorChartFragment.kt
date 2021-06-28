package de.niklasenglmeier.cultivates

import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.core.content.ContextCompat
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
        chartView.apply {
            setTouchEnabled(false)
            isDragEnabled = false
            setScaleEnabled(false)
            isScaleXEnabled = false
            isScaleYEnabled = false
            setPinchZoom(false)
        }

        return view
    }

    fun setData(_data: Array<SensorData>) {
        data = _data.toList()

        //Add bar entries from SensorValue Array
        for (i in _data.indices) {
            val barEntry = BarEntry((i.toFloat()+1), (_data[i].value / 1023.0f) * 100.0f)
            entrys.add(barEntry)
        }

        chartView.description.isEnabled = false

        val bes = BarDataSet(entrys, getString(R.string.chart_y_label))
        val color = ContextCompat.getColor(requireActivity().applicationContext, R.color.light_blue_400)
        bes.color = color

        chartView.data = BarData(bes)

        //Force view to update
        chartView.invalidate()

        //User feedback
        Toast.makeText(activity?.applicationContext, getString(R.string.notification_sensor_data_fetched), Toast.LENGTH_SHORT).show()
    }
}