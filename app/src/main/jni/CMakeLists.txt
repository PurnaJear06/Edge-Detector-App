cmake_minimum_required(VERSION 3.22.1)
project(EdgeDetector)

# Set OpenCV path - try multiple possible locations
set(OPENCV_SEARCH_PATHS
    ${CMAKE_CURRENT_SOURCE_DIR}/../../../../libs/OpenCV-android-sdk/sdk/native/jni
    ${CMAKE_CURRENT_SOURCE_DIR}/../../../../../libs/OpenCV-android-sdk/sdk/native/jni
    ${CMAKE_SOURCE_DIR}/libs/OpenCV-android-sdk/sdk/native/jni
    ${CMAKE_SOURCE_DIR}/../libs/OpenCV-android-sdk/sdk/native/jni
)

# Try each path
foreach(OPENCV_PATH ${OPENCV_SEARCH_PATHS})
    if(EXISTS ${OPENCV_PATH})
        set(OpenCV_DIR ${OPENCV_PATH})
        message(STATUS "Found OpenCV at: ${OpenCV_DIR}")
        break()
    endif()
endforeach()

# If not found, print error
if(NOT OpenCV_DIR)
    message(FATAL_ERROR "OpenCV not found in any of the search paths. Please check your OpenCV installation.")
endif()

find_package(OpenCV REQUIRED)

# Include OpenCV headers
include_directories(${OpenCV_INCLUDE_DIRS})

# Add edge detector library
add_library(edge-detector SHARED
            edgedetection/edge_detector.cpp
            opencv-check/opencv_check.cpp)

# Link with OpenCV libraries
target_link_libraries(edge-detector ${OpenCV_LIBS})

# Find required Android libraries
find_library(log-lib log)
find_library(android-lib android)
find_library(jnigraphics-lib jnigraphics)
find_library(GLESv2-lib GLESv2)
find_library(EGL-lib EGL)

# Link with Android libraries
target_link_libraries(edge-detector
                     ${log-lib}
                     ${android-lib}
                     ${jnigraphics-lib}
                     ${GLESv2-lib}
                     ${EGL-lib})
