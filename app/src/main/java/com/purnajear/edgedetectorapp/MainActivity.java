package com.purnajear.edgedetectorapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;
import com.google.android.material.snackbar.Snackbar;
import com.purnajear.edgedetectorapp.camera.CameraHelper;
import com.purnajear.edgedetectorapp.gl.OpenGLRenderer;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "EdgeDetector";
    private static final int CAMERA_PERMISSION_CODE = 100;
    
    private TextureView textureView;
    private CameraHelper cameraHelper;
    private OpenGLRenderer renderer;
    private MaterialButton toggleButton;
    private TextView fpsCounter;
    private LinearLayout edgeParamsLayout;
    private Slider thresholdSlider;
    private Slider ratioSlider;
    private TextView thresholdLabel;
    private TextView ratioLabel;
    
    private boolean isEdgeDetectionEnabled = false;
    
    // Load native library
    static {
        try {
            Log.d(TAG, "Loading edge-detector library...");
            System.loadLibrary("edge-detector");
            Log.d(TAG, "Library loaded successfully");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "Failed to load native library: " + e.getMessage(), e);
            e.printStackTrace();
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error loading library: " + e.getMessage(), e);
            e.printStackTrace();
        }
    }
    
    // Native methods
    public native void initOpenCV();
    public native String getOpenCVVersion();
    public native void setCannyParameters(int threshold, int ratio);
    public native void forceEdgeDetectionTest(); // Test method
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);
            
            Log.d(TAG, "Activity created");
            
            // Find views
            textureView = findViewById(R.id.texture_view);
            toggleButton = findViewById(R.id.toggle_button);
            fpsCounter = findViewById(R.id.fps_counter);
            edgeParamsLayout = findViewById(R.id.edge_params_layout);
            thresholdSlider = findViewById(R.id.threshold_slider);
            ratioSlider = findViewById(R.id.ratio_slider);
            thresholdLabel = findViewById(R.id.threshold_label);
            ratioLabel = findViewById(R.id.ratio_label);
            
            // Check OpenCV version
            try {
                String openCvVersion = getOpenCVVersion();
                Log.i(TAG, "OpenCV Version: " + openCvVersion);
                Toast.makeText(this, "OpenCV: " + openCvVersion, Toast.LENGTH_SHORT).show();
            } catch (UnsatisfiedLinkError e) {
                Log.e(TAG, "Cannot get OpenCV version", e);
                Toast.makeText(this, "Error: OpenCV not loaded properly", Toast.LENGTH_LONG).show();
                return; // Exit early if OpenCV is not available
            }
            
            // Initialize OpenCV
            try {
                Log.d(TAG, "Initializing OpenCV...");
                initOpenCV();
                Log.d(TAG, "OpenCV initialized");
            } catch (UnsatisfiedLinkError e) {
                Log.e(TAG, "Error initializing OpenCV", e);
                Toast.makeText(this, "Error initializing OpenCV", Toast.LENGTH_LONG).show();
                return;
            }
            
            // Check camera permission
            if (hasCameraPermission()) {
                setupCamera();
            } else {
                Log.d(TAG, "Requesting camera permission");
                requestCameraPermission();
            }
            
            // EMERGENCY SOLUTION - GUARANTEED VISIBLE TOGGLE
            toggleButton.setOnClickListener(view -> {
                isEdgeDetectionEnabled = !isEdgeDetectionEnabled;
                Log.d(TAG, "=== TOGGLE PRESSED - EDGE DETECTION: " + isEdgeDetectionEnabled + " ===");
                
                // Update UI immediately
                toggleButton.setText(isEdgeDetectionEnabled ? "Raw Camera" : "Edge Detection");
                edgeParamsLayout.setVisibility(isEdgeDetectionEnabled ? View.VISIBLE : View.GONE);
                
                // CRITICAL: Switch between TextureView and GLSurfaceView
                if (isEdgeDetectionEnabled) {
                    // Show processed output via GLSurfaceView
                    textureView.setVisibility(View.GONE);
                    if (renderer != null && renderer.getSurfaceView() != null) {
                        renderer.getSurfaceView().setVisibility(View.VISIBLE);
                    }
                } else {
                    // Show raw camera via TextureView
                    if (renderer != null && renderer.getSurfaceView() != null) {
                        renderer.getSurfaceView().setVisibility(View.GONE);
                    }
                    textureView.setVisibility(View.VISIBLE);
                }
                
                // CRITICAL: Update renderer with multiple methods
                if (renderer != null) {
                    renderer.isEdgeDetectionEnabled = isEdgeDetectionEnabled;
                    renderer.setEdgeDetectionEnabled(isEdgeDetectionEnabled);
                    
                    // Force immediate render
                    renderer.getSurfaceView().requestRender();
                    
                    Log.d(TAG, "Renderer updated - Edge detection: " + isEdgeDetectionEnabled);
                } else {
                    Log.e(TAG, "CRITICAL ERROR: Renderer is null!");
                }
                
                // Update parameters
                updateCannyParameters();
                
                // Show clear feedback
                String status = isEdgeDetectionEnabled ? "EDGE DETECTION ON - Look for MAGENTA/GREEN patterns" : "RAW CAMERA MODE";
                Toast.makeText(this, status, Toast.LENGTH_LONG).show();
                
                // Log for debugging
                Log.d(TAG, "Toggle complete - State: " + (isEdgeDetectionEnabled ? "EDGE MODE" : "CAMERA MODE"));
            });
            
            // Set up threshold slider
            thresholdSlider.addOnChangeListener((slider, value, fromUser) -> {
                int threshold = (int) value;
                thresholdLabel.setText("Threshold: " + threshold);
                updateCannyParameters();
            });
            
            // Set up ratio slider
            ratioSlider.addOnChangeListener((slider, value, fromUser) -> {
                int ratio = (int) value;
                ratioLabel.setText("Ratio: " + ratio);
                updateCannyParameters();
            });
            
            // Special debug - long press toggle button for JNI edge detection test
            toggleButton.setOnLongClickListener(v -> {
                Log.d(TAG, "Long press detected - running edge detection test");
                try {
                    // Test the native edge detection directly
                    forceEdgeDetectionTest();
                    
                    // Force enable edge detection
                    isEdgeDetectionEnabled = true;
                    toggleButton.setText("Raw Camera");
                    edgeParamsLayout.setVisibility(View.VISIBLE);
                    
                    if (renderer != null) {
                        renderer.isEdgeDetectionEnabled = true;
                        renderer.setEdgeDetectionEnabled(true);
                        renderer.getSurfaceView().requestRender();
                    }
                    
                    Toast.makeText(this, "Edge detection test executed - check logs", 
                                  Toast.LENGTH_LONG).show();
                                  
                } catch (UnsatisfiedLinkError e) {
                    Log.e(TAG, "Native method not found", e);
                    Toast.makeText(this, "Native test failed: " + e.getMessage(), 
                                  Toast.LENGTH_LONG).show();
                }
                return true;
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Error initializing app: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void updateCannyParameters() {
        try {
            int threshold = (int) thresholdSlider.getValue();
            int ratio = (int) ratioSlider.getValue();
            setCannyParameters(threshold, ratio);
            Log.d(TAG, "Updated Canny parameters: threshold=" + threshold + ", ratio=" + ratio);
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "Failed to update Canny parameters", e);
        }
    }
    
    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }
    
    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Camera permission granted");
                setupCamera();
            } else {
                Snackbar.make(textureView, "Camera permission is required", Snackbar.LENGTH_INDEFINITE)
                        .setAction("Grant", v -> requestCameraPermission())
                        .show();
                Log.e(TAG, "Camera permission denied");
            }
        }
    }
    
    private void setupCamera() {
        try {
            Log.d(TAG, "Setting up camera and renderer");
            
            // Setup renderer FIRST
            renderer = new OpenGLRenderer(this);
            if (renderer == null || renderer.getSurfaceView() == null) {
                throw new RuntimeException("Failed to create OpenGL renderer");
            }
            
            // CRITICAL FIX: Add GLSurfaceView to layout alongside TextureView
            androidx.constraintlayout.widget.ConstraintLayout rootLayout = 
                (androidx.constraintlayout.widget.ConstraintLayout) findViewById(android.R.id.content).getRootView().findViewById(R.id.texture_view).getParent();
            
            // Add the GLSurfaceView with the same layout parameters as TextureView
            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams params = 
                new androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
                    androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.MATCH_PARENT,
                    androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.MATCH_PARENT);
            
            renderer.getSurfaceView().setLayoutParams(params);
            renderer.getSurfaceView().setId(View.generateViewId()); // Generate unique ID
            
            // Add GLSurfaceView at index 1 (after TextureView but before controls)
            rootLayout.addView(renderer.getSurfaceView(), 1);
            
            // CRITICAL: Always show GLSurfaceView to ensure surface creation
            textureView.setVisibility(View.VISIBLE);
            renderer.getSurfaceView().setVisibility(View.VISIBLE);
            
            // Force GLSurfaceView to be on top for testing
            renderer.getSurfaceView().bringToFront();
            
            Log.d(TAG, "GLSurfaceView added to layout successfully");
            
            // Setup camera manager with TextureView (as designed)
            cameraHelper = new CameraHelper(this, textureView);
            cameraHelper.setFrameCallback((data, width, height) -> {
                try {
                    if (renderer != null) {
                        renderer.onFrameAvailable(data, width, height);
                    }
                    
                    // Update FPS on UI thread
                    runOnUiThread(() -> {
                        fpsCounter.setText(String.format("FPS: %.1f", cameraHelper.getCurrentFps()));
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error in frame callback", e);
                }
            });
            
            Log.d(TAG, "Camera and renderer setup complete");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up camera", e);
            Snackbar.make(textureView, "Failed to setup camera", Snackbar.LENGTH_LONG).show();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        try {
            // Synchronize edge detection state on resume
            if (renderer != null) {
                // Make sure renderer knows the current state
                renderer.setEdgeDetectionEnabled(isEdgeDetectionEnabled);
                Log.d(TAG, "Resuming renderer with edge detection: " + isEdgeDetectionEnabled);
                renderer.onResume();
            }
            
            if (cameraHelper != null) {
                Log.d(TAG, "Starting camera");
                cameraHelper.startCamera();
            }
            
            // Force update UI elements to match state
            toggleButton.setText(isEdgeDetectionEnabled ? "Raw Camera" : "Edge Detection");
            edgeParamsLayout.setVisibility(isEdgeDetectionEnabled ? View.VISIBLE : View.GONE);
            
            // Force update parameters to ensure they're applied
            updateCannyParameters();
        } catch (Exception e) {
            Log.e(TAG, "Error in onResume", e);
        }
    }
    
    @Override
    protected void onPause() {
        try {
            if (cameraHelper != null) {
                Log.d(TAG, "Stopping camera");
                cameraHelper.stopCamera();
            }
            if (renderer != null) {
                Log.d(TAG, "Pausing renderer");
                renderer.onPause();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onPause", e);
        }
        super.onPause();
    }
}
