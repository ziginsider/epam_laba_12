package io.github.ziginsider.epam_laba_12

import android.annotation.TargetApi
import android.app.job.JobParameters
import android.app.job.JobService
import android.os.Build

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class JobSchedulerService: JobService() {
    override fun onStartJob(params: JobParameters?): Boolean {

        return false
    }

    override fun onStopJob(params: JobParameters?) = false
}