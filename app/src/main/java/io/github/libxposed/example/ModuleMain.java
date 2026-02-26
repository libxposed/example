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

            hook(exampleMethod).intercept(chain -> {
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
                result += (String) getInvoker(chain.getExecutable(), Invoker.Type.ORIGIN).invoke(chain.getThisObject());

                return result;
            });

            hook(exampleMethod).intercept(chain -> {
                chain.proceed();
                // for void methods, it's identical to return anything or no return statement
                // return null;
            });

            hook(exampleConstructor)
                    .setPriority(PRIORITY_HIGHEST)
                    .intercept(chain -> {
                        log(Log.INFO, TAG, "thrown exception will be propagated to upper interceptors or the caller");
                        throw new RuntimeException("constructor hook exception");
                    });

            // call the original method
            getInvoker(exampleMethod, Invoker.Type.ORIGIN).invoke(new Object());
            // call the special method starting from the middle of the hook chain
            getInvoker(exampleMethod, new Invoker.Type.Chain(-50)).invokeSpecial(new Object());
            // create a new instance using the original constructor
            getInvoker(exampleConstructor, Invoker.Type.ORIGIN).newInstance();
            // create a new special instance with full hook chain
            getInvoker(exampleConstructor, Invoker.Type.Chain.FULL).newInstanceSpecial(exampleClass);
            // identical to the above line, default to call with full hook chain
            getInvoker(exampleConstructor).newInstanceSpecial(exampleClass);
        } catch (Throwable t) {
            log(Log.ERROR, TAG, "Error in onPackageLoaded", t);
        }
    }
}
