package com.purnajear.edgedetectorapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class DebugActivity extends AppCompatActivity {
    private static final String TAG = "EdgeDetectorDebug";
    
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
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);
        
        TextView statusText = findViewById(R.id.status_text);
        TextView versionText = findViewById(R.id.version_text);
        Button continueButton = findViewById(R.id.continue_button);
        
        boolean openCvInitialized = false;
        
        try {
            // Try to get OpenCV version
            String version = getOpenCVVersion();
            versionText.setText("OpenCV Version: " + version);
            Log.d(TAG, "OpenCV Version: " + version);
            
            // Try to initialize OpenCV
            initOpenCV();
            statusText.setText("OpenCV initialized successfully");
            Log.d(TAG, "OpenCV initialized successfully");
            openCvInitialized = true;
        } catch (UnsatisfiedLinkError e) {
            statusText.setText("Error: " + e.getMessage());
            Log.e(TAG, "Native method error", e);
        } catch (Exception e) {
            statusText.setText("Error: " + e.getMessage());
            Log.e(TAG, "Error initializing OpenCV", e);
        }
        
        // Set button state based on initialization success
        continueButton.setEnabled(openCvInitialized);
        
        // Set button click listener
        continueButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        });
    }
} 