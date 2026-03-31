package com.wizpizz.wifiauto.ui.activity.base

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.wizpizz.wifiauto.utils.LocaleUtils

abstract class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences(LocaleUtils.PREFS_NAME, Context.MODE_PRIVATE)
        val lang = prefs.getString(LocaleUtils.PREF_UI_LANGUAGE, "")
        super.attachBaseContext(LocaleUtils.wrapContext(newBase, lang))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        onCreate()
    }

    abstract fun onCreate()
}
