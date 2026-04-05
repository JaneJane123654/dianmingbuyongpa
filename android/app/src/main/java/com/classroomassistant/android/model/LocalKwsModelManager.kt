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
    companion object {
        const val CUSTOM_MODEL_ID = "custom-kws-url-model"
    }

    private data class DownloadSource(
        val name: String,
        val baseUrl: String
    )

    private val requiredFiles = listOf("encoder.onnx", "decoder.onnx", "joiner.onnx", "tokens.txt")
    private val optionalKeywordFiles = listOf("keywords.txt", "test_keywords.txt")
    private val downloadSources = listOf(
        DownloadSource(
            name = "GitHub 官方源",
            baseUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download"
        ),
        DownloadSource(
            name = "中国镜像源(kkgithub)",
            baseUrl = "https://kkgithub.com/k2-fsa/sherpa-onnx/releases/download"
        )
    )
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
            description = "中文唤醒模型，覆盖常见课堂场景，体积适中",
            sizeLabel = "约 3.3 MB"
        ),
        KwsModelOption(
            id = "sherpa-onnx-kws-zipformer-gigaspeech-3.3M-2024-01-01",
            name = "Zipformer GigaSpeech 3.3M (2024-01-01)",
            description = "英语为主的唤醒模型，适合英文课堂或双语环境",
            sizeLabel = "约 3.3 MB"
        ),
        KwsModelOption(
            id = "sherpa-onnx-kws-zipformer-zh-en-3M-2025-12-20",
            name = "Zipformer 中英 3M (2025-12-20)",
            description = "中英双语唤醒模型，优先推荐给混合语言场景",
            sizeLabel = "约 3 MB"
        ),
        KwsModelOption(
            id = CUSTOM_MODEL_ID,
            name = "自定义链接模型 / Custom URL Model",
            description = "通过设置页填写下载链接后可下载并使用",
            sizeLabel = "大小取决于下载链接"
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
        onProgress: (Long, Long) -> Unit,
        onEvent: (String) -> Unit = {}
    ): Result<File> {
        val archive = File(context.cacheDir, "kws_${modelId}_${UUID.randomUUID()}.tar.bz2")
        val tmpArchive = File(archive.absolutePath + ".part")
        try {
            downloadArchiveWithFallback(tmpArchive, modelId, onProgress, onEvent)
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

    fun downloadAndPrepareFromUrl(
        modelId: String = CUSTOM_MODEL_ID,
        archiveUrl: String,
        onProgress: (Long, Long) -> Unit,
        onEvent: (String) -> Unit = {}
    ): Result<File> {
        val normalizedUrl = archiveUrl.trim()
        if (normalizedUrl.isBlank()) {
            return Result.failure(IllegalArgumentException("请先填写唤醒模型下载链接"))
        }
        val isHttp = normalizedUrl.startsWith("https://", ignoreCase = true) ||
            normalizedUrl.startsWith("http://", ignoreCase = true)
        if (!isHttp) {
            return Result.failure(IllegalArgumentException("下载链接必须以 http:// 或 https:// 开头"))
        }
        if (!normalizedUrl.lowercase().endsWith(".tar.bz2")) {
            return Result.failure(IllegalArgumentException("当前仅支持 .tar.bz2 模型包链接"))
        }

        val archive = File(context.cacheDir, "kws_custom_${UUID.randomUUID()}.tar.bz2")
        val tmpArchive = File(archive.absolutePath + ".part")
        try {
            onEvent("自定义唤醒模型下载: 使用外部链接")
            downloadArchiveWithRetry(tmpArchive, normalizedUrl, "自定义链接", onProgress, onEvent)
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
        url: String,
        onProgress: (Long, Long) -> Unit
    ) {
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

    private fun buildArchiveUrl(baseUrl: String, modelId: String): String {
        return "$baseUrl/kws-models/$modelId.tar.bz2"
    }

    private fun downloadArchiveWithFallback(
        target: File,
        modelId: String,
        onProgress: (Long, Long) -> Unit,
        onEvent: (String) -> Unit
    ) {
        val sourceErrors = mutableListOf<String>()
        downloadSources.forEachIndexed { index, source ->
            val url = buildArchiveUrl(source.baseUrl, modelId)
            try {
                onEvent("模型下载源尝试(${index + 1}/${downloadSources.size}): ${source.name}")
                downloadArchiveWithRetry(target, url, source.name, onProgress, onEvent)
                if (index > 0) {
                    onEvent("已切换至${source.name}并下载成功")
                }
                return
            } catch (e: IOException) {
                val reason = e.message ?: e.javaClass.simpleName
                sourceErrors.add("${source.name}: $reason")
                onEvent("模型下载源失败: ${source.name}，原因: $reason")
                if (index == 0 && downloadSources.size > 1) {
                    onEvent("GitHub 下载失败，自动切换到中国镜像源重试")
                }
            }
        }
        throw IOException("模型下载失败，已尝试全部下载源: ${sourceErrors.joinToString(" | ")}")
    }

    private fun downloadArchiveWithRetry(
        target: File,
        url: String,
        sourceName: String,
        onProgress: (Long, Long) -> Unit,
        onEvent: (String) -> Unit
    ) {
        val maxAttempts = 3
        var lastError: IOException? = null
        for (attempt in 1..maxAttempts) {
            try {
                if (target.exists()) {
                    target.delete()
                }
                downloadArchive(target, url, onProgress)
                return
            } catch (e: IOException) {
                lastError = e
                if (attempt < maxAttempts) {
                    onEvent("下载重试: $sourceName 第${attempt}次失败，准备第${attempt + 1}次")
                    Thread.sleep(1200L * attempt)
                }
            }
        }
        throw lastError ?: IOException("下载失败")
    }

    data class KwsModelOption(
        val id: String,
        val name: String,
        val description: String,
        val sizeLabel: String
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
