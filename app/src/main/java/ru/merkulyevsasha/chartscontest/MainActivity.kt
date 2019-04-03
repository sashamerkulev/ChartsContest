package ru.merkulyevsasha.chartscontest

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.activity_main.*
import ru.merkulyevsasha.chartscontest.controls.ChartLayoutView
import ru.merkulyevsasha.chartscontest.models.ChartData
import ru.merkulyevsasha.chartscontest.sources.Example
import java.io.BufferedReader
import java.io.InputStreamReader


class MainActivity : AppCompatActivity(), IMainView {

    private val pres = MainPresenter(SourceDataConverter())

    private var charts = mutableListOf<ChartLayoutView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        charts.add(chart1)
        charts.add(chart2)
        charts.add(chart3)
        charts.add(chart4)
        charts.add(chart5)

        try {
            val source = readSource()
            val root = convertToObject(source)
            pres.onBind(this)
            pres.dealWithIt(root)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_activity_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        pres.onBind(this)
    }

    override fun onPause() {
        pres.onUnbind()
        super.onPause()
    }

    override fun showCharts(chartData: List<ChartData>) {
        for (index in 0 until chartData.size) {
            charts[index].setData(chartData[index])
        }
    }

    private fun readSource(): String {
        val stream = assets.open("chart_data.json")
        val text = StringBuilder()
        val inputReader = InputStreamReader(stream)
        val buffReader = BufferedReader(inputReader)
        val line = buffReader.readLine()
        if (line != null) {
            text.append(line)
        }
        return text.toString()
    }

    private fun convertToObject(json: String): List<Example> {
        val gson = Gson()
        val listType = object : TypeToken<List<Example>>() {}.type
        val data: List<Example> = gson.fromJson(json, listType)
        return data
    }

}
