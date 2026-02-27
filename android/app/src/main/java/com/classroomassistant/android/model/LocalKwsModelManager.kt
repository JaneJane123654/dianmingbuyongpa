package com.classroomassistant.android.model

import android.content.Context
import com.classroomassistant.android.platform.AndroidStorage
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

class LocalKwsModelManager(
    private val context: Context,
    private val storage: AndroidStorage,
    private val client: OkHttpClient = OkHttpClient()
) {
    private val baseUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download"
    private val models = listOf(
        KwsModelOption(
            id = "sherpa-onnx-kws-zipformer-wenetspeech-3.3M-2024-01-01",
            name = "Zipformer WenetSpeech 3.3M (2024-01-01)",
            description = "中文唤醒模型，覆盖常见课堂场景，体积适中"
        ),
        KwsModelOption(
            id = "sherpa-onnx-kws-zipformer-gigaspeech-3.3M-2024-01-01",
            name = "Zipformer GigaSpeech 3.3M (2024-01-01)",
            description = "英语为主的唤醒模型，适合英文课堂或双语环境"
        ),
        KwsModelOption(
            id = "sherpa-onnx-kws-zipformer-zh-en-3M-2025-12-20",
            name = "Zipformer 中英 3M (2025-12-20)",
            description = "中英双语唤醒模型，优先推荐给混合语言场景"
        )
    )

    fun getAvailableModels(): List<KwsModelOption> = models

    fun getDefaultModelId(): String = models.first().id

    fun getModelDir(modelId: String = getDefaultModelId()): File {
        val baseDir = File(storage.getModelsDir(), "sherpa-onnx-kws")
        val targetDir = File(baseDir, modelId)
        return targetDir
    }

    fun isModelReady(modelId: String = getDefaultModelId()): Boolean {
        val dir = getModelDir(modelId)
        return listOf("encoder.onnx", "decoder.onnx", "joiner.onnx", "tokens.txt")
            .all { File(dir, it).exists() }
    }

    fun downloadAndPrepare(
        modelId: String,
        onProgress: (Long, Long) -> Unit
    ): Result<File> {
        val archive = File(context.cacheDir, "kws_${modelId}_${UUID.randomUUID()}.tar.bz2")
        val tmpArchive = File(archive.absolutePath + ".part")
        try {
            downloadArchive(tmpArchive, modelId, onProgress)
            if (!tmpArchive.renameTo(archive)) {
                return Result.failure(IOException("模型包移动失败"))
            }
            val extractDir = File(storage.getModelsDir(), "kws_extract_${UUID.randomUUID()}")
            if (!extractDir.exists()) {
                extractDir.mkdirs()
            }
            extractTarBz2(archive, extractDir)
            val modelDir = resolveModelDir(extractDir)
                ?: return Result.failure(IOException("模型目录结构异常"))
            val targetDir = getModelDir(modelId)
            deleteRecursively(targetDir)
            if (!modelDir.renameTo(targetDir)) {
                copyRecursively(modelDir, targetDir)
                deleteRecursively(modelDir)
            }
            deleteRecursively(extractDir)
            if (!isModelReady(modelId)) {
                return Result.failure(IOException("模型文件不完整"))
            }
            return Result.success(targetDir)
        } catch (e: Exception) {
            return Result.failure(e)
        } finally {
            if (archive.exists()) {
                archive.delete()
            }
            if (tmpArchive.exists()) {
                tmpArchive.delete()
            }
        }
    }

    fun migrateLegacyModelIfNeeded(modelId: String = getDefaultModelId()): Boolean {
        if (modelId != getDefaultModelId()) {
            return false
        }
        val baseDir = File(storage.getModelsDir(), "sherpa-onnx-kws")
        val targetDir = File(baseDir, modelId)
        if (!baseDir.exists() || !baseDir.isDirectory || targetDir.exists()) {
            return false
        }
        val legacyTokens = File(baseDir, "tokens.txt")
        if (!legacyTokens.exists()) {
            return false
        }
        targetDir.parentFile?.mkdirs()
        baseDir.listFiles()?.forEach { child ->
            copyRecursively(child, File(targetDir, child.name))
        }
        return true
    }

    private fun downloadArchive(
        target: File,
        modelId: String,
        onProgress: (Long, Long) -> Unit
    ) {
        val url = "$baseUrl/kws-models/$modelId.tar.bz2"
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("下载失败: ${response.code}")
            }
            val body = response.body ?: throw IOException("下载内容为空")
            val total = body.contentLength().coerceAtLeast(0L)
            target.parentFile?.mkdirs()
            FileOutputStream(target).use { output ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(64 * 1024)
                    var downloaded = 0L
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloaded += read
                        onProgress(downloaded, total)
                    }
                }
            }
        }
    }

    data class KwsModelOption(
        val id: String,
        val name: String,
        val description: String
    )

    private fun extractTarBz2(archive: File, targetDir: File) {
        FileInputStream(archive).use { fileInput ->
            BufferedInputStream(fileInput).use { bufferedInput ->
                BZip2CompressorInputStream(bufferedInput).use { bzip2Input ->
                    TarArchiveInputStream(bzip2Input).use { tarInput ->
                        var entry = tarInput.nextTarEntry
                        while (entry != null) {
                            val outFile = File(targetDir, entry.name)
                            val canonicalPath = outFile.canonicalPath
                            val targetPath = targetDir.canonicalPath
                            if (!canonicalPath.startsWith(targetPath)) {
                                throw IOException("非法文件路径: ${entry.name}")
                            }
                            if (entry.isDirectory) {
                                outFile.mkdirs()
                            } else {
                                outFile.parentFile?.mkdirs()
                                FileOutputStream(outFile).use { output ->
                                    val buffer = ByteArray(64 * 1024)
                                    var read: Int
                                    while (tarInput.read(buffer).also { read = it } != -1) {
                                        output.write(buffer, 0, read)
                                    }
                                }
                            }
                            entry = tarInput.nextTarEntry
                        }
                    }
                }
            }
        }
    }

    private fun resolveModelDir(root: File): File? {
        val tokens = File(root, "tokens.txt")
        if (tokens.exists()) {
            return root
        }
        val children = root.listFiles()?.filter { it.isDirectory } ?: return null
        return children.firstOrNull { File(it, "tokens.txt").exists() }
    }

    private fun deleteRecursively(file: File) {
        if (!file.exists()) {
            return
        }
        if (file.isDirectory) {
            file.listFiles()?.forEach { deleteRecursively(it) }
        }
        file.delete()
    }

    private fun copyRecursively(source: File, target: File) {
        if (source.isDirectory) {
            target.mkdirs()
            source.listFiles()?.forEach { child ->
                copyRecursively(child, File(target, child.name))
            }
        } else {
            target.parentFile?.mkdirs()
            FileInputStream(source).use { input ->
                FileOutputStream(target).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                    }
                }
            }
        }
    }
}
