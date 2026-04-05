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

class LocalAsrModelManager(
    private val context: Context,
    private val storage: AndroidStorage,
    private val client: OkHttpClient = OkHttpClient()
) {
    companion object {
        const val CUSTOM_MODEL_ID = "custom-asr-url-model"
    }

    private data class DownloadSource(
        val name: String,
        val baseUrl: String
    )

    private val requiredFiles = listOf("encoder.onnx", "decoder.onnx", "joiner.onnx", "tokens.txt")
    private val minFileSizeBytes = mapOf(
        "encoder.onnx" to 16 * 1024L,
        "decoder.onnx" to 16 * 1024L,
        "joiner.onnx" to 16 * 1024L,
        "tokens.txt" to 32L
    )
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
        .readTimeout(3, TimeUnit.MINUTES)
        .writeTimeout(3, TimeUnit.MINUTES)
        .callTimeout(20, TimeUnit.MINUTES)
        .build()

    private val models = listOf(
        AsrModelOption(
            id = "sherpa-onnx-streaming-zipformer-small-bilingual-zh-en-2023-02-16",
            name = "中英双语极速模型（默认）",
            description = "小体积+低延迟优先，适合移动端 3 秒内出结果",
            sizeLabel = "约 40~60 MB"
        ),
        AsrModelOption(
            id = "sherpa-onnx-streaming-zipformer-bilingual-zh-en-2023-02-20-mobile",
            name = "中英双语均衡模型",
            description = "速度与效果平衡，适合课堂实时识别",
            sizeLabel = "约 70~100 MB"
        ),
        AsrModelOption(
            id = "sherpa-onnx-streaming-zipformer-multi-zh-hans-2023-12-12-mobile",
            name = "中文增强模型（高精度）",
            description = "中文识别效果优先，体积更大，移动端可用",
            sizeLabel = "约 100~150 MB"
        ),
        AsrModelOption(
            id = "sherpa-onnx-streaming-zipformer-zh-int8-2025-06-30",
            name = "中文加速模型 INT8",
            description = "量化加速，中文识别更快，适合性能较好的手机",
            sizeLabel = "约 50~80 MB"
        ),
        AsrModelOption(
            id = "sherpa-onnx-streaming-zipformer-zh-14M-2023-02-23",
            name = "中文小模型 14M（轻量）",
            description = "体积较小，优先下载，适合快速启动本机语音识别",
            sizeLabel = "约 14 MB"
        ),
        AsrModelOption(
            id = "sherpa-onnx-streaming-zipformer-en-20M-2023-02-17",
            name = "英文小模型 20M",
            description = "英文识别优先选择，移动端常用轻量配置",
            sizeLabel = "约 20 MB"
        ),
        AsrModelOption(
            id = "sherpa-onnx-streaming-zipformer-en-2023-06-26-mobile",
            name = "英文增强模型（zipformer）",
            description = "英文识别效果优先，体积更大，建议在较新手机上使用",
            sizeLabel = "约 80~120 MB"
        ),
        AsrModelOption(
            id = CUSTOM_MODEL_ID,
            name = "自定义链接模型 / Custom URL Model",
            description = "通过设置页填写下载链接后可下载并使用",
            sizeLabel = "大小取决于下载链接"
        )
    )

    fun getAvailableModels(): List<AsrModelOption> = models

    fun getDefaultModelId(): String = models.first().id

    fun getModelDir(modelId: String = getDefaultModelId()): File {
        val baseDir = File(storage.getModelsDir(), "sherpa-onnx-asr")
        return File(baseDir, modelId)
    }

    fun isModelReady(modelId: String = getDefaultModelId()): Boolean {
        val dir = getModelDir(modelId)
        return invalidFiles(dir).isEmpty()
    }

    fun downloadAndPrepare(
        modelId: String,
        onProgress: (Long, Long) -> Unit,
        onEvent: (String) -> Unit = {}
    ): Result<File> {
        val archive = File(context.cacheDir, "asr_${modelId}_${UUID.randomUUID()}.tar.bz2")
        val tmpArchive = File(archive.absolutePath + ".part")
        try {
            downloadArchiveWithFallback(tmpArchive, modelId, onProgress, onEvent)
            if (!tmpArchive.renameTo(archive)) {
                return Result.failure(IOException("ASR模型包移动失败"))
            }
            val extractDir = File(storage.getModelsDir(), "asr_extract_${UUID.randomUUID()}")
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
                return Result.failure(IOException("ASR模型文件不完整: ${missing.joinToString(", ")}"))
            }

            requiredFiles.forEach { name ->
                val source = required[name] ?: return@forEach
                copyRecursively(source, File(targetDir, name))
            }

            deleteRecursively(extractDir)

            if (!isModelReady(modelId)) {
                val missingNow = invalidFiles(targetDir)
                return Result.failure(IOException("ASR模型文件不完整: ${missingNow.joinToString(", ")}"))
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
            return Result.failure(IllegalArgumentException("请先填写语音识别模型下载链接"))
        }
        val isHttp = normalizedUrl.startsWith("https://", ignoreCase = true) ||
            normalizedUrl.startsWith("http://", ignoreCase = true)
        if (!isHttp) {
            return Result.failure(IllegalArgumentException("下载链接必须以 http:// 或 https:// 开头"))
        }
        if (!normalizedUrl.lowercase().endsWith(".tar.bz2")) {
            return Result.failure(IllegalArgumentException("当前仅支持 .tar.bz2 模型包链接"))
        }

        val archive = File(context.cacheDir, "asr_custom_${UUID.randomUUID()}.tar.bz2")
        val tmpArchive = File(archive.absolutePath + ".part")
        try {
            onEvent("自定义ASR模型下载: 使用外部链接")
            downloadArchiveWithRetry(tmpArchive, normalizedUrl, "自定义链接", onProgress, onEvent)
            if (!tmpArchive.renameTo(archive)) {
                return Result.failure(IOException("ASR模型包移动失败"))
            }
            val extractDir = File(storage.getModelsDir(), "asr_extract_${UUID.randomUUID()}")
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
                return Result.failure(IOException("ASR模型文件不完整: ${missing.joinToString(", ")}"))
            }

            requiredFiles.forEach { name ->
                val source = required[name] ?: return@forEach
                copyRecursively(source, File(targetDir, name))
            }

            deleteRecursively(extractDir)

            if (!isModelReady(modelId)) {
                val missingNow = invalidFiles(targetDir)
                return Result.failure(IOException("ASR模型文件不完整: ${missingNow.joinToString(", ")}"))
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

    data class AsrModelOption(
        val id: String,
        val name: String,
        val description: String,
        val sizeLabel: String
    )

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
        return "$baseUrl/asr-models/$modelId.tar.bz2"
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
                onEvent("ASR模型下载源尝试(${index + 1}/${downloadSources.size}): ${source.name}")
                downloadArchiveWithRetry(target, url, source.name, onProgress, onEvent)
                if (index > 0) {
                    onEvent("ASR已切换至${source.name}并下载成功")
                }
                return
            } catch (e: IOException) {
                val reason = e.message ?: e.javaClass.simpleName
                sourceErrors.add("${source.name}: $reason")
                onEvent("ASR模型下载源失败: ${source.name}，原因: $reason")
                if (index == 0 && downloadSources.size > 1) {
                    onEvent("ASR GitHub 下载失败，自动切换到中国镜像源重试")
                }
            }
        }
        throw IOException("ASR模型下载失败，已尝试全部下载源: ${sourceErrors.joinToString(" | ")}")
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

    private fun invalidFiles(dir: File): List<String> {
        return requiredFiles.filter { name ->
            val file = File(dir, name)
            if (!file.exists()) {
                return@filter true
            }
            val minSize = minFileSizeBytes[name] ?: 1L
            file.length() < minSize
        }
    }

    private fun collectRequiredFiles(root: File): Map<String, File> {
        val allFiles = collectAllFiles(root)
        val found = mutableMapOf<String, File>()

        allFiles.firstOrNull { it.name.equals("tokens.txt", ignoreCase = true) }?.let {
            found["tokens.txt"] = it
        }
        selectOnnxCandidate(allFiles, "encoder")?.let { found["encoder.onnx"] = it }
        selectOnnxCandidate(allFiles, "decoder")?.let { found["decoder.onnx"] = it }
        selectOnnxCandidate(allFiles, "joiner")?.let { found["joiner.onnx"] = it }
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
            file.name.endsWith(".onnx", ignoreCase = true) &&
                file.name.contains(keyword, ignoreCase = true)
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
        if (name.contains("epoch-99-avg-1", ignoreCase = true)) {
            score += 30
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
