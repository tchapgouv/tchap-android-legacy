package im.vector.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import im.vector.util.CallsManager
import org.matrix.androidsdk.core.Log

class CallHeadsUpActionReceiver : BroadcastReceiver() {
    private val LOG_TAG = CallHeadsUpActionReceiver::class.java.simpleName

    companion object {
        const val EXTRA_CALL_ACTION_KEY = "EXTRA_CALL_ACTION_KEY"
        const val CALL_ACTION_REJECT = 0
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val callsManager = CallsManager.getSharedInstance()

        when (intent?.getIntExtra(EXTRA_CALL_ACTION_KEY, 0)) {
            CALL_ACTION_REJECT -> onCallRejectClicked(callsManager)
        }
    }

    private fun onCallRejectClicked(callManager: CallsManager) {
        Log.d(LOG_TAG,"onCallRejectClicked")
        callManager.rejectCall()
    }
}