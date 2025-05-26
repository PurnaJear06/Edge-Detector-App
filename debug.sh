#!/bin/bash

# Clear logcat
adb logcat -c

# Run logcat with filters for our app and OpenCV-related errors
adb logcat EdgeDetector:V EdgeDetectorDebug:V *:E | grep -E "EdgeDetector|OpenCV|edge-detector|UnsatisfiedLinkError" 