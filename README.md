# 📱 Edge Detector App

<div align="center">
  <!-- Banner image placeholder - 1280x640px with dark background and edge detection visualization -->
  <p><i>A banner image showing edge detection in action would look great here (1280x640px)</i></p>
  
  <p>
    <a href="#features">Features</a> •
    <a href="#demo">Demo</a> •
    <a href="#setup">Setup</a> •
    <a href="#architecture">Architecture</a> •
    <a href="#tech-stack">Tech Stack</a>
  </p>
  
  ![OpenCV](https://img.shields.io/badge/OpenCV-4.5.0-green)
  ![OpenGL ES](https://img.shields.io/badge/OpenGL_ES-2.0+-blue)
  ![Android](https://img.shields.io/badge/Android_SDK-24+-brightgreen)
  ![NDK](https://img.shields.io/badge/NDK-21.0+-orange)
</div>

## 🌟 Overview

A real-time edge detection application that captures camera frames, processes them using OpenCV in C++, and displays the output using OpenGL ES. This project demonstrates powerful image processing capabilities using native code while maintaining smooth performance.

## ✨ Features

<table>
  <tr>
    <td width="50%">
      <h3>📸 Camera Integration</h3>
      <ul>
        <li>Camera2 API with TextureView</li>
        <li>Continuous frame capture</li>
        <li>Efficient frame processing pipeline</li>
      </ul>
    </td>
    <td width="50%">
      <h3>🔄 OpenCV Processing (C++)</h3>
      <ul>
        <li>JNI bridge for native code execution</li>
        <li>Canny Edge Detection algorithm</li>
        <li>Gaussian blur preprocessing</li>
        <li>Optimized memory management</li>
      </ul>
    </td>
  </tr>
  <tr>
    <td width="50%">
      <h3>🎨 OpenGL ES Rendering</h3>
      <ul>
        <li>Real-time texture updates</li>
        <li>Custom GLSL shaders</li>
        <li>15+ FPS on mid-range devices</li>
      </ul>
    </td>
    <td width="50%">
      <h3>➕ Bonus Features</h3>
      <ul>
        <li>Toggle between raw camera and edge detection</li>
        <li>Real-time FPS counter</li>
        <li>Material Design UI with immersive mode</li>
      </ul>
    </td>
  </tr>
</table>

## 📷 Demo

<div align="center">
  <table>
    <tr>
      <td align="center"><b>Edge Detection Mode</b></td>
      <td align="center"><b>Raw Camera Mode</b></td>
    </tr>
    <tr>
      <td><img src="screenshots/edge_detection.jpg" alt="Edge Detection Mode" width="300px"/></td>
      <td><img src="screenshots/raw_camera.jpg" alt="Raw Camera Mode" width="300px"/></td>
    </tr>
  </table>
</div>

## ⚙️ Setup

### Prerequisites

- Android Studio Arctic Fox or newer
- Android SDK 24+
- NDK 21.0+
- OpenCV 4.5.0+ for Android

### Installation Steps

1. **Clone the repository**
   ```bash
   git clone https://github.com/your-username/EdgeDetectorApp.git
   cd EdgeDetectorApp
   ```

2. **Install OpenCV for Android**
   - Download [OpenCV Android SDK](https://opencv.org/releases/)
   - Extract the ZIP file
   - Copy the folder to the `libs` directory in project root
   - Rename it to `OpenCV-android-sdk`

3. **Build & Run**
   - Open project in Android Studio
   - Sync Gradle files
   - Connect a device with camera access
   - Build and run the app

## 🧠 Architecture

### Project Structure

```
app/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/purnajear/edgedetectorapp/
│   │   │       ├── camera/         # Camera handling
│   │   │       │   └── CameraHelper.java
│   │   │       ├── gl/             # OpenGL rendering
│   │   │       │   └── OpenGLRenderer.java
│   │   │       └── MainActivity.java
│   │   ├── jni/
│   │   │   ├── edgedetection/      # Native edge detection
│   │   │   │   └── edge_detector.cpp
│   │   │   ├── opencv-check/       # OpenCV version check
│   │   │   │   └── opencv_check.cpp
│   │   │   └── CMakeLists.txt
│   │   └── res/                    # Resources
└── build.gradle.kts
```

### Data Flow

<div align="center">
  <!-- Flow diagram placeholder - 800x400px showing the data flow through the app -->
  <p><i>A flow diagram would look great here (800x400px)</i></p>
</div>

1. **Camera Capture** → Camera frames captured using Camera2 API in `CameraHelper.java`
2. **JNI Transfer** → Frames passed to native layer via `nativeProcessFrame()` method
3. **OpenCV Processing** → Frames converted from YUV to RGBA and processed with Canny edge detection
4. **Texture Update** → Processed frames returned to Java and passed to `OpenGLRenderer`
5. **Rendering** → OpenGL ES renders frames to screen using GLSL shaders

## 🛠️ Tech Stack

<table>
  <tr>
    <td align="center"><img src="https://raw.githubusercontent.com/opencv/opencv/master/doc/opencv-logo2.png" height="40px"/><br/>OpenCV (C++)</td>
    <td align="center"><img src="https://www.khronos.org/assets/uploads/apis/opengles_100px.png" height="40px"/><br/>OpenGL ES</td>
    <td align="center"><img src="https://developer.android.com/static/images/brand/Android_Robot.png" height="40px"/><br/>Android SDK</td>
    <td align="center"><img src="https://developer.android.com/static/images/ndk/ndk-logo.png" height="40px"/><br/>Android NDK</td>
  </tr>
</table>

## 📊 Performance Considerations

- **Memory Management**: Frame reuse to minimize allocations
- **Efficient Rendering**: Direct texture updates for minimal overhead
- **Optimized Algorithms**: Carefully tuned OpenCV operations
- **Background Processing**: Camera operations on dedicated thread

## 📄 License

This project is part of a technical assessment for an R&D internship position.

---

<div align="center">
  <sub>Built with ❤️ for Android Computer Vision</sub>
</div>