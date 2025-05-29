try {
    Write-Host "Testing promotions API..."
    $promotions = Invoke-WebRequest -Uri "http://localhost:8090/api/promotions" -Headers @{"Authorization"="Bearer test"}
    Write-Host "Promotions response status: $($promotions.StatusCode)"
    Write-Host "Promotions data length: $($promotions.Content.Length)"
    $promotions.Content | Out-File "promotions_response.json" -Encoding UTF8
    
    Write-Host "`nTesting promo codes API..."
    $promoCodes = Invoke-WebRequest -Uri "http://localhost:8090/api/promo-codes" -Headers @{"Authorization"="Bearer test"}
    Write-Host "Promo codes response status: $($promoCodes.StatusCode)"
    Write-Host "Promo codes data length: $($promoCodes.Content.Length)"
    $promoCodes.Content | Out-File "promo_codes_response.json" -Encoding UTF8
    
    Write-Host "`nTesting statistics API..."
    $statistics = Invoke-WebRequest -Uri "http://localhost:8090/api/promotions/statistics" -Headers @{"Authorization"="Bearer test"}
    Write-Host "Statistics response status: $($statistics.StatusCode)"
    Write-Host "Statistics data length: $($statistics.Content.Length)"
    $statistics.Content | Out-File "statistics_response.json" -Encoding UTF8
    
    Write-Host "`nAll tests completed successfully!"
} catch {
    Write-Host "Error: $($_.Exception.Message)"
} 