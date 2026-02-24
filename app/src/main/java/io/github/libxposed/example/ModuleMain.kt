package io.github.libxposed.example

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.util.Log
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam
import java.io.FileNotFoundException
import java.io.FileReader
import java.lang.reflect.Method

class ModuleMain : XposedModule() {
    companion object {
        const val TAG = "XposedExample"
    }

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        log(Log.INFO, TAG, "onModuleLoaded: " + param.processName)
        log(Log.INFO, TAG, "framework: $frameworkName($frameworkVersionCode) API $apiVersion")

        val hasCap: (Long) -> Boolean = { cap -> frameworkCapabilities.and(cap) != 0L }
        log(Log.INFO, TAG, "system supported: " + hasCap(CAP_SYSTEM))
        log(Log.INFO, TAG, "remote supported: " + hasCap(CAP_REMOTE))
        log(Log.INFO, TAG, "dynamic code api call supported: " + hasCap(CAP_RT_DYNAMIC_CODE_API_ACCESS))
    }

    override fun onPackageLoaded(param: PackageLoadedParam) {
        log(Log.INFO, TAG, "onPackageLoaded: " + param.packageName)
        log(Log.INFO, TAG, "param classloader is " + param.classLoader)
        log(Log.INFO, TAG, "module apk path: " + this.applicationInfo.sourceDir)
        log(Log.INFO, TAG, "----------")

        if (!param.isFirstPackage) return

        val prefs = getRemotePreferences("test")
        log(Log.INFO, TAG, "remote prefs: " + prefs.getInt("test", -1))
        prefs.registerOnSharedPreferenceChangeListener { _, key ->
            val value = prefs.getInt(key, 0)
            log(Log.INFO, TAG, "onSharedPreferenceChanged: $key->$value")
        }

        try {
            val text = openRemoteFile("test.txt").use {
                FileReader(it.fileDescriptor).readText()
            }
            log(Log.INFO, TAG, "remote file content: $text")
        } catch (e: FileNotFoundException) {
            log(Log.INFO, TAG, "remote file not found")
        }

        @SuppressLint("DiscouragedPrivateApi")
        val exampleMethod = Application::class.java.getDeclaredMethod("attach", Context::class.java)

        hook(exampleMethod, object : XposedInterface.SimpleHooker<Method> {
            override fun before(callback: XposedInterface.BeforeHookCallback<Method>) {
                log(Log.INFO, TAG, "before Application.attach")
            }
        })
    }
}
