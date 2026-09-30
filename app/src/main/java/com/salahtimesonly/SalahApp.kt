package com.salahtimesonly

import android.app.Application
import com.salahtimesonly.schedule.AlarmScheduler
import com.salahtimesonly.schedule.Channels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SalahApp : Application() {
    /** Outlives activities, so settings writes survive a language-change recreate. */
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        Channels.ensure(this)
        AlarmScheduler.ensureWorker(this)
        scope.launch { AlarmScheduler.refreshAll(this@SalahApp) }
    }
}
