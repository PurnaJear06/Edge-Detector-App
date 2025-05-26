package com.purnajear.edgedetectorapp.camera;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CameraHelper implements TextureView.SurfaceTextureListener {
    private static final String TAG = "CameraHelper";
    
    // Camera preview size
    private static final int PREVIEW_WIDTH = 640;
    private static final int PREVIEW_HEIGHT = 480;
    
    private final Context context;
    private final TextureView textureView;
    private String cameraId;
    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private ImageReader imageReader;
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    private FrameCallback frameCallback;
    private final Semaphore cameraOpenCloseLock = new Semaphore(1);
    
    private long lastFrameTime = 0;
    private float currentFps = 0;
    
    // Camera state callbacks
    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            createCaptureSession();
            cameraOpenCloseLock.release();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            cameraOpenCloseLock.release();
            camera.close();
            cameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            cameraOpenCloseLock.release();
            camera.close();
            cameraDevice = null;
            Log.e(TAG, "Camera device error: " + error);
        }
    };
    
    public interface FrameCallback {
        void onFrame(byte[] data, int width, int height);
    }
    
    public CameraHelper(Context context, TextureView textureView) {
        this.context = context;
        this.textureView = textureView;
        textureView.setSurfaceTextureListener(this);
    }
    
    public void setFrameCallback(FrameCallback callback) {
        this.frameCallback = callback;
    }
    
    public float getCurrentFps() {
        return currentFps;
    }
    
    public void startCamera() {
        startBackgroundThread();
        if (textureView.isAvailable()) {
            openCamera();
        }
    }
    
    public void stopCamera() {
        try {
            cameraOpenCloseLock.acquire();
            closeCamera();
            stopBackgroundThread();
        } catch (InterruptedException e) {
            Log.e(TAG, "Interrupted while stopping camera", e);
        } finally {
            cameraOpenCloseLock.release();
        }
    }
    
    private void openCamera() {
        android.hardware.camera2.CameraManager manager = (android.hardware.camera2.CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            if (manager == null) {
                Log.e(TAG, "Camera manager is null");
                return;
            }
            
            // Get back-facing camera
            String[] cameraIds = manager.getCameraIdList();
            if (cameraIds.length == 0) {
                Log.e(TAG, "No cameras available on this device");
                return;
            }
            
            // First try to get the back camera
            for (String cameraId : cameraIds) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    this.cameraId = cameraId;
                    
                    // Check if this camera supports our desired preview size
                    StreamConfigurationMap map = characteristics.get(
                            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    if (map == null) {
                        continue;
                    }
                    
                    // Create ImageReader for frame processing
                    imageReader = ImageReader.newInstance(PREVIEW_WIDTH, PREVIEW_HEIGHT, 
                            ImageFormat.YUV_420_888, 2);
                    imageReader.setOnImageAvailableListener(onImageAvailableListener, backgroundHandler);
                    
                    break;
                }
            }
            
            // If no back camera, try to use any available camera
            if (this.cameraId == null && cameraIds.length > 0) {
                Log.d(TAG, "No back camera found, using first available camera");
                this.cameraId = cameraIds[0];
                
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(this.cameraId);
                StreamConfigurationMap map = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map != null) {
                    imageReader = ImageReader.newInstance(PREVIEW_WIDTH, PREVIEW_HEIGHT, 
                            ImageFormat.YUV_420_888, 2);
                    imageReader.setOnImageAvailableListener(onImageAvailableListener, backgroundHandler);
                }
            }
            
            if (this.cameraId != null) {
                if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                    throw new RuntimeException("Timed out waiting to lock camera opening.");
                }
                manager.openCamera(cameraId, stateCallback, backgroundHandler);
            } else {
                Log.e(TAG, "No camera found");
            }
        } catch (CameraAccessException | SecurityException | InterruptedException e) {
            Log.e(TAG, "Error opening camera", e);
        }
    }
    
    private void createCaptureSession() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            texture.setDefaultBufferSize(PREVIEW_WIDTH, PREVIEW_HEIGHT);
            Surface previewSurface = new Surface(texture);
            Surface readerSurface = imageReader.getSurface();
            
            // Create request and add targets
            CaptureRequest.Builder requestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            requestBuilder.addTarget(previewSurface);
            requestBuilder.addTarget(readerSurface);
            
            // Create capture session
            cameraDevice.createCaptureSession(Arrays.asList(previewSurface, readerSurface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            if (cameraDevice == null) return;
                            
                            captureSession = session;
                            try {
                                // Auto-focus mode
                                requestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                
                                // Start the capture session
                                CaptureRequest captureRequest = requestBuilder.build();
                                captureSession.setRepeatingRequest(captureRequest, null, backgroundHandler);
                            } catch (CameraAccessException e) {
                                Log.e(TAG, "Error configuring camera capture session", e);
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            Log.e(TAG, "Camera capture session configuration failed");
                        }
                    }, null);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Error creating capture session", e);
        }
    }
    
    private final ImageReader.OnImageAvailableListener onImageAvailableListener = reader -> {
        try (Image image = reader.acquireLatestImage()) {
            if (image != null && frameCallback != null) {
                // Calculate FPS
                long currentTime = System.currentTimeMillis();
                if (lastFrameTime > 0) {
                    float timeDiff = (currentTime - lastFrameTime) / 1000f;
                    if (timeDiff > 0) {
                        currentFps = 0.9f * currentFps + 0.1f * (1f / timeDiff); // Smooth FPS
                    }
                }
                lastFrameTime = currentTime;
                
                // CRITICAL FIX: Proper YUV_420_888 to NV21 conversion based on web sources
                Image.Plane yPlane = image.getPlanes()[0];
                Image.Plane uPlane = image.getPlanes()[1];
                Image.Plane vPlane = image.getPlanes()[2];
                
                ByteBuffer yBuffer = yPlane.getBuffer();
                ByteBuffer uBuffer = uPlane.getBuffer();
                ByteBuffer vBuffer = vPlane.getBuffer();
                
                int ySize = yBuffer.remaining();
                int uSize = uBuffer.remaining();
                int vSize = vBuffer.remaining();
                
                // Create NV21 format: Y plane + interleaved VU plane
                byte[] nv21 = new byte[ySize + uSize + vSize];
                
                // Copy Y plane
                yBuffer.get(nv21, 0, ySize);
                
                // CRITICAL FIX: Handle UV planes correctly for NV21 format
                int uvPixelStride = uPlane.getPixelStride();
                Log.d(TAG, "UV pixel stride: " + uvPixelStride);
                
                if (uvPixelStride == 1) {
                    // Simple case: no padding - direct copy
                    vBuffer.get(nv21, ySize, vSize);
                    uBuffer.get(nv21, ySize + vSize, uSize);
                } else {
                    // Complex case: pixel stride = 2, need to handle interleaving
                    // For NV21: VUVUVUVU pattern required
                    byte[] vData = new byte[vSize];
                    byte[] uData = new byte[uSize];
                    vBuffer.get(vData);
                    uBuffer.get(uData);
                    
                    // Create interleaved VU pattern for NV21
                    int uvIndex = ySize;
                    for (int i = 0; i < vSize && i < uSize; i += uvPixelStride) {
                        if (uvIndex < nv21.length - 1) {
                            nv21[uvIndex++] = vData[i];     // V
                            nv21[uvIndex++] = uData[i];     // U
                        }
                    }
                }
                
                Log.d(TAG, "YUV conversion: ySize=" + ySize + ", uSize=" + uSize + ", vSize=" + vSize + ", pixelStride=" + uvPixelStride);
                
                // Send properly formatted NV21 data to callback
                frameCallback.onFrame(nv21, PREVIEW_WIDTH, PREVIEW_HEIGHT);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing image", e);
        }
    };
    
    private void closeCamera() {
        if (captureSession != null) {
            captureSession.close();
            captureSession = null;
        }
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
    }
    
    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraThread");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }
    
    private void stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join();
                backgroundThread = null;
                backgroundHandler = null;
            } catch (InterruptedException e) {
                Log.e(TAG, "Error stopping background thread", e);
            }
        }
    }
    
    @Override
    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
        openCamera();
    }

    @Override
    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
        // No action needed
    }

    @Override
    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {
        // No action needed
    }
}
