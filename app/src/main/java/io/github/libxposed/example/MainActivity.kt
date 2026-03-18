package io.github.libxposed.example

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
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
        override fun onScopeRequestApproved(approved: List<String>) {
            runOnUiThread {
                Toast.makeText(this@MainActivity, "onScopeRequestApproved: $approved", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onScopeRequestFailed(message: String) {
            runOnUiThread {
                Toast.makeText(this@MainActivity, "onScopeRequestFailed: $message", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val handler = Handler(Looper.getMainLooper())
        binding.binder.text = "Loading"
        XposedServiceHelper.registerListener(object : XposedServiceHelper.OnServiceListener {
            override fun onServiceBind(service: XposedService) {
                mService = service
                handler.post {
                    binding.binder.text = "Binder acquired"
                    binding.api.text = "API " + service.apiVersion
                    binding.framework.text = "Framework " + service.frameworkName
                    binding.frameworkVersion.text = "Framework version " + service.frameworkVersion
                    binding.frameworkVersionCode.text = "Framework version code " + service.frameworkVersionCode
                    binding.frameworkProperties.text = "Framework properties: " + service.frameworkProperties.toHexString()
                    binding.scope.text = "Scope: " + service.scope
                }

                binding.requestScope.setOnClickListener {
                    service.requestScope(listOf("com.android.settings"), mCallback)
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


        handler.postDelayed({
            if (mService == null) {
                binding.binder.text = "Binder is null"
            }
        }, 5000)
    }
}
