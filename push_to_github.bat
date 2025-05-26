@echo off
echo Initializing Git repository...
git init

echo Adding all files to staging...
git add .

echo Creating initial commit...
git commit -m "Initial commit: Android Edge Detection App with OpenCV and OpenGL"

echo Setting main branch...
git branch -M main

echo Adding GitHub remote...
git remote add origin https://github.com/PurnaJear06/Edge-Detector-App.git

echo Pushing to GitHub...
git push -u origin main

echo.
echo ✅ Project successfully pushed to GitHub!
echo Repository URL: https://github.com/PurnaJear06/Edge-Detector-App
pause 