package io.github.libxposed.example

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import io.github.libxposed.api.XposedContext
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam
import io.github.libxposed.api.annotations.AfterInvocation
import io.github.libxposed.api.annotations.BeforeInvocation
import io.github.libxposed.api.annotations.XposedHooker
import java.lang.reflect.Member
import kotlin.random.Random

private lateinit var module: ModuleMain

class ModuleMain(base: XposedContext, param: ModuleLoadedParam) : XposedModule(base, param) {

    init {
        log("ModuleMain at " + param.processName)
        module = this
    }

    @XposedHooker
    class MyHooker(private val magic: Int) : XposedInterface.Hooker {
        companion object {
            @JvmStatic
            @BeforeInvocation
            fun beforeInvocation(method: Member, thisObject: Any?, args: Array<out Any>): XposedInterface.Hooker {
                val key = Random.nextInt()
                val appContext = args[0] as Context
                module.log("beforeInvocation: key = $key")
                module.log("app context: $appContext")
                module.log("xposed context: $this")
                return MyHooker(key)
            }

            @JvmStatic
            @AfterInvocation
            fun afterInvocation(extras: XposedInterface.Hooker, result: Any?) {
                val hooker = extras as MyHooker
                module.log("afterInvocation: key = ${hooker.magic}")
            }
        }
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
        hook(exampleMethod, MyHooker::class.java)
    }
}
