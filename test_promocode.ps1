try {
    Write-Host "Testing promo code creation..."
    
    $promoData = @{
        code = "TEST2025"
        expiryDate = "2025-12-31"
        description = "Test promo code"
        bonusPoints = 100
        discountPercent = 10.0
        usesLimit = 1
        active = $true
        used = $false
    }
    
    $jsonData = $promoData | ConvertTo-Json
    Write-Host "Sending promo code data: $jsonData"
    
    $response = Invoke-WebRequest -Uri "http://localhost:8090/api/promo-codes" -Method POST -Body $jsonData -Headers @{"Authorization"="Bearer test"; "Content-Type"="application/json"}
    
    Write-Host "Response status: $($response.StatusCode)"
    Write-Host "Response body: $($response.Content)"
    
    if ($response.StatusCode -eq 201) {
        Write-Host "✅ Promo code created successfully!"
    } else {
        Write-Host "❌ Failed to create promo code"
    }
    
} catch {
    Write-Host "❌ Error: $($_.Exception.Message)"
} 