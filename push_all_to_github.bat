@echo off
echo ==========================================
echo  Farm To Home - GitHub Push Script
echo ==========================================

echo Staging all files...
git add .

echo Committing changes...
git commit -m "fix(backend): add missing setRating and setReviewCount setters to ProductEntity for clean build and Railway deployment"

echo Pushing to GitHub...
git push origin main

echo.
echo All changes successfully committed and pushed to GitHub!
pause
