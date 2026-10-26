# Run from any directory; keep Russian input/output in UTF-8 on Windows.
$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    [Console]::InputEncoding = [System.Text.UTF8Encoding]::new($false)
    [Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
    $OutputEncoding = [Console]::OutputEncoding
    chcp.com 65001 | Out-Null
    if (-not (Test-Path 'target/s-cloud-1.0.0.jar')) {
        throw 'Build the application first: mvn clean package'
    }
    $javaExecutable = 'java'
    if ($env:JAVA_HOME) {
        $javaExecutable = Join-Path $env:JAVA_HOME 'bin/java.exe'
        if (-not (Test-Path -LiteralPath $javaExecutable)) {
            throw 'JAVA_HOME must point to an installed JDK 21.'
        }
    }
    & $javaExecutable '-Dfile.encoding=UTF-8' '-Dstdin.encoding=UTF-8' '-Dstdout.encoding=UTF-8' '-Dstderr.encoding=UTF-8' -jar target/s-cloud-1.0.0.jar @args
    $applicationExitCode = $LASTEXITCODE
} finally {
    Pop-Location
}
exit $applicationExitCode
