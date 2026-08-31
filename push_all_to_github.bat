@echo off
echo ==========================================
echo  Farm To Home - GitHub Push Script
echo ==========================================

echo Staging all files...
git add .

echo Committing changes...
git commit -m "feat: fix PostgreSQL connection pool, add Flyway V12 migration, and full real-time API sync"

echo Pushing to GitHub...
git push origin main

echo.
echo All changes successfully committed and pushed to GitHub!
pause
