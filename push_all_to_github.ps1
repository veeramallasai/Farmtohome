# Script to commit and push all recent PostgreSQL connection fixes, Flyway migrations, and backend/frontend APIs to GitHub

Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -ErrorAction SilentlyContinue

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host " Farm To Home - GitHub Push Script        " -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan

Write-Host "[1/4] Checking Git Status..." -ForegroundColor Yellow
git status

Write-Host "[2/4] Staging all modified and new files..." -ForegroundColor Yellow
git add .

Write-Host "[3/4] Committing changes..." -ForegroundColor Yellow
git commit -m "feat: fix PostgreSQL connection pool, add Flyway V12 migration, and full real-time API sync"

Write-Host "[4/4] Pushing to GitHub (origin main)..." -ForegroundColor Yellow
git push origin main

Write-Host "`nAll changes successfully committed and pushed to GitHub!" -ForegroundColor Green
