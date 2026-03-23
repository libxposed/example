package io.github.libxposed.example

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import io.github.libxposed.example.databinding.ActivityMainBinding
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedService.OnScopeEventListener
import java.io.FileWriter
import kotlin.random.Random

@SuppressLint("SetTextI18n")
class MainActivity : Activity(), App.ServiceStateListener {
    private var mService: XposedService? = null
    private lateinit var binding: ActivityMainBinding

    private val mCallback = object : OnScopeEventListener {
        override fun onScopeRequestApproved(approved: List<String>) {
            runOnUiThread {
                Toast.makeText(
                    this@MainActivity,
                    "onScopeRequestApproved: $approved",
                    Toast.LENGTH_SHORT
                ).show()
                binding.scope.text = "Scope: " + mService?.scope
            }
        }

        override fun onScopeRequestFailed(message: String) {
            runOnUiThread {
                Toast.makeText(
                    this@MainActivity,
                    "onScopeRequestFailed: $message",
                    Toast.LENGTH_SHORT
                ).show()
                binding.scope.text = "Scope: " + mService?.scope
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        binding.let {
            setContentView(it.root)
            it.binder.text = "Loading"
        }
    }

    override fun onStart() {
        super.onStart()
        App.addServiceStateListener(this, true)
    }

    override fun onStop() {
        App.removeServiceStateListener(this)
        super.onStop()
    }

    override fun onServiceStateChanged(service: XposedService?) {
        mService = service
        runOnUiThread {
            if (service == null) {
                binding.binder.text = "Binder is null"
            } else {
                binding.binder.text = "Binder acquired"
                binding.api.text = "API " + service.apiVersion
                binding.framework.text = "Framework " + service.frameworkName
                binding.frameworkVersion.text = "Framework version " + service.frameworkVersion
                binding.frameworkVersionCode.text =
                    "Framework version code " + service.frameworkVersionCode
                val cap = service.frameworkProperties
                val capStringList = mutableListOf<String>()
                if (cap.and(XposedService.PROP_CAP_SYSTEM) != 0L) {
                    capStringList.add("PROP_CAP_SYSTEM")
                }
                if (cap.and(XposedService.PROP_CAP_REMOTE) != 0L) {
                    capStringList.add("PROP_CAP_REMOTE")
                }
                if (cap.and(XposedService.PROP_RT_API_PROTECTION) != 0L) {
                    capStringList.add("PROP_RT_API_PROTECTION")
                }
                binding.frameworkProperties.text =
                    "Framework properties: $capStringList"
                binding.scope.text = "Scope: " + service.scope

                binding.requestScope.setOnClickListener {
                    service.requestScope(listOf("com.android.settings"), mCallback)
                }
                binding.randomPrefs.setOnClickListener {
                    val prefs = service.getRemotePreferences("test")
                    val old = prefs.getInt("test", -1)
                    val new = Random.nextInt()
                    Toast.makeText(this@MainActivity, "$old -> $new", Toast.LENGTH_SHORT).show()
                    prefs.edit()?.putInt("test", new)?.apply()
                }
                binding.remoteFile.setOnClickListener {
                    service.openRemoteFile("test.txt").use { pfd ->
                        FileWriter(pfd.fileDescriptor).use {
                            it.append("Hello World!")
                        }
                    }
                }
            }
        }
    }
}
