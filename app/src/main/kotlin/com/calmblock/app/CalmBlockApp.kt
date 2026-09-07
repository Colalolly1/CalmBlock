package com.calmblock.app

import android.app.Application
import com.calmblock.app.engine.Reconciler
import com.calmblock.app.ui.theme.Fonts
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CalmBlockApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Fonts.init(this)
        CoroutineScope(Dispatchers.IO).launch { Reconciler.run(this@CalmBlockApp) }
    }
}
