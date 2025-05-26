package com.purnajear.edgedetectorapp.gl;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class OpenGLRenderer implements GLSurfaceView.Renderer {
    private static final String TAG = "OpenGLRenderer";
    
    // Native methods
    public native void nativeProcessFrame(byte[] input, int width, int height, ByteBuffer output);
    public native void nativeInit();
    public native void nativeRelease();
    
    private final Context context;
    private GLSurfaceView surfaceView;
    public boolean isEdgeDetectionEnabled = false; // Making this public for JNI access
    private boolean isInitialized = false;
    
    // Texture
    private int[] textures = new int[1];
    private int textureWidth = 0;
    private int textureHeight = 0;
    
    // Shader program
    private int shaderProgram;
    private int positionAttrHandle;
    private int texCoordAttrHandle;
    private int textureUniformHandle;
    
    // Buffers
    private FloatBuffer vertexBuffer;
    private FloatBuffer textureBuffer;
    private ByteBuffer frameData;
    
    // VBO vertices for the quad
    private static final float[] VERTICES = {
            -1.0f, -1.0f, 0.0f,  // bottom left
            1.0f, -1.0f, 0.0f,   // bottom right
            -1.0f, 1.0f, 0.0f,   // top left
            1.0f, 1.0f, 0.0f     // top right
    };
    
    // Texture coordinates
    private static final float[] TEXTURE_COORDS = {
            0.0f, 1.0f,  // bottom left
            1.0f, 1.0f,  // bottom right
            0.0f, 0.0f,  // top left
            1.0f, 0.0f   // top right
    };
    
    // Shader sources
    private static final String VERTEX_SHADER = 
            "attribute vec4 aPosition;\n" +
            "attribute vec2 aTexCoord;\n" +
            "varying vec2 vTexCoord;\n" +
            "void main() {\n" +
            "  gl_Position = aPosition;\n" +
            "  vTexCoord = aTexCoord;\n" +
            "}";
            
    private static final String FRAGMENT_SHADER = 
            "precision mediump float;\n" +
            "varying vec2 vTexCoord;\n" +
            "uniform sampler2D uTexture;\n" +
            "void main() {\n" +
            "  gl_FragColor = texture2D(uTexture, vTexCoord);\n" +
            "}";
    
    public OpenGLRenderer(Context context) {
        this.context = context;
        
        try {
            // Init native
            Log.d(TAG, "Initializing native code");
            nativeInit();
            
            // Initialize buffers
            ByteBuffer bb = ByteBuffer.allocateDirect(VERTICES.length * 4); // 4 bytes per float
            bb.order(ByteOrder.nativeOrder());
            vertexBuffer = bb.asFloatBuffer();
            vertexBuffer.put(VERTICES);
            vertexBuffer.position(0);
            
            bb = ByteBuffer.allocateDirect(TEXTURE_COORDS.length * 4); // 4 bytes per float
            bb.order(ByteOrder.nativeOrder());
            textureBuffer = bb.asFloatBuffer();
            textureBuffer.put(TEXTURE_COORDS);
            textureBuffer.position(0);
            
            // Create GLSurfaceView
            surfaceView = new GLSurfaceView(context);
            surfaceView.setEGLContextClientVersion(2);
            surfaceView.setRenderer(this);
            surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY); // Force continuous rendering
            
            Log.d(TAG, "OpenGLRenderer initialization complete");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing OpenGLRenderer", e);
        }
    }
    
    public GLSurfaceView getSurfaceView() {
        return surfaceView;
    }
    
    public void setEdgeDetectionEnabled(boolean enabled) {
        isEdgeDetectionEnabled = enabled;
        Log.d(TAG, "Edge detection mode set to: " + enabled);
    }
    
    // Added getter for JNI access as fallback
    public boolean getEdgeDetectionState() {
        Log.d(TAG, "getEdgeDetectionState called, returning: " + isEdgeDetectionEnabled);
        return isEdgeDetectionEnabled;
    }
    
    public void onResume() {
        if (surfaceView != null) {
            try {
                surfaceView.onResume();
            } catch (Exception e) {
                Log.e(TAG, "Error resuming OpenGL surface", e);
            }
        }
    }
    
    public void onPause() {
        if (surfaceView != null) {
            try {
                surfaceView.onPause();
            } catch (Exception e) {
                Log.e(TAG, "Error pausing OpenGL surface", e);
            }
        }
    }
    
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        try {
            // Set clear color to black
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
            
            // Create shader program
            shaderProgram = createShaderProgram(VERTEX_SHADER, FRAGMENT_SHADER);
            if (shaderProgram == 0) {
                Log.e(TAG, "Failed to create shader program");
                return;
            }
            
            // Get handles
            positionAttrHandle = GLES20.glGetAttribLocation(shaderProgram, "aPosition");
            texCoordAttrHandle = GLES20.glGetAttribLocation(shaderProgram, "aTexCoord");
            textureUniformHandle = GLES20.glGetUniformLocation(shaderProgram, "uTexture");
            
            // Generate texture
            GLES20.glGenTextures(1, textures, 0);
            
            isInitialized = true;
            Log.d(TAG, "OpenGL surface created successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error creating OpenGL surface", e);
        }
    }
    
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        Log.d(TAG, "OpenGL surface changed: " + width + "x" + height);
    }
    
    @Override
    public void onDrawFrame(GL10 gl) {
        try {
            // Clear the screen
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            
            // Skip if no frame data
            if (frameData == null || textureWidth <= 0 || textureHeight <= 0) {
                return;
            }
            
            // Use shader program
            GLES20.glUseProgram(shaderProgram);
            
            // Set texture
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
            
            // Update texture with new frame data
            updateTexture();
            
            // Set texture uniform
            GLES20.glUniform1i(textureUniformHandle, 0);
            
            // Set vertex attributes
            GLES20.glVertexAttribPointer(positionAttrHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);
            GLES20.glEnableVertexAttribArray(positionAttrHandle);
            
            GLES20.glVertexAttribPointer(texCoordAttrHandle, 2, GLES20.GL_FLOAT, false, 0, textureBuffer);
            GLES20.glEnableVertexAttribArray(texCoordAttrHandle);
            
            // Draw quad
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
            
            // Clean up
            GLES20.glDisableVertexAttribArray(positionAttrHandle);
            GLES20.glDisableVertexAttribArray(texCoordAttrHandle);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        } catch (Exception e) {
            Log.e(TAG, "Error drawing frame", e);
        }
    }
    
    private void updateTexture() {
        try {
            // Configure texture
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            
            // TODO: Might optimize later with grayscale shader
            
            // Upload texture data
            frameData.position(0);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, textureWidth, textureHeight,
                    0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, frameData);
        } catch (Exception e) {
            Log.e(TAG, "Error updating texture", e);
        }
    }
    
    public void onFrameAvailable(byte[] data, int width, int height) {
        try {
            if (width <= 0 || height <= 0 || data == null) {
                Log.e(TAG, "Invalid frame data: " + (data == null ? "null" : "width=" + width + ", height=" + height));
                return;
            }
            
            if (!isInitialized) {
                Log.d(TAG, "OpenGL not initialized yet, skipping frame");
                return;
            }
            
            // Periodically log edge detection state for debugging
            if (System.currentTimeMillis() % 1000 < 50) { // Log roughly every second
                Log.d(TAG, "Current edge detection state: " + (isEdgeDetectionEnabled ? "ENABLED" : "DISABLED"));
            }
            
            textureWidth = width;
            textureHeight = height;
            
            // Allocate direct ByteBuffer if needed
            int bufferSize = width * height * 4; // RGBA
            if (frameData == null || frameData.capacity() < bufferSize) {
                // Use direct ByteBuffer for better performance with native code
                frameData = ByteBuffer.allocateDirect(bufferSize);
                frameData.order(ByteOrder.nativeOrder());
                Log.d(TAG, "Created direct frame buffer: " + width + "x" + height + ", size: " + (bufferSize / 1024) + "KB");
            }
            
            long startTime = System.nanoTime();
            
            // Process frame with OpenCV native code using direct buffer
            try {
                frameData.clear(); // Reset position and limits
                
                // Log if edge detection is enabled for this frame
                if (isEdgeDetectionEnabled) {
                    Log.d(TAG, "Processing frame with edge detection ENABLED");
                }
                
                nativeProcessFrame(data, width, height, frameData);
            } catch (UnsatisfiedLinkError e) {
                Log.e(TAG, "Native method not found", e);
                return;
            } catch (Exception e) {
                Log.e(TAG, "Error in native code processing frame", e);
                return;
            }
            
            long processingTime = System.nanoTime() - startTime;
            double processingTimeMs = processingTime / 1_000_000.0;
            
            // Always log processing time for debugging
            Log.d(TAG, String.format("Frame processing time: %.2f ms, edge detection: %s", 
                  processingTimeMs, isEdgeDetectionEnabled ? "ON" : "OFF"));
            
            // Request render
            if (surfaceView != null) {
                surfaceView.requestRender();
                
                // If edge detection is enabled, add a second render request to ensure display updates
                if (isEdgeDetectionEnabled) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        surfaceView.requestRender();
                    });
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing frame", e);
        }
    }
    
    private int createShaderProgram(String vertexShader, String fragmentShader) {
        int program = GLES20.glCreateProgram();
        
        // Compile shaders
        int vertexShaderId = compileShader(GLES20.GL_VERTEX_SHADER, vertexShader);
        int fragmentShaderId = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);
        
        if (vertexShaderId == 0 || fragmentShaderId == 0) {
            return 0;
        }
        
        // Attach shaders to program
        GLES20.glAttachShader(program, vertexShaderId);
        GLES20.glAttachShader(program, fragmentShaderId);
        
        // Link program
        GLES20.glLinkProgram(program);
        
        // Check link status
        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] != GLES20.GL_TRUE) {
            String error = GLES20.glGetProgramInfoLog(program);
            Log.e(TAG, "Error linking program: " + error);
            GLES20.glDeleteProgram(program);
            return 0;
        }
        
        // Delete shaders
        GLES20.glDeleteShader(vertexShaderId);
        GLES20.glDeleteShader(fragmentShaderId);
        
        return program;
    }
    
    private int compileShader(int type, String source) {
        int shader = GLES20.glCreateShader(type);
        
        // Compile shader
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        
        // Check compile status
        int[] compileStatus = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);
        if (compileStatus[0] != GLES20.GL_TRUE) {
            String error = GLES20.glGetShaderInfoLog(shader);
            Log.e(TAG, "Error compiling shader: " + error);
            GLES20.glDeleteShader(shader);
            return 0;
        }
        
        return shader;
    }
    
    @Override
    protected void finalize() throws Throwable {
        try {
            nativeRelease();
        } catch (Exception e) {
            Log.e(TAG, "Error releasing native resources", e);
        }
        super.finalize();
    }
}
