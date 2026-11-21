param(
    [Parameter(Mandatory = $true)]
    [string]$Area,
    [string]$Level = '高中',
    [switch]$Apply,
    [switch]$Overwrite
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$classpathFile = Join-Path $projectRoot 'target\oss-classpath.txt'
$toolSource = Join-Path $PSScriptRoot 'ImportSchoolDetailsFromHar.java'
$toolClasses = Join-Path $projectRoot 'target\tools'
$mainClasses = Join-Path $projectRoot 'target\classes'

if (-not (Test-Path -LiteralPath $classpathFile)) {
    throw 'Missing target\oss-classpath.txt. Build the Maven dependency classpath first.'
}

$dependencyClasspath = Get-Content -LiteralPath $classpathFile -Raw
New-Item -ItemType Directory -Path $toolClasses -Force | Out-Null

& javac -encoding UTF-8 -cp "$mainClasses;$dependencyClasspath" -d $toolClasses $toolSource
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$arguments = @(
    '-Dfile.encoding=UTF-8',
    '-cp', "$toolClasses;$mainClasses;$dependencyClasspath",
    'ImportSchoolDetailsFromHar',
    '--area', $Area
    '--level', $Level
)
if ($Apply) { $arguments += '--apply' }
if ($Overwrite) { $arguments += '--overwrite' }

Push-Location $projectRoot
try {
    & java @arguments
    exit $LASTEXITCODE
} finally {
    Pop-Location
}
