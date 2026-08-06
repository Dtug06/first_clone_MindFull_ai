$base = "c:\Users\ADMIN\OneDrive\Desktop\first_clone_MindFull_ai\backend\src\main\java\com\mindbridge\behavior\feature\job"
$files = @(
    "persistence\DbDialect.java",
    "persistence\UserDailyFeatureUpsertService.java",
    "persistence\UserDailyFeatureUpsertServiceImpl.java",
    "recorder\JobRunRecorder.java",
    "entity\JobRun.java",
    "entity\JobRunItemLog.java",
    "entity\JobRunStatus.java",
    "entity\JobRunTrigger.java",
    "entity\JobRunItemLogStatus.java",
    "dto\JobRunSummary.java",
    "dto\UserAggregationResult.java",
    "DailyFeatureAggregationProperties.java",
    "DailyFeatureAggregationService.java",
    "DailyFeatureAggregationServiceImpl.java",
    "DailyFeatureAggregationJob.java",
    "repository\JobRunRepository.java",
    "repository\JobRunItemLogRepository.java",
    "cli\DailyFeatureAggregationCliProperties.java",
    "cli\DailyFeatureAggregationCliTarget.java",
    "cli\DailyFeatureAggregationCliTargetParser.java",
    "cli\DailyFeatureAggregationCliRunner.java",
    "entity\UserDailyFeature.java"
)
$allOk = $True
foreach ($f in $files) {
    $path = Join-Path $base $f
    $bytes = [System.IO.File]::ReadAllBytes($path)
    $hasNul = ($bytes -contains 0)
    if ($hasNul) {
        Write-Host "NUL FOUND: $f"
        $allOk = $False
    } else {
        Write-Host "OK: $f ($($bytes.Length) bytes)"
    }
}
if ($allOk) { Write-Host "ALL 22 FILES ARE CLEAN UTF-8" } else { Write-Host "SOME FILES HAVE ISSUES" }
