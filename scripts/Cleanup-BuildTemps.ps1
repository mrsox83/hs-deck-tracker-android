param([switch]$Apply)
$ErrorActionPreference = 'Stop'
# Run only after build/test completion. Never clean toolchain caches, outputs or evidence.
$repoRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$buildRoot = Join-Path $repoRoot 'build'
$freedBytes = 0L
if (!(Test-Path -LiteralPath $buildRoot)) { return }
if ((Get-Item -LiteralPath $buildRoot).Attributes -band [IO.FileAttributes]::ReparsePoint) { throw 'Linked build root is not eligible for cleanup' }
foreach ($run in Get-ChildItem -LiteralPath $buildRoot -Directory) {
    if ($run.Attributes -band [IO.FileAttributes]::ReparsePoint) { continue }
    foreach ($module in @('app', 'core')) {
        foreach ($leaf in @('intermediates', 'tmp')) {
            $target = [IO.Path]::GetFullPath((Join-Path $run.FullName "$module\$leaf"))
            if (!$target.StartsWith($buildRoot + '\', [StringComparison]::OrdinalIgnoreCase)) { throw 'Unsafe cleanup path' }
            if (!(Test-Path -LiteralPath $target)) { continue }
            $item = Get-Item -LiteralPath $target
            $parent = Get-Item -LiteralPath $item.Parent.FullName
            if (($item.Attributes -band [IO.FileAttributes]::ReparsePoint) -or
                ($parent.Attributes -band [IO.FileAttributes]::ReparsePoint)) { continue }
            $links = @(Get-ChildItem -LiteralPath $target -Recurse -Force |
                Where-Object { $_.Attributes -band [IO.FileAttributes]::ReparsePoint })
            if ($links.Count) { Write-Warning "Skipped linked directory: $target"; continue }
            $bytes = (Get-ChildItem -LiteralPath $target -Recurse -File -Force | Measure-Object Length -Sum).Sum
            if ($Apply) { Remove-Item -LiteralPath $target -Recurse -Force; $freedBytes += $bytes }
            [pscustomobject]@{Path=$target;MiB=[math]::Round($bytes/1MB,1);Removed=[bool]$Apply}
        }
    }
}
if ($Apply) { "Removed disposable build files: $([math]::Round($freedBytes/1GB,2)) GiB" }
