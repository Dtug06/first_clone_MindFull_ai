param(
    [string]$Name
)

$ErrorActionPreference = "Stop"

$base = "http://localhost:8080/api/v1"
$aliceJson = Get-Content "C:\Users\ADMIN\OneDrive\Desktop\first_clone_MindFull_ai\alice_login.json" -Raw | ConvertFrom-Json
$bobJson = Get-Content "C:\Users\ADMIN\OneDrive\Desktop\first_clone_MindFull_ai\bob_login.json" -Raw | ConvertFrom-Json
$aliceToken = $aliceJson.accessToken
$aliceId = $aliceJson.user.id
$bobToken = $bobJson.accessToken
$bobId = $bobJson.user.id

function Call-Api {
    param(
        [string]$Method,
        [string]$Path,
        [string]$Token = "",
        [string]$Body = "",
        [string]$ExtraHeaders = ""
    )
    $headers = @()
    if ($Token) { $headers += "Authorization: Bearer $Token" }
    if ($Body) { $headers += "Content-Type: application/json" }
    if ($ExtraHeaders) { $headers += $ExtraHeaders }
    $headerStr = $headers -join "`r`n"
    $cmd = if ($Body) {
        "curl.exe -s -w ""`r`n___HTTP=%{http_code}`r`n"" -X $Method `"$base$Path`" $headerLine --data-binary @body.json" -replace 'headerLine',$headerStr
    } else {
        "curl.exe -s -w ""`r`n___HTTP=%{http_code}`r`n"" -X $Method `"$base$Path`" $headerStr"
    }
    if ($Body) {
        $Body | Out-File -FilePath "C:\Users\ADMIN\OneDrive\Desktop\first_clone_MindFull_ai\body.json" -Encoding utf8
    }
    Write-Host "[$Name] $Method $Path"
    cmd.exe /c $cmd 2>&1 | Select-Object -Skip 0
    Write-Host "---"
}

switch ($Name) {
    "T07_users_me" {
        Call-Api GET "/users/me" $aliceToken
        Call-Api GET "/users/me" $bobToken
        Call-Api GET "/users/me"
    }
    "T08_consent_grant" {
        $body = '{"consentType":"CHAT_ANALYSIS","action":"GRANTED","policyVersion":"v1"}'
        Call-Api POST "/consents" $aliceToken $body
        Call-Api GET "/consents/current" $aliceToken
        Call-Api POST "/consents" $aliceToken '{"consentType":"CHAT_ANALYSIS","action":"REVOKED","policyVersion":"v1"}'
        Call-Api GET "/consents/current" $aliceToken
        Call-Api GET "/consents/current" $bobToken
    }
    "T09_trace" {
        $reqId = "test-req-$(Get-Date -Format HHmmssfff)"
        $extra = "X-Request-Id: $reqId"
        Call-Api GET "/actuator/health" "" "" $extra
    }
    default {
        Write-Host "Unknown test: $Name"
        Write-Host "Available: T07_users_me, T08_consent_grant, T09_trace"
    }
}