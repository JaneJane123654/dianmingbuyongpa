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
import java.util.concurrent.TimeUnit

class LocalKwsModelManager(
    private val context: Context,
    private val storage: AndroidStorage,
    private val client: OkHttpClient = OkHttpClient()
) {
    private val requiredFiles = listOf("encoder.onnx", "decoder.onnx", "joiner.onnx", "tokens.txt")
    private val optionalKeywordFiles = listOf("keywords.txt", "test_keywords.txt")
    private val baseUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download"
    private val downloadClient = client.newBuilder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(2, TimeUnit.MINUTES)
        .writeTimeout(2, TimeUnit.MINUTES)
        .callTimeout(15, TimeUnit.MINUTES)
        .build()
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
        return missingFiles(dir).isEmpty()
    }

    fun downloadAndPrepare(
        modelId: String,
        onProgress: (Long, Long) -> Unit
    ): Result<File> {
        val archive = File(context.cacheDir, "kws_${modelId}_${UUID.randomUUID()}.tar.bz2")
        val tmpArchive = File(archive.absolutePath + ".part")
        try {
            downloadArchiveWithRetry(tmpArchive, modelId, onProgress)
            if (!tmpArchive.renameTo(archive)) {
                return Result.failure(IOException("模型包移动失败"))
            }
            val extractDir = File(storage.getModelsDir(), "kws_extract_${UUID.randomUUID()}")
            if (!extractDir.exists()) {
                extractDir.mkdirs()
            }
            extractTarBz2(archive, extractDir)
            val targetDir = getModelDir(modelId)
            deleteRecursively(targetDir)
            targetDir.mkdirs()
            val required = collectRequiredFiles(extractDir)
            val missing = requiredFiles.filter { required[it] == null }
            if (missing.isNotEmpty()) {
                return Result.failure(IOException("模型文件不完整: ${missing.joinToString(", ")}"))
            }
            requiredFiles.forEach { name ->
                val source = required[name] ?: return@forEach
                copyRecursively(source, File(targetDir, name))
            }
            optionalKeywordFiles.forEach { name ->
                val source = required[name] ?: return@forEach
                copyRecursively(source, File(targetDir, name))
            }
            deleteRecursively(extractDir)
            if (!isModelReady(modelId)) {
                val missing = missingFiles(targetDir)
                return Result.failure(IOException("模型文件不完整: ${missing.joinToString(", ")}"))
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
        downloadClient.newCall(request).execute().use { response ->
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

    private fun downloadArchiveWithRetry(
        target: File,
        modelId: String,
        onProgress: (Long, Long) -> Unit
    ) {
        val maxAttempts = 3
        var lastError: IOException? = null
        for (attempt in 1..maxAttempts) {
            try {
                if (target.exists()) {
                    target.delete()
                }
                downloadArchive(target, modelId, onProgress)
                return
            } catch (e: IOException) {
                lastError = e
                if (attempt < maxAttempts) {
                    Thread.sleep(1200L * attempt)
                }
            }
        }
        throw lastError ?: IOException("下载失败")
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

    private fun missingFiles(dir: File): List<String> {
        return requiredFiles.filter { !File(dir, it).exists() }
    }

    private fun collectRequiredFiles(root: File): Map<String, File> {
        val allFiles = collectAllFiles(root)
        val found = mutableMapOf<String, File>()
        allFiles.forEach { entry ->
            if (entry.name in requiredFiles && !found.containsKey(entry.name)) {
                found[entry.name] = entry
            }
        }
        optionalKeywordFiles.forEach { keywordFileName ->
            allFiles.firstOrNull { it.name.equals(keywordFileName, ignoreCase = true) }?.let {
                found[keywordFileName] = it
            }
        }
        if (!found.containsKey("tokens.txt")) {
            allFiles.firstOrNull { it.name.equals("tokens.txt", ignoreCase = true) }?.let {
                found["tokens.txt"] = it
            }
        }
        if (!found.containsKey("encoder.onnx")) {
            selectOnnxCandidate(allFiles, "encoder")?.let { found["encoder.onnx"] = it }
        }
        if (!found.containsKey("decoder.onnx")) {
            selectOnnxCandidate(allFiles, "decoder")?.let { found["decoder.onnx"] = it }
        }
        if (!found.containsKey("joiner.onnx")) {
            selectOnnxCandidate(allFiles, "joiner")?.let { found["joiner.onnx"] = it }
        }
        return found
    }

    private fun collectAllFiles(root: File): List<File> {
        val result = mutableListOf<File>()
        val queue = ArrayDeque<Pair<File, Int>>()
        queue.add(root to 0)
        while (queue.isNotEmpty()) {
            val (dir, depth) = queue.removeFirst()
            dir.listFiles()?.forEach { entry ->
                if (entry.isDirectory) {
                    if (depth < 12) {
                        queue.add(entry to depth + 1)
                    }
                } else {
                    result.add(entry)
                }
            }
        }
        return result
    }

    private fun selectOnnxCandidate(files: List<File>, keyword: String): File? {
        val candidates = files.filter { file ->
            file.name.endsWith(".onnx", ignoreCase = true) && file.name.contains(keyword, ignoreCase = true)
        }
        if (candidates.isEmpty()) {
            return null
        }
        return candidates.maxBy { scoreOnnxCandidate(it.name) }
    }

    private fun scoreOnnxCandidate(name: String): Int {
        var score = 0
        if (!name.contains("int8", ignoreCase = true)) {
            score += 100
        }
        if (name.contains("epoch-12-avg-2", ignoreCase = true)) {
            score += 50
        }
        if (name.contains("epoch-99-avg-1", ignoreCase = true)) {
            score += 40
        }
        if (name.contains("chunk", ignoreCase = true)) {
            score += 5
        }
        score -= name.length / 10
        return score
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
