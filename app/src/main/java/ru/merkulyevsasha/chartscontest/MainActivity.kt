package ru.merkulyevsasha.chartscontest

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.AppCompatDelegate
import android.view.Menu
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_main.*
import ru.merkulyevsasha.chartscontest.controls.ChartLayoutView
import ru.merkulyevsasha.chartscontest.controls.ChartLegend
import ru.merkulyevsasha.chartscontest.controls.OnLegendClicked
import ru.merkulyevsasha.chartscontest.models.ChartData
import ru.merkulyevsasha.chartscontest.models.ChartTypeEnum
import ru.merkulyevsasha.chartscontest.models.XValuesEnum
import ru.merkulyevsasha.chartscontest.sources.Example
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*


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
                examples.add(getAssetObjectFor(getAssetPath(index) + "overview.json"))
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
        for (index in 0 until charts.size) {
            val chartDataIndex = index + 1
            val oldChart = charts[index]
            oldChart.setData(chartData[index], object : OnLegendClicked {
                override fun onLegendClicked(point: ChartLegend.Distance) {
                    if (index == 4) {
                        oldChart.showPie()
                        return
                    }
                    val calendar = Calendar.getInstance()
                    calendar.time = point.xDate
                    try {
                        val path = getAssetPathAccordingTo(chartDataIndex, calendar)
                        val root = getAssetObjectFor(path)
                        val oldChartData = oldChart.getChartData()
                        if (!oldChartData.stacked && oldChartData.ys.first().type == ChartTypeEnum.BAR) {
                            val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
                            root.names.y0 = dateFormat.format(calendar.time)

                            calendar.add(Calendar.DAY_OF_MONTH, -1)
                            val path1 = getAssetPathAccordingTo(chartDataIndex, calendar)
                            val root1 = getAssetObjectFor(path1)
                            val xxx = root1.columns.subList(1, root1.columns.size)
                            xxx[0][0] = "y1"
                            root.names.y1 = dateFormat.format(calendar.time)
                            root.colors.y1 = "#558DED"
                            root.types.y1 = "line"
                            root.columns.addAll(xxx)

                            calendar.add(Calendar.DAY_OF_MONTH, 1)
                            calendar.add(Calendar.WEEK_OF_YEAR, -1)
                            val path2 = getAssetPathAccordingTo(chartDataIndex, calendar)
                            val root2 = getAssetObjectFor(path2)
                            val yyy = root2.columns.subList(1, root2.columns.size)
                            yyy[0][0] = "y2"
                            root.colors.y2 = "#5CBCDF"
                            root.names.y2 = dateFormat.format(calendar.time)
                            root.types.y2 = "line"
                            root.columns.addAll(yyy)
                            pres.dealWithIt(index, root)
                        } else {
                            pres.dealWithIt(index, root)
                        }
                    } catch (e: java.lang.Exception) {
                        e.printStackTrace()
                    }
                }
            })
        }
    }

    override fun updateChart(index: Int, chartData: ChartData) {
        chartData.xValuesIn = if (index == 3) XValuesEnum.X_MINUTES else XValuesEnum.X_HOURS
        charts[index].show(chartData)
    }

    private fun getAssetPath(index: Int): String {
        return "contest2/$index/"
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

    private fun getAssetObjectFor(path: String): Example {
        return convertToObject(readSource(path))
    }

    private fun getAssetPathAccordingTo(index: Int, calendar: Calendar): String {
        val year = calendar.get(Calendar.YEAR)
        var month = (calendar.get(Calendar.MONTH) + 1).toString()
        if (month.length < 2) month = "0$month"
        var day = calendar.get(Calendar.DAY_OF_MONTH).toString()
        if (day.length < 2) day = "0$day"
        val path = getAssetPath(index) + "$year-$month/$day.json"
        return path
    }

//    private fun convertToObject(json: String): List<Example> {
//        val gson = Gson()
//        val listType = object : TypeToken<List<Example>>() {}.type
//        val data: List<Example> = gson.fromJson(json, listType)
//        return data
//    }

}
