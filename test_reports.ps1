try {
    Write-Host "Testing reports API..."
    
    $fromDate = "2025-04-25"
    $toDate = "2025-05-25"
    
    Write-Host "Testing points report..."
    $pointsResponse = Invoke-WebRequest -Uri "http://localhost:8090/api/reports/points?from=$fromDate&to=$toDate" -Headers @{"Authorization"="Bearer test"} -TimeoutSec 30
    Write-Host "Points report status: $($pointsResponse.StatusCode)"
    if ($pointsResponse.StatusCode -eq 200) {
        $pointsData = $pointsResponse.Content | ConvertFrom-Json
        if ($pointsData.success) {
            Write-Host "✅ Points report loaded successfully! Records: $($pointsData.data.records.Count)"
        } else {
            Write-Host "❌ Points report failed: $($pointsData.errorMessage)"
        }
    }
    
    Write-Host "`nTesting activity report..."
    $activityResponse = Invoke-WebRequest -Uri "http://localhost:8090/api/reports/activity?from=$fromDate&to=$toDate" -Headers @{"Authorization"="Bearer test"} -TimeoutSec 30
    Write-Host "Activity report status: $($activityResponse.StatusCode)"
    if ($activityResponse.StatusCode -eq 200) {
        Write-Host "✅ Activity report loaded successfully!"
    }
    
    Write-Host "`nTesting promotions report..."
    $promotionsResponse = Invoke-WebRequest -Uri "http://localhost:8090/api/reports/promotions?from=$fromDate&to=$toDate" -Headers @{"Authorization"="Bearer test"} -TimeoutSec 30
    Write-Host "Promotions report status: $($promotionsResponse.StatusCode)"
    if ($promotionsResponse.StatusCode -eq 200) {
        Write-Host "✅ Promotions report loaded successfully!"
    }
    
} catch {
    Write-Host "❌ Error: $($_.Exception.Message)"
} 