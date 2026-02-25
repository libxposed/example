package io.github.libxposed.example;

import android.util.Log;

import androidx.annotation.NonNull;

import io.github.libxposed.api.XposedModule;

public class ModuleMain extends XposedModule {
    static final String TAG = "ModuleMain";

    @Override
    public void onPackageLoaded(@NonNull PackageLoadedParam param) {
        try {
            var exampleClass = Class.forName("io.github.libxposed.example.Example", true, param.getClassLoader());
            var exampleMethod = exampleClass.getDeclaredMethod("method");
            var exampleConstructor = exampleClass.getDeclaredConstructor();

            hook(exampleMethod, (chain) -> {
                log(Log.INFO, TAG, "call the following chains with the same args");
                var result = (String) chain.proceed();

                log(Log.INFO, TAG, "call the following chains with different args");
                // you can pass the args one by one
                String old0 = chain.getArg(0);
                Object new1 = new Object();
                result += (String) chain.proceed(old0, new1);
                // or use an array to pass all the args
                var newArgs = chain.getArgs().toArray();
                newArgs[1] = new1;
                result += (String) chain.proceed(newArgs);

                log(Log.INFO, TAG, "call the following chains with different this object");
                var newThis = new Object();
                result += (String) chain.proceedWith(newThis);
                result += (String) chain.proceedWith(newThis, old0, new1);
                result += (String) chain.proceedWith(newThis, newArgs);

                log(Log.INFO, TAG, "call the raw method");
                result += (String) getInvoker(chain.getExecutable(), null).invoke(chain.getThisObject());

                return result;
            });

            hook(exampleConstructor, PRIORITY_HIGHEST, (chain) -> {
                log(Log.INFO, TAG, "thrown exception will be propagated to upper interceptors or the caller");
                throw new RuntimeException("constructor hook exception");
            });

            getInvoker(exampleMethod, null).invoke(new Object());
            getInvoker(exampleMethod, PRIORITY_DEFAULT).invokeSpecial(new Object());
            getInvoker(exampleConstructor, null).newInstance();
            getInvoker(exampleConstructor, PRIORITY_DEFAULT).newInstanceSpecial(exampleClass);
        } catch (Throwable t) {
            log(Log.ERROR, TAG, "Error in onPackageLoaded", t);
        }
    }
}
