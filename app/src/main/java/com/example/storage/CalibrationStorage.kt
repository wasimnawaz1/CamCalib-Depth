package com.example.storage

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.example.model.CalibrationResult
import com.example.model.CameraLockConfig
import com.example.model.CheckerboardConfig
import java.io.File
import java.io.FileOutputStream

class CalibrationStorage(private val context: Context) {

    private val prefs = context.getSharedPreferences("cam_calib_prefs", Context.MODE_PRIVATE)

    fun getCalibrationDir(): File {
        val extDocs = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        val dir = File(extDocs ?: context.filesDir, "Calibration")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getCalibrationImagesDir(): File {
        val imgDir = File(getCalibrationDir(), "Images")
        if (!imgDir.exists()) imgDir.mkdirs()
        return imgDir
    }

    /**
     * Persists a captured calibration image directly to mobile device storage.
     */
    fun saveCalibrationImage(bitmap: Bitmap, index: Int): File {
        val dir = getCalibrationImagesDir()
        val file = File(dir, "calib_${String.format(java.util.Locale.US, "%02d", index)}_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
        return file
    }

    /**
     * Retrieves all saved calibration image files stored on the device.
     */
    fun loadCalibrationImageFiles(): List<File> {
        val dir = getCalibrationImagesDir()
        return dir.listFiles { f -> f.extension.equals("jpg", ignoreCase = true) || f.extension.equals("png", ignoreCase = true) }
            ?.sortedBy { it.name }
            ?.toList() ?: emptyList()
    }

    fun deleteCalibrationImage(file: File): Boolean {
        return if (file.exists()) file.delete() else false
    }

    fun loadBitmapFromFile(file: File): Bitmap? {
        return if (file.exists()) {
            BitmapFactory.decodeFile(file.absolutePath)
        } else null
    }

    fun createShareAllImagesIntent(): Intent? {
        val files = loadCalibrationImageFiles()
        if (files.isEmpty()) return null

        val uris = ArrayList<Uri>()
        for (f in files) {
            uris.add(
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    f
                )
            )
        }

        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "image/jpeg"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            putExtra(Intent.EXTRA_SUBJECT, "CamCalib Calibration Images (${files.size} photos)")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(intent, "Share Calibration Images").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun clearAllCalibrationImages() {
        val dir = getCalibrationImagesDir()
        dir.listFiles()?.forEach { it.delete() }
    }

    fun saveCalibration(result: CalibrationResult): Pair<File, File> {
        val dir = getCalibrationDir()
        val jsonFile = File(dir, "calibration.json")
        jsonFile.writeText(result.toJson())

        val ymlFile = File(dir, "calibration.yml")
        ymlFile.writeText(result.toYaml())

        // Save active calibration JSON to SharedPreferences for fast access
        prefs.edit().putString("active_calibration_json", result.toJson()).apply()

        // Also save camera lock config if present
        result.cameraLockConfig?.let { saveCameraLock(it) }

        return Pair(jsonFile, ymlFile)
    }

    fun loadCalibration(): CalibrationResult? {
        val jsonStr = prefs.getString("active_calibration_json", null)
        if (!jsonStr.isNullOrBlank()) {
            val res = CalibrationResult.fromJson(jsonStr)
            if (res != null) return res
        }

        val jsonFile = File(getCalibrationDir(), "calibration.json")
        if (jsonFile.exists()) {
            return CalibrationResult.fromJson(jsonFile.readText())
        }
        return null
    }

    fun clearCalibration() {
        prefs.edit().remove("active_calibration_json").apply()
        val jsonFile = File(getCalibrationDir(), "calibration.json")
        if (jsonFile.exists()) jsonFile.delete()
        val ymlFile = File(getCalibrationDir(), "calibration.yml")
        if (ymlFile.exists()) ymlFile.delete()
    }

    fun saveCameraLock(config: CameraLockConfig) {
        val dir = getCalibrationDir()
        val file = File(dir, "camera_lock_config.json")
        file.writeText(config.toJson())
        prefs.edit().putString("camera_lock_json", config.toJson()).apply()
    }

    fun loadCameraLock(): CameraLockConfig {
        val jsonStr = prefs.getString("camera_lock_json", null)
        if (!jsonStr.isNullOrBlank()) {
            return CameraLockConfig.fromJson(jsonStr)
        }
        val file = File(getCalibrationDir(), "camera_lock_config.json")
        if (file.exists()) {
            return CameraLockConfig.fromJson(file.readText())
        }
        return CameraLockConfig() // Defaults
    }

    fun saveCheckerboardConfig(config: CheckerboardConfig) {
        prefs.edit().putString("checkerboard_config_json", config.toJson()).apply()
    }

    fun loadCheckerboardConfig(): CheckerboardConfig {
        val jsonStr = prefs.getString("checkerboard_config_json", null)
        return if (!jsonStr.isNullOrBlank()) {
            CheckerboardConfig.fromJson(jsonStr)
        } else {
            CheckerboardConfig() // 9x6 corners, 25mm, 15 target
        }
    }

    fun saveBaselineMeters(baseline: Float) {
        prefs.edit().putFloat("stereo_baseline_m", baseline).apply()
    }

    fun loadBaselineMeters(): Float {
        return prefs.getFloat("stereo_baseline_m", 0.10f) // default 10cm = 0.10m
    }

    fun createShareIntent(file: File, mimeType: String, title: String): Intent {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(intent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
