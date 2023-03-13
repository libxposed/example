package io.github.libxposed.example

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import io.github.libxposed.api.XposedContext
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam

class ModuleMain(base: XposedContext, param: ModuleLoadedParam) : XposedModule(base, param) {

    init {
        log("ModuleMain at " + param.processName)
    }

    @SuppressLint("DiscouragedPrivateApi")
    override fun onPackageLoaded(param: PackageLoadedParam) {
        super.onPackageLoaded(param)
        log("onPackageLoaded: " + param.packageName)
        log("main classloader is " + this.classLoader)
        log("param classloader is " + param.classLoader)
        log("module apk path: " + this.packageCodePath)
        log("----------")

        if (!param.isFirstPackage) return

        val exampleMethod = Application::class.java.getDeclaredMethod("attach", Context::class.java)
        hookBefore(exampleMethod) { callback ->
            val appContext = callback.args[0] as Context
            log("app context: $appContext")
            log("xposed context: $this")
        }
    }
}
