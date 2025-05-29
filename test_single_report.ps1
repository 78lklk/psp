try {
    Write-Host "Testing single points report..."
    
    $response = Invoke-WebRequest -Uri "http://localhost:8090/api/reports/points?from=2025-04-25&to=2025-05-25" -Headers @{"Authorization"="Bearer test"} -TimeoutSec 10
    
    Write-Host "Status: $($response.StatusCode)"
    Write-Host "Content length: $($response.Content.Length)"
    
    if ($response.StatusCode -eq 200) {
        $jsonData = $response.Content | ConvertFrom-Json
        Write-Host "Success: $($jsonData.success)"
        if ($jsonData.success) {
            Write-Host "Records count: $($jsonData.data.records.Count)"
            Write-Host "Points by day count: $($jsonData.data.pointsByDay.Count)"
            
            # Show first few records
            if ($jsonData.data.records.Count -gt 0) {
                Write-Host "First record sample:"
                $jsonData.data.records[0] | ConvertTo-Json -Depth 2
            }
        } else {
            Write-Host "Error: $($jsonData.errorMessage)"
        }
    }
    
} catch {
    Write-Host "Error: $($_.Exception.Message)"
} 