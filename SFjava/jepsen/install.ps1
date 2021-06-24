function Uninstall() {
    Write-Host "Uninstalling App as installation failed... Please try installation again."
    Invoke-Expression "& $PSScriptRoot\uninstall.ps1"
    Exit
}

$AppPath = "$PSScriptRoot\jepsen"
Copy-ServiceFabricApplicationPackage -ApplicationPackagePath $AppPath -ApplicationPackagePathInImageStore jepsen -ShowProgress
if (!$?) {
    Uninstall
}

Register-ServiceFabricApplicationType jepsen
if (!$?) {
    Uninstall
}

New-ServiceFabricApplication fabric:/jepsen jepsenType 1.0.0
if (!$?) {
    Uninstall
}