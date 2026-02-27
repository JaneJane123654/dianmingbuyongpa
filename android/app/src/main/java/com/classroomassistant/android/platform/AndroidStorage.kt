package com.classroomassistant.android.platform

import android.content.Context
import com.classroomassistant.core.platform.PlatformStorage
import java.io.*

/**
 * Android 文件存储实现
 */
class AndroidStorage(private val context: Context) : PlatformStorage {

    override fun getAppDataDir(): File {
        return context.filesDir
    }

    override fun getModelsDir(): File {
        val modelsDir = context.getExternalFilesDir("models") 
            ?: File(context.filesDir, "models")
        if (!modelsDir.exists()) {
            modelsDir.mkdirs()
        }
        return modelsDir
    }

    override fun getLogsDir(): File {
        val logsDir = File(context.filesDir, "logs")
        if (!logsDir.exists()) {
            logsDir.mkdirs()
        }
        return logsDir
    }

    override fun getCacheDir(): File {
        return context.cacheDir
    }

    override fun exists(relativePath: String): Boolean {
        return File(appDataDir, relativePath).exists()
    }

    override fun openInputStream(relativePath: String): InputStream {
        return FileInputStream(File(appDataDir, relativePath))
    }

    override fun openOutputStream(relativePath: String): OutputStream {
        val file = File(appDataDir, relativePath)
        file.parentFile?.mkdirs()
        return FileOutputStream(file)
    }

    override fun delete(relativePath: String): Boolean {
        return File(appDataDir, relativePath).delete()
    }

    override fun getAvailableSpace(): Long {
        return appDataDir.usableSpace
    }
}
