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
import java.io.FileWriter
import kotlin.random.Random

@SuppressLint("SetTextI18n")
class MainActivity : Activity(), App.ServiceStateListener {
    private var mService: XposedService? = null
    private var binding: ActivityMainBinding? = null

    private val mCallback = object : OnScopeEventListener {
        override fun onScopeRequestApproved(approved: List<String>) {
            runOnUiThread {
                Toast.makeText(
                    this@MainActivity,
                    "onScopeRequestApproved: $approved",
                    Toast.LENGTH_SHORT
                ).show()
                binding?.scope?.text = "Scope: " + mService?.scope
            }
        }

        override fun onScopeRequestFailed(message: String) {
            runOnUiThread {
                Toast.makeText(
                    this@MainActivity,
                    "onScopeRequestFailed: $message",
                    Toast.LENGTH_SHORT
                ).show()
                binding?.scope?.text = "Scope: " + mService?.scope
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        binding?.let {
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
        val handler = Handler(Looper.getMainLooper())
        binding?.let {
            handler.post {
                it.binder.text = "Binder acquired"
                it.api.text = "API " + service?.apiVersion
                it.framework.text = "Framework " + service?.frameworkName
                it.frameworkVersion.text = "Framework version " + service?.frameworkVersion
                it.frameworkVersionCode.text =
                    "Framework version code " + service?.frameworkVersionCode
                val cap = service?.frameworkProperties
                val capStringList = mutableListOf<String>()
                cap?.and(XposedService.PROP_CAP_SYSTEM)?.let {
                    capStringList.add("PROP_CAP_SYSTEM")
                }
                cap?.and(XposedService.PROP_CAP_REMOTE)?.let {
                    capStringList.add("PROP_CAP_REMOTE")
                }
                cap?.and(XposedService.PROP_RT_API_PROTECTION)?.let {
                    capStringList.add("PROP_RT_API_PROTECTION")
                }
                it.frameworkProperties.text =
                    "Framework properties: $capStringList"
                it.scope.text = "Scope: " + service?.scope

                it.requestScope.setOnClickListener {
                    service?.requestScope(listOf("com.android.settings"), mCallback)
                }
                it.randomPrefs.setOnClickListener {
                    val prefs = service?.getRemotePreferences("test")
                    val old = prefs?.getInt("test", -1)
                    val new = Random.nextInt()
                    Toast.makeText(this@MainActivity, "$old -> $new", Toast.LENGTH_SHORT).show()
                    prefs?.edit()?.putInt("test", new)?.apply()
                }
                it.remoteFile.setOnClickListener {
                    service?.openRemoteFile("test.txt").use { pfd ->
                        pfd?.let { fileDescriptor ->
                            FileWriter(fileDescriptor.fileDescriptor).use { writer ->
                                writer.append("Hello World!")
                            }
                        }
                    }
                }
                if (service == null) {
                    it.let { binding -> binding.binder.text = "Binder is null" }
                }
            }
        }
    }
}
