param([string]$OutputDirectory)
$ErrorActionPreference='Stop'
$projectRoot=Split-Path $PSScriptRoot -Parent
if(-not $env:JAVA_HOME -or -not $env:ANDROID_HOME) {throw 'Set JAVA_HOME (JDK 17) and ANDROID_HOME (SDK 35) first.'}
if(-not (Test-Path -LiteralPath "$projectRoot\.signing\keystore.properties")) {throw 'Restore the existing release key and properties from your private backup.'}
Push-Location -LiteralPath $projectRoot
try {
    & .\gradlew.bat :app:assembleRelease :app:lintRelease --console=plain
    if($LASTEXITCODE -ne 0){throw 'Release build or lint failed.'}
    if(-not $OutputDirectory){$OutputDirectory=Join-Path $projectRoot 'releases\rustore'}
    New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null
    $apk=Join-Path $OutputDirectory 'slovoboy-rc3-sound1.apk'
    Copy-Item -LiteralPath 'app\build\outputs\apk\release\app-release.apk' -Destination $apk -Force
    & "$env:ANDROID_HOME\build-tools\35.0.0\apksigner.bat" verify --verbose $apk
    if($LASTEXITCODE -ne 0){throw 'APK signature verification failed.'}
    $hash=Get-FileHash -LiteralPath $apk -Algorithm SHA256
    "$($hash.Hash.ToLower())  $(Split-Path $apk -Leaf)" | Set-Content (Join-Path $OutputDirectory 'SHA256SUMS.txt') -Encoding ascii
    Write-Output "Verified release candidate: $apk"
} finally {Pop-Location}
