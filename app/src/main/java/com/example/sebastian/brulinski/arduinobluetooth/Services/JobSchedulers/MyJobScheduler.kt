package com.example.sebastian.brulinski.arduinobluetooth.Services.JobSchedulers

import android.annotation.SuppressLint
import android.app.job.JobParameters
import android.app.job.JobService
import android.widget.Toast
import com.example.sebastian.brulinski.arduinobluetooth.AsyncTasks.MyBluetoothJobExecutor


class MyJobScheduler : JobService() {

    private lateinit var mJobExecutor: MyBluetoothJobExecutor

    override fun onStopJob(p0: JobParameters?): Boolean {

        mJobExecutor = @SuppressLint("StaticFieldLeak")
        object : MyBluetoothJobExecutor() {

            override fun onPostExecute(result: String?) {
                Toast.makeText(applicationContext, result, Toast.LENGTH_SHORT).show()
                jobFinished(p0, false)
            }
        }

        mJobExecutor.execute()

        return false
    }

    override fun onStartJob(p0: JobParameters?): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}