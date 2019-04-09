package ru.merkulyevsasha.chartscontest

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.AppCompatDelegate
import android.view.Menu
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_main.*
import ru.merkulyevsasha.chartscontest.controls.ChartLayoutView
import ru.merkulyevsasha.chartscontest.models.ChartData
import ru.merkulyevsasha.chartscontest.sources.Example
import java.io.BufferedReader
import java.io.InputStreamReader


class MainActivity : AppCompatActivity(), IMainView {

    private val pres = MainPresenter(SourceDataConverter())

    private var charts = mutableListOf<ChartLayoutView>()
    private var nightMode: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)

        charts.add(chart1)
        charts.add(chart2)
        charts.add(chart3)
        charts.add(chart4)
        charts.add(chart5)

        try {
            val examples = mutableListOf<Example>()
            for (index in 1..5) {
                val source = readSource("contest2/$index/overview.json")
                val root = convertToObject(source)
                examples.add(root)
            }
            pres.onBind(this)
            pres.dealWithIt(examples)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_activity_main, menu)

        val dayNightMenu = menu.findItem(R.id.day_night)
        dayNightMenu.setOnMenuItemClickListener {
            nightMode = !nightMode
            if (nightMode) {
                delegate.setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                delegate.setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            true
        }

        return true
    }

    override fun onResume() {
        super.onResume()
        pres.onBind(this)
    }

    override fun onPause() {
        pres.onUnbind()
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putBoolean("nightmode", nightMode)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        nightMode = savedInstanceState?.getBoolean("nightmode", false) ?: false
    }

    override fun showCharts(chartData: List<ChartData>) {
        for (index in 0 until chartData.size) {
            charts[index].setData(chartData[index])
        }
    }

    private fun readSource(name: String): String {
        val stream = assets.open(name)
        val text = StringBuilder()
        val inputReader = InputStreamReader(stream)
        val buffReader = BufferedReader(inputReader)
        val line = buffReader.readLine()
        if (line != null) {
            text.append(line)
        }
        return text.toString()
    }

    private fun convertToObject(json: String): Example {
        val gson = Gson()
        return gson.fromJson(json, Example::class.java)
    }

//    private fun convertToObject(json: String): List<Example> {
//        val gson = Gson()
//        val listType = object : TypeToken<List<Example>>() {}.type
//        val data: List<Example> = gson.fromJson(json, listType)
//        return data
//    }

}
