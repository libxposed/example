package io.github.libxposed.example

import android.util.Log
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam
import java.io.FileNotFoundException
import java.io.FileReader

class ModuleMainKt : XposedModule() {
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

        val exampleClass = Class.forName("io.github.libxposed.example.Example", true, param.classLoader)
        val exampleMethod = exampleClass.getDeclaredMethod("method")
        val exampleConstructor = exampleClass.getDeclaredConstructor()

        hook(exampleMethod) { chain ->
            log(Log.INFO, TAG, "call the following chains with the same args")
            var result = chain.proceed() as String

            log(Log.INFO, TAG, "call the following chains with different args")
            // you can pass the args one by one
            val old0 = chain.getArg<String>(0)
            val new1 = Any()
            result += chain.proceed(old0, new1) as String
            // or use an array to pass all the args
            val newArgs = chain.args.toTypedArray()
            newArgs[1] = new1
            result += chain.proceed(*newArgs) as String

            log(Log.INFO, TAG, "call the following chains with different this object")
            val newThis = Any()
            result += chain.proceedWith(newThis) as String
            result += chain.proceedWith(newThis, old0, new1) as String
            result += chain.proceedWith(newThis, *newArgs) as String

            log(Log.INFO, TAG, "call the raw method")
            result += getInvoker(chain.executable, null).invoke(chain.thisObject) as String

            log(Log.INFO, TAG, "returned value will pass to higher priority chains")
            result
        }

        hook(exampleConstructor, PRIORITY_HIGHEST) { chain ->
            log(Log.INFO, TAG, "thrown exception will be propagated to upper interceptors or the caller")
            throw RuntimeException("constructor hook exception")
        }

        getInvoker(exampleMethod, null).invoke(Any())
        getInvoker(exampleMethod, PRIORITY_DEFAULT).invokeSpecial(Any())
        getInvoker(exampleConstructor, null).newInstance()
        getInvoker(exampleConstructor, PRIORITY_DEFAULT).newInstanceSpecial(exampleClass)
    }
}
