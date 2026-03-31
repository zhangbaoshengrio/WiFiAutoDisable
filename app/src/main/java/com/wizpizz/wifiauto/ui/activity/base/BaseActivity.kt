@file:Suppress("MemberVisibilityCanBePrivate", "UNCHECKED_CAST")

package com.wizpizz.wifiauto.ui.activity.base

import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.WindowCompat
import androidx.viewbinding.ViewBinding
import com.highcapable.yukihookapi.hook.factory.current
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.type.android.LayoutInflaterClass
import com.wizpizz.wifiauto.R

abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity() {

    lateinit var binding: VB

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = current().generic()?.argument()?.method {
            name = "inflate"
            param(LayoutInflaterClass)
        }?.get()?.invoke<VB>(layoutInflater) ?: error("ViewBinding inflate failed")
        setContentView(binding.root)
        supportActionBar?.hide()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            val isLight = !resources.configuration.isNightModeActive
            isAppearanceLightStatusBars = isLight
            isAppearanceLightNavigationBars = isLight
        }
        ResourcesCompat.getColor(resources, R.color.colorThemeBackground, null).also {
            window?.statusBarColor = it
            window?.navigationBarColor = it
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) window?.navigationBarDividerColor = it
        }
        onCreate()
    }

    abstract fun onCreate()
}
