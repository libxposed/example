package io.github.libxposed.example

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import io.github.libxposed.example.databinding.ActivityMainBinding
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedService.OnScopeEventListener
import io.github.libxposed.service.XposedServiceHelper
import java.io.FileWriter
import kotlin.random.Random

class MainActivity : Activity() {

    private var mService: XposedService? = null

    private val mCallback = object : OnScopeEventListener {
        override fun onScopeRequestPrompted(packageName: String) {
            runOnUiThread {
                Toast.makeText(this@MainActivity, "onScopeRequestPrompted: $packageName", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onScopeRequestApproved(packageName: String) {
            runOnUiThread {
                Toast.makeText(this@MainActivity, "onScopeRequestApproved: $packageName", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onScopeRequestDenied(packageName: String) {
            runOnUiThread {
                Toast.makeText(this@MainActivity, "onScopeRequestDenied: $packageName", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onScopeRequestTimeout(packageName: String) {
            runOnUiThread {
                Toast.makeText(this@MainActivity, "onScopeRequestTimeout: $packageName", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onScopeRequestFailed(packageName: String, message: String) {
            runOnUiThread {
                Toast.makeText(this@MainActivity, "onScopeRequestFailed: $packageName, $message", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.binder.text = "Loading"
        XposedServiceHelper.registerListener(object : XposedServiceHelper.OnServiceListener {
            override fun onServiceBind(service: XposedService) {
                mService = service
                binding.binder.text = "Binder acquired"
                binding.api.text = "API " + service.apiVersion
                binding.framework.text = "Framework " + service.frameworkName
                binding.frameworkVersion.text = "Framework version " + service.frameworkVersion
                binding.frameworkVersionCode.text = "Framework version code " + service.frameworkVersionCode
                binding.scope.text = "Scope: " + service.scope

                binding.requestScope.setOnClickListener {
                    service.requestScope("com.android.settings", mCallback)
                }
                binding.randomPrefs.setOnClickListener {
                    val prefs = service.getRemotePreferences("test")
                    val old = prefs.getInt("test", -1)
                    val new = Random.nextInt()
                    Toast.makeText(this@MainActivity, "$old -> $new", Toast.LENGTH_SHORT).show()
                    prefs.edit().putInt("test", new).apply()
                }
                binding.remoteFile.setOnClickListener {
                    service.openRemoteFile("test.txt").use { pfd ->
                        FileWriter(pfd.fileDescriptor).use {
                            it.append("Hello World!")
                        }
                    }
                }
            }

            override fun onServiceDied(service: XposedService) {
            }
        })

        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            if (mService == null) {
                binding.binder.text = "Binder is null"
            }
        }, 5000)
    }
}
