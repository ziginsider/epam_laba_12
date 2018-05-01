package io.github.ziginsider.epam_laba_12

import android.annotation.TargetApi
import android.app.job.JobParameters
import android.app.job.JobService
import android.os.Build
import io.github.ziginsider.epam_laba_12.download.DOWNLOADED_FILE_NAME
import io.github.ziginsider.epam_laba_12.download.RETROFIT_BASE_URL
import io.github.ziginsider.epam_laba_12.download.RETROFIT_GET_REQUEST
import android.content.Context.JOB_SCHEDULER_SERVICE
import android.app.job.JobScheduler
import android.content.Context
import android.util.Log


const val JOB_START_COUNT_TIMES = 5

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class JobSchedulerService: JobService() {
    private var counter = 0

    override fun onStartJob(params: JobParameters?): Boolean {
        if (++counter == JOB_START_COUNT_TIMES + 1) {
            Log.d("TAG", "[JOB SCHEDULER CANCEL]")
            val jobScheduler = this.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            jobScheduler.cancelAll()
            return false
        } else {
            Log.d("TAG", "[JOB SCHEDULER onStartJob() run $counter times]")
            ScheduledService.startScheduledJob(applicationContext, RETROFIT_BASE_URL,
                    RETROFIT_GET_REQUEST, DOWNLOADED_FILE_NAME)
            return true
        }
    }

    override fun onStopJob(params: JobParameters?) = false
}