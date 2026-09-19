package com.sbro.emucorea

import android.app.Application
import android.os.Build
import androidx.annotation.RequiresApi
import com.sbro.emucorea.core.CrashLogger
import com.sbro.emucorea.core.EmulatorBridge
import com.sbro.emucorea.discord.DiscordIntegration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class EmuCoreAApp : Application() {
    internal val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate() {
        super.onCreate()
        // The Discord SDK helper must not initialize the emulator-side application graph.
        if (getProcessName().endsWith(":discord")) return
        // CrashLogger must be the very first thing — it catches crashes in all subsequent init steps
        CrashLogger.init(this)
        EmulatorBridge.initializeOnce(this)
        DiscordIntegration.initialize(this)
    }
}
