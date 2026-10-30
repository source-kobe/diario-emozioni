# Costruisce Diario Emozioni.apk senza Gradle: aapt2 -> javac -> d8 -> zipalign -> apksigner.
$ErrorActionPreference = "Stop"

# A ogni release: codice +1 e nome nuovo. Il nome deve coincidere col tag su
# GitHub (senza la "v"): è con quello che l'app capisce se c'è un aggiornamento.
$versioneCodice = 3
$versioneNome   = "1.2-beta"

$base = Split-Path -Parent $MyInvocation.MyCommand.Path
# SDK e JDK: li cerco dove possono stare, invece di fissare un percorso
$sdk  = @($env:ANDROID_HOME, "$env:LOCALAPPDATA\Android\Sdk", "C:\Android\Sdk") |
        Where-Object { $_ -and (Test-Path "$_\platforms\android-34\android.jar") } | Select-Object -First 1
if (-not $sdk) { throw "Android SDK con la piattaforma android-34 non trovato" }
# build-tools più recenti: il d8 della 34 non digerisce le classi prodotte dal JDK 21
$bt   = (Get-ChildItem "$sdk\build-tools" -Directory |
         Sort-Object { [version]($_.Name -replace '-.*$', '') } -Descending | Select-Object -First 1).FullName
$jar  = "$sdk\platforms\android-34\android.jar"
$jdk  = (Get-ChildItem "C:\Program Files\Eclipse Adoptium" -Directory |
         Where-Object Name -like "jdk-*" | Sort-Object Name -Descending | Select-Object -First 1).FullName
if (-not $jdk) { throw "JDK non trovato in C:\Program Files\Eclipse Adoptium" }

$env:JAVA_HOME = $jdk          # d8 e apksigner lo cercano nell'ambiente
$env:ANDROID_HOME = $sdk

$proj  = "$base\android"
$build = "$base\build"
$out   = "$base\Diario Emozioni.apk"

if (Test-Path $build) { Remove-Item -Recurse -Force $build }
New-Item -ItemType Directory -Force "$build\gen", "$build\classes", "$build\dex", "$build\assets" | Out-Null

# --- asset: l'interfaccia web e i font, dentro l'APK ---------------------
# aapt2 su Windows scrive i path annidati con il backslash, che AssetManager
# non risolve: i font stanno quindi nella radice di assets/ e la copia
# dell'HTML destinata all'APK punta li'.
Copy-Item "$base\fonts\*.woff2" "$build\assets\"
(Get-Content "$base\index.html" -Raw -Encoding utf8).Replace('url("fonts/', 'url("') |
    Set-Content "$build\assets\index.html" -Encoding utf8 -NoNewline

# --- 1. risorse ----------------------------------------------------------
Write-Host "[1/6] compilo le risorse"
& "$bt\aapt2.exe" compile --dir "$proj\res" -o "$build\res.zip"
if ($LASTEXITCODE) { throw "aapt2 compile fallito" }

Write-Host "[2/6] collego risorse, manifest e asset"
& "$bt\aapt2.exe" link `
    -o "$build\base.apk" `
    -I $jar `
    --manifest "$proj\AndroidManifest.xml" `
    -A "$build\assets" `
    --java "$build\gen" `
    --min-sdk-version 26 `
    --target-sdk-version 34 `
    --version-code $versioneCodice `
    --version-name $versioneNome `
    "$build\res.zip"
if ($LASTEXITCODE) { throw "aapt2 link fallito" }

# --- 2. codice -----------------------------------------------------------
Write-Host "[3/6] compilo il codice Java"
$sorgenti = @(Get-ChildItem -Recurse "$proj\java" -Filter *.java | ForEach-Object FullName) +
            @(Get-ChildItem -Recurse "$build\gen" -Filter R.java | ForEach-Object FullName)
& "$jdk\bin\javac.exe" -source 8 -target 8 -nowarn -Xlint:-options `
    -classpath $jar -d "$build\classes" $sorgenti
if ($LASTEXITCODE) { throw "javac fallito" }

Write-Host "[4/6] converto in dex"
$classi = Get-ChildItem -Recurse "$build\classes" -Filter *.class | ForEach-Object FullName
& "$bt\d8.bat" --lib $jar --min-api 26 --output "$build\dex" $classi
if ($LASTEXITCODE) { throw "d8 fallito" }

# --- 3. impacchetta ------------------------------------------------------
Write-Host "[5/6] inserisco classes.dex nell'APK"
Add-Type -AssemblyName System.IO.Compression.FileSystem
Add-Type -AssemblyName System.IO.Compression
$zip = [System.IO.Compression.ZipFile]::Open("$build\base.apk", "Update")
$voce = $zip.CreateEntry("classes.dex", [System.IO.Compression.CompressionLevel]::Optimal)
$fs = $voce.Open()
$byte = [System.IO.File]::ReadAllBytes("$build\dex\classes.dex")
$fs.Write($byte, 0, $byte.Length)
$fs.Close()
$zip.Dispose()

# --- 4. allinea e firma --------------------------------------------------
Write-Host "[6/6] allineo e firmo"
& "$bt\zipalign.exe" -f -p 4 "$build\base.apk" "$build\aligned.apk"
if ($LASTEXITCODE) { throw "zipalign fallito" }

# La chiave e la sua password non stanno nel repository: chi ha entrambe può
# firmare aggiornamenti che Android accetta come nostri. La password arriva
# dalla variabile DIARIO_KS_PASS oppure la chiedo; la passo ai programmi
# attraverso l'ambiente, così non compare nella riga di comando.
if (-not $env:DIARIO_KS_PASS) {
    $sicura = Read-Host "Password della chiave di firma" -AsSecureString
    $env:DIARIO_KS_PASS = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
        [Runtime.InteropServices.Marshal]::SecureStringToBSTR($sicura))
}

$ks = "$base\chiave-diario.jks"
if (-not (Test-Path $ks)) {
    & "$jdk\bin\keytool.exe" -genkeypair -v `
        -keystore $ks -storepass:env DIARIO_KS_PASS -keypass:env DIARIO_KS_PASS `
        -alias diario -keyalg RSA -keysize 2048 -validity 10950 `
        -dname "CN=Diario Emozioni, OU=personale, O=personale, L=-, S=-, C=IT" | Out-Null
}

if (Test-Path $out) { Remove-Item -Force $out }
& "$bt\apksigner.bat" sign `
    --ks $ks --ks-pass env:DIARIO_KS_PASS --key-pass env:DIARIO_KS_PASS `
    --v1-signing-enabled true --v2-signing-enabled true `
    --out $out "$build\aligned.apk"
if ($LASTEXITCODE) { throw "apksigner fallito" }

# (in una pipeline con Select-Object il codice di uscita risulta sporco: raccolgo e stampo)
$certi = & "$bt\apksigner.bat" verify --print-certs $out
$certi | Where-Object { $_ -match "certificate DN|SHA-256" } | ForEach-Object { Write-Host $_ }
Write-Host ""
Write-Host ("FATTO -> " + $out + "  (" + [math]::Round((Get-Item $out).Length/1MB, 2) + " MB)")
