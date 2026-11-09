# CamCalib Depth: Mobile Camera Calibration + Stereo Depth Estimation with OpenCV

A complete, production-grade Android application for precision mobile camera calibration and metric real-world depth estimation using OpenCV and Android Camera2 API with manual parameter locking.

Designed and tested for modern Android smartphones (including Vivo Y100, Pixel, Samsung Galaxy, and all Camera2 Level 3/Full/Limited hardware).

---

## Key Features

### 1. Camera2 Manual Parameter Locking
Smartphone cameras dynamically adjust autofocus, auto-exposure, auto-white-balance, and optical image stabilization (OIS) across frames. This changes the optical focal length ($f_x, f_y$) and principal point ($c_x, c_y$) continuously, breaking computer vision assumptions.
- **Fixed Auto-Focus (AF OFF):** Sets `LENS_FOCUS_DISTANCE` to fixed diopter (default 1.5 diopters ~ 0.67m arm's length) and cancels focus triggers.
- **Fixed Auto-Exposure (AE OFF):** Locks sensor exposure time (`16,666,666 ns` / 1/60s) and sensor sensitivity (`ISO 200`).
- **Fixed White Balance (AWB OFF):** Locks manual channel gains (`RggbChannelVector`).
- **Stabilization Disabled:** Turns off optical (`LENS_OPTICAL_STABILIZATION_MODE_OFF`) and digital video stabilization (`CONTROL_VIDEO_STABILIZATION_MODE_OFF`).
- **Auto-Meter & Lock:** Tap the magic wand icon to meter the room lighting once for 1.5s, then permanently freeze and store those exact hardware parameters in `camera_lock_config.json`.
- **Session Continuity:** The same locked settings are reloaded for Depth Estimation mode to ensure geometric consistency.

### 2. Live Checkerboard Detection & Pose Diversity Meter
- **Live Detection:** Real-time corner detection with sub-pixel refinement (`Imgproc.cornerSubPix`).
- **Sharpness / Blur Rejection:** Computes Laplacian variance on every frame. Automatically rejects blurry frames ($< 90$ variance threshold) or motion blur.
- **Pose Diversity Checklist:** Guides the user through:
  - Front (facing center)
  - Left 30° Tilt
  - Right 30° Tilt
  - Top Tilt
  - Bottom Tilt
  - Rotated 45° (in-plane tilt)
  - Close Up (~50% frame)
  - Far Away (~25% frame)
- **Progress Counter:** Live counter (e.g. 18 / 35 images) with visual thumbnails and delete option per image.

### 3. OpenCV Camera Calibration
- Executes `Calib3d.calibrateCamera` across all captured corners and 3D board coordinates ($Z = 0$).
- Solves:
  - **Intrinsic Parameters (Matrix K):** $f_x, f_y, c_x, c_y$
  - **Distortion Coefficients:** Radial ($k_1, k_2, k_3$) and Tangential ($p_1, p_2$)
  - **Extrinsic Parameters:** Rotation ($rvecs$) and Translation ($tvecs$) per frame
  - **Mean Reprojection Error:** Computes L2 error comparing projected corners with detected points. Highlights any outlier images $> 0.50$ px.
- **Interactive Before/After Slider:** Visual split-screen showing the original distorted image alongside the mathematically undistorted image with straight lines.
- **Export Options:** Generates both standard OpenCV `calibration.yml` and `calibration.json` with instant Android Share Sheet sharing.

### 4. Two-View Stereo Depth Estimation & Tap-to-Measure
- **Calibrated Stereo Geometry:** Undistorts Left and Right captures using saved intrinsic parameters and distortion coefficients.
- **Dense Disparity:** Computes stereo disparity map using OpenCV `StereoSGBM` with 3-way semi-global dynamic matching.
- **Metric Distance Conversion:**
  $$Z(x, y) = \frac{f_x \cdot \text{baseline}}{\text{disparity}(x, y)}$$
- **Jet Colormap Visualization:** Colorizes valid depth (Warm Red = Near, Cool Blue = Far) with real-time scale bar.
- **Interactive Tap-to-Measure:** Tap anywhere on the depth map or camera view to place a crosshair and view the exact metric distance in meters and centimeters (e.g. `1.42 meters (142 cm)`).
- **3D Point Cloud Export:** Generates standard Stanford `.PLY` point cloud files with 3D coordinates $(X, Y, Z)$ and true RGB vertex colors for viewing in MeshLab, Blender, or CloudCompare.

### 5. Built-in Printable Checkerboard Generator
- Generates a high-resolution A4 pattern (9x6 inner corners, 10x7 squares, 25mm square size).
- Includes a 10cm reference ruler to verify 1:1 print scaling.
- Export as PNG or share directly to print or view on a second screen/tablet.

---

## Step-by-Step Instructions

### Step 1: Prepare the Checkerboard
1. Open the app and tap **Checkerboard (Printable A4 / Screen)**.
2. Either print it at 100% scale (A4 paper) and tape it flat to a piece of rigid cardboard or clipboard, OR display it full-screen on a tablet / laptop monitor.

### Step 2: Calibrate the Phone Camera
1. On the Home Screen, tap **Calibrate Camera**.
2. Aim at the checkerboard. Notice the **AF/AE/AWB LOCKED** chip at the top.
3. If the room is brighter or darker than default, tap the **Auto-Meter (Magic Wand)** icon to meter and freeze the lighting.
4. Move around the board to capture 30 to 35 images:
   - Capture from the center, left 30°, right 30°, tilted up, tilted down, rotated diagonally, closer, and further away.
   - The checklist chips will automatically turn green as each pose is covered.
5. Tap **Calibrate Now** once you have captured 30+ frames.
6. Review your intrinsic matrix ($f_x, f_y, c_x, c_y$), distortion coefficients, and before/after undistortion slider.
7. Tap **Export .YML** or **Export .JSON** if you need the calibration files for external ROS/OpenCV/Python workflows.

### Step 3: Estimate Depth of Real-World Scenes
1. Tap **Proceed to Two-View Depth Estimation**.
2. Aim camera at an object or scene (e.g., a chair, desk, box, or person ~1 to 3 meters away).
3. Tap the Shutter button to capture the **Left View**.
4. Move your phone ~10 cm horizontally to the right (keep the phone at the same height and orientation). Adjust the baseline slider if your displacement is different.
5. Tap the Shutter button to capture the **Right View**.
6. The app automatically undistorts the stereo pair and computes the dense metric depth map.
7. **Tap anywhere on the depth image** to inspect the metric distance in meters and centimeters!
8. Tap **Export 3D .PLY** to export the 3D point cloud.
