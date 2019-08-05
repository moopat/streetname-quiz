package at.trycatch.streets.service

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.util.Log

class ImportService : IntentService("ImportService") {


    override fun onHandleIntent(intent: Intent?) {
        handleImport()
    }

    private fun handleImport() {
        Log.d(TAG, "Starting import.")
    }

    companion object {

        private const val TAG = "ImportService"

        @JvmStatic
        fun startImport(context: Context) {
            context.startService(Intent(context, ImportService::class.java))
        }

    }
}
