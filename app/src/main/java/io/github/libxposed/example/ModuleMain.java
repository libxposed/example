package io.github.libxposed.example;

import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.Locale;

import io.github.libxposed.api.XposedModule;

public class ModuleMain extends XposedModule {
    static final String TAG = "ModuleMain";

    private boolean hasProp(long prop) {
        return (getFrameworkProperties() & prop) != 0;
    }

    @Override
    public void onModuleLoaded(@NonNull ModuleLoadedParam param) {
        log(Log.INFO, TAG, "onModuleLoaded: " + param.getProcessName());
        log(Log.INFO, TAG, String.format(Locale.getDefault(), "framework: %s (%s) API %d", getFrameworkName(), getFrameworkVersionCode(), getApiVersion()));
        log(Log.INFO, TAG, "system supported: " + hasProp(PROP_CAP_SYSTEM));
        log(Log.INFO, TAG, "remote supported: " + hasProp(PROP_CAP_REMOTE));
        log(Log.INFO, TAG, "api protection: " + hasProp(PROP_RT_API_PROTECTION));
    }

    @Override
    @RequiresApi(Build.VERSION_CODES.Q)
    public void onPackageLoaded(@NonNull PackageLoadedParam param) {
        log(Log.INFO, TAG, "onPackageLoaded: " + param.getPackageName());
        log(Log.INFO, TAG, "default classloader is " + param.getDefaultClassLoader());
    }

    @Override
    public void onPackageReady(@NonNull PackageReadyParam param) {
        try {
            var exampleClass = Class.forName("io.github.libxposed.example.Example", true, param.getClassLoader());
            var exampleMethod = exampleClass.getDeclaredMethod("method");
            var exampleConstructor = exampleClass.getDeclaredConstructor();

            hook(exampleMethod).intercept(chain -> {
                log(Log.INFO, TAG, "call the following chains with the same args");
                var result = (String) chain.proceed();

                log(Log.INFO, TAG, "call the following chains with different args");
                String old0 = (String) chain.getArg(0);
                Object new1 = new Object();
                var newArgs = new Object[]{old0, new1};
                result += (String) chain.proceed(newArgs);

                log(Log.INFO, TAG, "call the following chains with different this object");
                var newThis = new Object();
                result += (String) chain.proceedWith(newThis);
                result += (String) chain.proceedWith(newThis, newArgs);

                log(Log.INFO, TAG, "call the raw method");
                result += (String) getInvoker(exampleMethod).setType(Invoker.Type.ORIGIN).invoke(chain.getThisObject());

                return result;
            });

            hook(exampleMethod).intercept(chain -> {
                chain.proceed();
                // for void methods, it's needed to return null because Java doesn't support unit type
                return null;
            });

            hook(exampleConstructor)
                    .setPriority(PRIORITY_HIGHEST)
                    .setExceptionMode(ExceptionMode.PASSTHROUGH)
                    .intercept(chain -> {
                        log(Log.INFO, TAG, "thrown exception will be propagated to upper interceptors or the caller");
                        throw new RuntimeException("constructor hook exception");
                    });

            // call the original method
            getInvoker(exampleMethod).setType(Invoker.Type.ORIGIN).invoke(new Object());
            // call the special method starting from the middle of the hook chain
            getInvoker(exampleMethod).setType(new Invoker.Type.Chain(-50)).invokeSpecial(new Object());
            // create a new instance using the original constructor
            getInvoker(exampleConstructor).setType(Invoker.Type.ORIGIN).newInstance();
            // create a new special instance with full hook chain
            getInvoker(exampleConstructor).setType(Invoker.Type.Chain.FULL).newInstanceSpecial(exampleClass);
            // identical to the above line, default to call with full hook chain
            getInvoker(exampleConstructor).newInstanceSpecial(exampleClass);
        } catch (Throwable t) {
            log(Log.ERROR, TAG, "Error in onPackageLoaded", t);
        }
    }
}
