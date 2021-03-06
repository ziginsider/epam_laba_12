package io.github.ziginsider.epam_laba_12

import android.annotation.TargetApi
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobService
import android.os.Build
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.util.Log
import android.preference.PreferenceManager
import io.github.ziginsider.epam_laba_12.download.Contract.DOWNLOADED_FILE_NAME
import io.github.ziginsider.epam_laba_12.download.Contract.RETROFIT_BASE_URL
import io.github.ziginsider.epam_laba_12.download.Contract.RETROFIT_GET_REQUEST

/**
 * Implementation Job Scheduler. Runs file downloading every 2 hours with showing notification and
 * progress of download. Repeats this downloading five times.
 *
 * @since 2018-04-30
 * @author Alex Kisel
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class JobSchedulerService : JobService() {

    private var counter: Int
        get() = PreferenceManager.getDefaultSharedPreferences(this)
                .getInt(JOB_COUNTER, 0)

        set(value) = PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putInt(JOB_COUNTER, value)
                .apply()


    override fun onStartJob(params: JobParameters?): Boolean {
        if (++counter > JOB_START_COUNT_TIMES) {
            Log.d("TAG", "[JOB SCHEDULER CANCEL]")
            counter = 0
            JobSchedulerService.cancel(this)
        } else {
            Log.d("TAG", "[JOB SCHEDULER onStartJob() run $counter times]")
            ScheduledService.startScheduledJob(applicationContext, RETROFIT_BASE_URL,
                    RETROFIT_GET_REQUEST, DOWNLOADED_FILE_NAME)
        }
        return true
    }

    override fun onStopJob(params: JobParameters?) = false

    companion object {

        const val JOB_START_COUNT_TIMES = 5
        const val JOB_ID = 13
        const val JOB_COUNTER = "jobCounter"

        /**
         * Runs Job Scheduler with a periodic interval
         *
         * @param intervalMillis the interval in milliseconds to repeat the job
         */
        fun schedule(context: Context, intervalMillis: Long) {
            val serviceComponent = ComponentName(context, JobSchedulerService::class.java)
            val infoBuilder = JobInfo.Builder(JOB_ID, serviceComponent)
            infoBuilder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
                    .setPeriodic(intervalMillis)
            val info = infoBuilder.build()
            val scheduler = context.getSystemService(android.content.Context.JOB_SCHEDULER_SERVICE)
                    as JobScheduler
            scheduler.schedule(info)
        }

        /**
         * Cancel current Job
         */
        fun cancel(context: Context) {
            val jobScheduler
                    = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            jobScheduler.cancel(JOB_ID)
        }
    }
}
