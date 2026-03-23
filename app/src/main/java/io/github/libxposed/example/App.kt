package io.github.libxposed.example

import android.app.Application
import android.os.Handler
import android.os.Looper
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.concurrent.Volatile

class App : Application(), XposedServiceHelper.OnServiceListener {


    companion object {
        @Volatile
        var mService: XposedService? = null
        val serviceStateListeners =
            CopyOnWriteArraySet<ServiceStateListener>()
        val mainHandler: Handler = Handler(Looper.getMainLooper())
        private fun dispatchServiceState(
            listener: ServiceStateListener,
            service: XposedService?
        ) {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                listener.onServiceStateChanged(service)
                return
            }
            mainHandler.post {
                listener.onServiceStateChanged(
                    service
                )
            }
        }

        fun addServiceStateListener(
            listener: ServiceStateListener,
            notifyImmediately: Boolean
        ) {
            serviceStateListeners.add(listener)
            if (notifyImmediately) {
                dispatchServiceState(
                    listener,
                    mService
                )
            }
        }

        fun removeServiceStateListener(listener: ServiceStateListener) {
            serviceStateListeners.remove(listener)
        }
    }

    private fun notifyServiceStateChanged(service: XposedService?) {
        for (listener in serviceStateListeners) {
            dispatchServiceState(listener, service)
        }
    }

    override fun onCreate() {
        super.onCreate()
        XposedServiceHelper.registerListener(this)
    }

    interface ServiceStateListener {
        fun onServiceStateChanged(service: XposedService?)
    }

    override fun onServiceBind(service: XposedService) {
        mService = service
        notifyServiceStateChanged(mService)
    }

    override fun onServiceDied(service: XposedService) {
        mService = null
        notifyServiceStateChanged(mService)
    }
}
