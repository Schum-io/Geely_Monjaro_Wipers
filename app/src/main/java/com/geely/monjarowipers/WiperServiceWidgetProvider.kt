package com.geely.monjarowipers

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.widget.RemoteViews
import com.geely.os.car.ConnectionListener
import com.geely.os.car.GlyCar
import com.geely.os.car.IGlyCar

class WiperServiceWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_wiper_service)

            val intent = Intent(context, WiperServiceWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE_WIPER_SERVICE
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.wiperServiceIcon, pendingIntent)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        updateWiperServiceState(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == ACTION_TOGGLE_WIPER_SERVICE) {
            val pendingResult = goAsync()
            withCar(context, onDone = { pendingResult.finish() }) { car ->
                val current = car.getIntProperty(PROP_WINDSCREEN_SERVICE_POSITION, AREA_FRONT_WIPER)
                val next = if (current == 0) 1 else 0
                car.setIntProperty(PROP_WINDSCREEN_SERVICE_POSITION, AREA_FRONT_WIPER, next)
                updateWidgetIcon(context, next == 1)
            }
        }
    }

    private fun updateWidgetIcon(context: Context, active: Boolean) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val thisWidget = ComponentName(context, WiperServiceWidgetProvider::class.java)
        val iconRes = if (active) R.drawable.ic_wiper_on else R.drawable.ic_wiper_off
        val bitmap = drawableToBitmap(context.getDrawable(iconRes)!!)
        for (id in appWidgetManager.getAppWidgetIds(thisWidget)) {
            val views = RemoteViews(context.packageName, R.layout.widget_wiper_service)
            views.setImageViewBitmap(R.id.wiperServiceIcon, bitmap)
            val intent = Intent(context, WiperServiceWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE_WIPER_SERVICE
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.wiperServiceIcon, pendingIntent)
            appWidgetManager.updateAppWidget(id, views)
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        val size = 192
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)
        return bitmap
    }

    /**
     * Читает актуальное состояние и обновляет внешний вид виджета.
     */
    private fun updateWiperServiceState(context: Context) {
        withCar(context, onDone = {}) { car ->
            val state = car.getIntProperty(PROP_WINDSCREEN_SERVICE_POSITION, AREA_FRONT_WIPER)
            updateWidgetIcon(context, state == 1)
        }
    }

    /**
     * Создаёт подключение к GlyCar, выполняет [block] после установки соединения,
     * отключается и вызывает [onDone]. Гарантирует что [onDone] вызывается ровно один раз.
     */
    private fun withCar(context: Context, onDone: () -> Unit, block: (IGlyCar) -> Unit) {
        val finished = java.util.concurrent.atomic.AtomicBoolean(false)
        val finish = { if (finished.compareAndSet(false, true)) onDone() }
        var car: IGlyCar? = null
        car = GlyCar.create(context.applicationContext, object : ConnectionListener {
            override fun onConnected() {
                try {
                    block(car!!)
                } catch (_: Exception) {
                } finally {
                    car?.disconnect()
                    finish()
                }
            }

            override fun onDisConnected() {
                finish()
            }
        })
    }

    companion object {
        private const val ACTION_TOGGLE_WIPER_SERVICE =
            "com.geely.monjarowipers.action.TOGGLE_WIPER_SERVICE"

        /** GlyCarPropertyIds.SETTING_FUNC_WINDSCREEN_SERVICE_POSITION */
        private const val PROP_WINDSCREEN_SERVICE_POSITION = 0x200c0100
        private const val AREA_FRONT_WIPER = 1
    }
}
