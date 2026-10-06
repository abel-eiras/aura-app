package io.github.abeleiras.aura.recording

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat

/**
 * Reports call start/end so the recording can auto-pause (FR-001-06). Does nothing
 * without READ_PHONE_STATE: that permission is optional and recording works without it.
 */
class CallStateMonitor(private val context: Context) {
    private val telephonyManager = context.getSystemService(TelephonyManager::class.java)
    private var legacyListener: PhoneStateListener? = null
    // Typed as Any so this class doesn't reference a class that only exists from API 31.
    private var modernCallback: Any? = null

    @SuppressLint("MissingPermission") // guarded by the checkSelfPermission below
    fun start(onCallActive: () -> Unit, onCallEnded: () -> Unit) {
        val manager = telephonyManager ?: return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                override fun onCallStateChanged(state: Int) = dispatch(state, onCallActive, onCallEnded)
            }
            modernCallback = callback
            manager.registerTelephonyCallback(context.mainExecutor, callback)
        } else {
            @Suppress("DEPRECATION")
            val listener = object : PhoneStateListener() {
                @Deprecated("Deprecated in Java")
                override fun onCallStateChanged(state: Int, phoneNumber: String?) =
                    dispatch(state, onCallActive, onCallEnded)
            }
            legacyListener = listener
            @Suppress("DEPRECATION")
            manager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
        }
    }

    fun stop() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (modernCallback as? TelephonyCallback)?.let { telephonyManager?.unregisterTelephonyCallback(it) }
        }
        modernCallback = null
        legacyListener?.let {
            @Suppress("DEPRECATION")
            telephonyManager?.listen(it, PhoneStateListener.LISTEN_NONE)
        }
        legacyListener = null
    }

    private fun dispatch(state: Int, onCallActive: () -> Unit, onCallEnded: () -> Unit) {
        when (state) {
            TelephonyManager.CALL_STATE_RINGING, TelephonyManager.CALL_STATE_OFFHOOK -> onCallActive()
            TelephonyManager.CALL_STATE_IDLE -> onCallEnded()
        }
    }
}
