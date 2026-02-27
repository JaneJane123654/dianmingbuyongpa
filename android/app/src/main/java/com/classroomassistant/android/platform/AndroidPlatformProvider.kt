package com.classroomassistant.android.platform

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.classroomassistant.core.platform.*

/**
 * Android 平台提供者实现
 */
class AndroidPlatformProvider(private val context: Context) : PlatformProvider {

    private val mainHandler = Handler(Looper.getMainLooper())
    
    private val audioRecorder by lazy { AndroidAudioRecorder(context) }
    private val preferences by lazy { AndroidPreferences(context) }
    private val storage by lazy { AndroidStorage(context) }
    private val secureStorage by lazy { AndroidSecureStorage(context) }

    override fun getPlatformName(): String = "android"

    override fun getAudioRecorder(): PlatformAudioRecorder = audioRecorder

    override fun getPreferences(): PlatformPreferences = preferences

    override fun getStorage(): PlatformStorage = storage

    override fun getSecureStorage(): PlatformSecureStorage = secureStorage

    override fun runOnMainThread(task: Runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            task.run()
        } else {
            mainHandler.post(task)
        }
    }

    override fun showToast(message: String) {
        runOnMainThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "3.0.0"
        } catch (e: Exception) {
            "3.0.0"
        }
    }
}
