package io.github.ziginsider.epam_laba_12

import android.annotation.TargetApi
import android.app.job.JobParameters
import android.app.job.JobService
import android.os.Build
import io.github.ziginsider.epam_laba_12.download.DOWNLOADED_FILE_NAME
import io.github.ziginsider.epam_laba_12.download.RETROFIT_BASE_URL
import io.github.ziginsider.epam_laba_12.download.RETROFIT_GET_REQUEST

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class JobSchedulerService: JobService() {
    override fun onStartJob(params: JobParameters?): Boolean {
        ScheduledService.startScheduledJob(applicationContext, RETROFIT_BASE_URL,
                RETROFIT_GET_REQUEST, DOWNLOADED_FILE_NAME)
        return false
    }

    override fun onStopJob(params: JobParameters?) = false
}