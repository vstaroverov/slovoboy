param([string]$OutputDirectory)
$ErrorActionPreference='Stop'
$projectRoot=Split-Path $PSScriptRoot -Parent
if(-not $OutputDirectory){$OutputDirectory=Join-Path $projectRoot 'build-check-rc3'}
$compiler=if($env:JAVA_HOME){Join-Path $env:JAVA_HOME 'bin\javac.exe'}else{'javac'}
$runtime=if($env:JAVA_HOME){Join-Path $env:JAVA_HOME 'bin\java.exe'}else{'java'}
$sources=@('Game','Rules','GameAudio','WordCatalog','StartCountdown','WordHistory','Rc5Catalog') | ForEach-Object {Join-Path $projectRoot "app\src\main\java\ru\serg\slogi\$_.java"}
$sources+=@('DuelRulesCheck','GameCheck','BalanceProbe','GameAudioCheck','UpdateCheck','RoundSessionProbe','LevelCheck') | ForEach-Object {Join-Path $projectRoot "tests\$_.java"}
New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null
& $compiler -encoding UTF-8 -d $OutputDirectory @sources
if($LASTEXITCODE -ne 0){throw 'RC3 checks did not compile'}
& $runtime -cp $OutputDirectory DuelRulesCheck
if($LASTEXITCODE -ne 0){throw 'RC3 regression failed'}

& $runtime -cp $OutputDirectory GameAudioCheck
if($LASTEXITCODE -ne 0){throw 'Game audio transitions failed'}

& $runtime -cp $OutputDirectory UpdateCheck (Join-Path $projectRoot 'tests\fixtures\rc3-single-letter.bin')
if($LASTEXITCODE -ne 0){throw 'Update rules or migration failed'}


& $runtime -cp $OutputDirectory LevelCheck (Join-Path $projectRoot 'web\tests\frames.txt')
if($LASTEXITCODE -ne 0){throw 'Level RC3/RC5 parity failed'}
