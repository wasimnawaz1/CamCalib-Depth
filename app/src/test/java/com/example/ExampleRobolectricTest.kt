package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.CalibrationResult
import com.example.model.CameraLockConfig
import com.example.model.CheckerboardConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("CamCalib Depth", appName)
  }

  @Test
  fun testCheckerboardConfigSerialization() {
    val config = CheckerboardConfig(patternCols = 9, patternRows = 6, squareSizeMm = 25.0f)
    val json = config.toJson()
    val parsed = CheckerboardConfig.fromJson(json)

    assertEquals(9, parsed.patternCols)
    assertEquals(6, parsed.patternRows)
    assertEquals(25.0f, parsed.squareSizeMm, 0.001f)
    assertEquals(54, parsed.totalCorners)
  }

  @Test
  fun testCameraLockConfigSerialization() {
    val lockConfig = CameraLockConfig(
      focusDistance = 1.5f,
      exposureTimeNs = 16_666_666L,
      sensitivityIso = 200,
      rGain = 1.85f,
      gEvenGain = 1.0f,
      gOddGain = 1.0f,
      bGain = 1.65f,
      isLocked = true
    )
    val json = lockConfig.toJson()
    val parsed = CameraLockConfig.fromJson(json)

    assertEquals(1.5f, parsed.focusDistance, 0.001f)
    assertEquals(16_666_666L, parsed.exposureTimeNs)
    assertEquals(200, parsed.sensitivityIso)
    assertEquals(1.85f, parsed.rGain, 0.01f)
    assertTrue(parsed.isLocked)
  }

  @Test
  fun testCalibrationResultYamlAndJson() {
    val calib = CalibrationResult(
      fx = 1420.5,
      fy = 1419.8,
      cx = 960.0,
      cy = 540.0,
      k1 = -0.12,
      k2 = 0.05,
      p1 = 0.001,
      p2 = -0.002,
      k3 = 0.0,
      reprojectionError = 0.28,
      imageWidth = 1920,
      imageHeight = 1080,
      imageCount = 32,
      perViewErrors = listOf(0.25, 0.29, 0.31)
    )

    val yaml = calib.toYaml()
    assertTrue(yaml.contains("%YAML:1.0"))
    assertTrue(yaml.contains("camera_matrix"))
    assertTrue(yaml.contains("distortion_coefficients"))
    assertTrue(yaml.contains("1420.5"))

    val json = calib.toJson()
    val parsed = CalibrationResult.fromJson(json)
    assertNotNull(parsed)
    assertEquals(1420.5, parsed!!.fx, 0.001)
    assertEquals(1419.8, parsed.fy, 0.001)
    assertEquals(0.28, parsed.reprojectionError, 0.001)
    assertEquals("Good (≤ 0.50 px)", parsed.qualityRating)
  }
}
