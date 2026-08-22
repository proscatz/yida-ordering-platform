param(
    [string]$Root = (Split-Path -Parent $PSScriptRoot)
)

$ErrorActionPreference = 'Stop'
$resolvedRoot = (Resolve-Path -LiteralPath $Root).Path
$excludedDirectories = @('.git', '.idea', '.vscode', 'node_modules', 'target', 'dist', 'coverage')
$blockedNames = @(
    'application-dev.yml',
    'application-local.yml',
    'application-prod.yml',
    'dataSources.xml',
    'dataSources.local.xml'
)
$blockedExtensions = @('.pem', '.key', '.p12', '.pfx', '.jks', '.keystore')
$textExtensions = @('.java', '.xml', '.yml', '.yaml', '.properties', '.sql', '.js', '.mjs', '.ts', '.vue', '.json', '.md', '.env', '.txt')
$maxFileBytes = 10MB

$legacyCnA = -join (0x82CD, 0x7A79, 0x5916, 0x5356 | ForEach-Object { [char]$_ })
$legacyCnB = -join (0x745E, 0x5409, 0x5916, 0x5356 | ForEach-Object { [char]$_ })
$rules = [ordered]@{
    'legacy-brand' = "(?i)$legacyCnA|$legacyCnB|$('it' + 'cast')|$('it' + 'heima')|$('vue' + '[\\s_-]*typescript[\\s_-]*admin')"
    'private-key' = '-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----'
    'cloud-access-key' = '(?i)(?:LTAI[A-Za-z0-9]{12,}|AKIA[A-Z0-9]{16})'
    'identity-card-in-sql' = '(?<!\d)\d{17}[0-9Xx](?!\w)'
    'phone-in-sql' = '(?<!\d)1[3-9]\d{9}(?!\d)'
}

$issues = [System.Collections.Generic.List[object]]::new()

function Add-Issue([string]$Rule, [string]$Path) {
    $issues.Add([pscustomobject]@{ Rule = $Rule; Path = $Path })
}

$files = Get-ChildItem -LiteralPath $resolvedRoot -Recurse -Force -File | Where-Object {
    $relative = $_.FullName.Substring($resolvedRoot.Length).TrimStart('\')
    $segments = $relative -split '[\\/]'
    -not ($segments | Where-Object { $excludedDirectories -contains $_ })
}

foreach ($file in $files) {
    $relative = $file.FullName.Substring($resolvedRoot.Length).TrimStart('\')
    if ($blockedNames -contains $file.Name) { Add-Issue 'blocked-file-name' $relative }
    if ($blockedExtensions -contains $file.Extension.ToLowerInvariant()) { Add-Issue 'blocked-secret-extension' $relative }
    if ($file.Length -gt $maxFileBytes) { Add-Issue 'file-over-10mb' $relative }

    $isText = $textExtensions -contains $file.Extension.ToLowerInvariant() -or $file.Name.StartsWith('.env')
    if (-not $isText) { continue }

    $content = Get-Content -LiteralPath $file.FullName -Raw -ErrorAction SilentlyContinue
    if ($null -eq $content) { continue }

    foreach ($rule in $rules.GetEnumerator()) {
        if (($rule.Key -like '*-in-sql') -and $file.Extension -ne '.sql') { continue }
        if ([regex]::IsMatch($content, $rule.Value)) { Add-Issue $rule.Key $relative }
    }

    if ($file.Extension -in @('.yml', '.yaml', '.properties') -or $file.Name.StartsWith('.env')) {
        $literalCredential = '(?im)^[ \t]*(?:password|secret|access-key-secret|admin-secret-key|user-secret-key)[ \t]*[:=](?>[ \t]*)(?!\$\{|change-me[ \t]*$|guest[ \t]*$|$)[^#\r\n]+'
        if ([regex]::IsMatch($content, $literalCredential)) { Add-Issue 'literal-credential' $relative }
    }
}

Write-Output "[publication-check] root=$resolvedRoot"
Write-Output "[publication-check] files=$($files.Count) issues=$($issues.Count)"
foreach ($issue in $issues | Sort-Object Rule, Path -Unique) {
    Write-Output "[publication-check] FAIL rule=$($issue.Rule) file=$($issue.Path)"
}

if ($issues.Count -gt 0) {
    exit 1
}

Write-Output '[publication-check] PASS'
