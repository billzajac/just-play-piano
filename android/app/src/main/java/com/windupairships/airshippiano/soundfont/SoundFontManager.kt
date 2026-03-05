package com.windupairships.airshippiano.soundfont

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

sealed class SoundFontState {
    data object Checking : SoundFontState()
    data class Downloading(val progress: Float) : SoundFontState()
    data class Ready(val path: String) : SoundFontState()
    data class Error(val message: String) : SoundFontState()
}

class SoundFontManager(private val context: Context) {

    private val _state = MutableStateFlow<SoundFontState>(SoundFontState.Checking)
    val state: StateFlow<SoundFontState> = _state

    private val soundFontFile: File
        get() = File(context.filesDir, FILE_NAME)

    suspend fun ensureSoundFont() {
        val file = soundFontFile
        if (file.exists() && file.length() > 0) {
            _state.value = SoundFontState.Ready(file.absolutePath)
            return
        }
        download()
    }

    suspend fun retry() {
        download()
    }

    private suspend fun download() {
        _state.value = SoundFontState.Downloading(0f)

        withContext(Dispatchers.IO) {
            try {
                val url = URL(DOWNLOAD_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 15_000
                connection.readTimeout = 30_000
                connection.connect()

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    _state.value = SoundFontState.Error("Download failed: HTTP ${connection.responseCode}")
                    return@withContext
                }

                val totalBytes = connection.contentLength.toLong()
                val tempFile = File(context.filesDir, "$FILE_NAME.tmp")

                connection.inputStream.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var bytesRead: Long = 0
                        var count: Int

                        while (input.read(buffer).also { count = it } != -1) {
                            output.write(buffer, 0, count)
                            bytesRead += count
                            if (totalBytes > 0) {
                                _state.value = SoundFontState.Downloading(bytesRead.toFloat() / totalBytes)
                            }
                        }
                    }
                }

                // Verify SHA256
                if (!verifySHA256(tempFile)) {
                    tempFile.delete()
                    _state.value = SoundFontState.Error(
                        "Download integrity check failed \u2014 file hash does not match expected value"
                    )
                    return@withContext
                }

                // Move to final location
                val dest = soundFontFile
                if (dest.exists()) dest.delete()
                if (!tempFile.renameTo(dest)) {
                    tempFile.delete()
                    _state.value = SoundFontState.Error("Failed to save sound font file")
                    return@withContext
                }

                _state.value = SoundFontState.Ready(dest.absolutePath)
            } catch (e: Exception) {
                _state.value = SoundFontState.Error("Download failed: ${e.message}")
            }
        }
    }

    private fun verifySHA256(file: File): Boolean {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            var count: Int
            while (input.read(buffer).also { count = it } != -1) {
                digest.update(buffer, 0, count)
            }
        }
        val hex = digest.digest().joinToString("") { "%02x".format(it) }
        return hex == EXPECTED_SHA256
    }

    companion object {
        private const val FILE_NAME = "SalC5Light2.sf2"
        // Salamander Grand Piano - sampled from a Yamaha C5 grand piano by Alexander Holm
        // SF2 conversion "Salamander C5 Light" by HED-Sounds
        private const val DOWNLOAD_URL = "https://github.com/knuton/piano-notes/raw/master/SalC5Light2.sf2"
        private const val EXPECTED_SHA256 = "f0c8cb73b87e1b3b1a190e9e37ace4668fb4fcfa381f94aac00178b72e68fca4"
    }
}
