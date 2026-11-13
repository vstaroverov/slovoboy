param([string]$OutputDirectory)
$ErrorActionPreference='Stop'
$projectRoot=Split-Path $PSScriptRoot -Parent
if(-not $OutputDirectory){$OutputDirectory=Join-Path $projectRoot 'build-check-rc3'}
$compiler=if($env:JAVA_HOME){Join-Path $env:JAVA_HOME 'bin\javac.exe'}else{'javac'}
$runtime=if($env:JAVA_HOME){Join-Path $env:JAVA_HOME 'bin\java.exe'}else{'java'}
$sources=@('Game','Rules','GameAudio') | ForEach-Object {Join-Path $projectRoot "app\src\main\java\ru\serg\slogi\$_.java"}
$sources+=@('DuelRulesCheck','GameCheck','BalanceProbe','GameAudioCheck') | ForEach-Object {Join-Path $projectRoot "tests\$_.java"}
New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null
& $compiler -encoding UTF-8 -d $OutputDirectory @sources
if($LASTEXITCODE -ne 0){throw 'RC3 checks did not compile'}
& $runtime -cp $OutputDirectory DuelRulesCheck
if($LASTEXITCODE -ne 0){throw 'RC3 regression failed'}

& $runtime -cp $OutputDirectory GameAudioCheck
if($LASTEXITCODE -ne 0){throw 'Game audio transitions failed'}
