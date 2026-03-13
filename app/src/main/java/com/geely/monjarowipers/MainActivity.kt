package com.geely.monjarowipers

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Bundle
import android.widget.ImageView
import android.widget.RemoteViews
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.geely.os.car.ConnectionListener
import com.geely.os.car.GlyCar
import com.geely.os.car.IGlyCar

class MainActivity : AppCompatActivity() {

    private lateinit var wiperSwitch: Switch
    private lateinit var statusText: TextView
    private lateinit var wiperImage: ImageView

    private var car: IGlyCar? = null
    private var isUpdatingSwitch = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        wiperSwitch = findViewById(R.id.wiperSwitch)
        statusText = findViewById(R.id.statusText)
        wiperImage = findViewById(R.id.wiperImage)

        wiperSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingSwitch) {
                setWiperServiceMode(if (isChecked) 1 else 0)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        connectCar()
    }

    override fun onPause() {
        super.onPause()
        car?.disconnect()
        car = null
    }

    private fun connectCar() {
        car = GlyCar.create(this, object : ConnectionListener {
            override fun onConnected() {
                readAndUpdateUi()
            }

            override fun onDisConnected() {}
        })
    }

    private fun readAndUpdateUi() {
        val state = car?.getIntProperty(PROP_WINDSCREEN_SERVICE_POSITION, AREA_FRONT_WIPER) ?: return
        runOnUiThread {
            isUpdatingSwitch = true
            wiperSwitch.isChecked = state == 1
            updateStatusText(state == 1)
            isUpdatingSwitch = false
        }
    }

    private fun setWiperServiceMode(value: Int) {
        car?.setIntProperty(PROP_WINDSCREEN_SERVICE_POSITION, AREA_FRONT_WIPER, value)
        val active = value == 1
        updateStatusText(active)
        updateWidget(active)
    }

    private fun updateWidget(active: Boolean) {
        val manager = AppWidgetManager.getInstance(this)
        val ids = manager.getAppWidgetIds(ComponentName(this, WiperServiceWidgetProvider::class.java))
        if (ids.isEmpty()) return
        val iconRes = if (active) R.drawable.ic_wiper_on else R.drawable.ic_wiper_off
        for (id in ids) {
            val views = RemoteViews(packageName, R.layout.widget_wiper_service)
            views.setImageViewResource(R.id.wiperServiceIcon, iconRes)
            manager.updateAppWidget(id, views)
        }
    }

    private fun updateStatusText(active: Boolean) {
        statusText.setText(if (active) R.string.wiper_service_on else R.string.wiper_service_off)
        wiperImage.setImageResource(if (active) R.drawable.ic_wiper_on else R.drawable.ic_wiper_off)
    }

    companion object {
        private const val PROP_WINDSCREEN_SERVICE_POSITION = 0x200c0100
        private const val AREA_FRONT_WIPER = 1
    }
}
