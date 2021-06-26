function Uninstall() {
    Write-Host "Uninstalling App as installation failed... Please try installation again."
    Invoke-Expression "& $PSScriptRoot\uninstall.ps1"
    Exit
}

$AppPath = "$DIR..\VotingApplication"
$version = "1.0.0"

Copy-ServiceFabricApplicationPackage -ApplicationPackagePath $AppPath -ApplicationPackagePathInImageStore VotingApplication -ShowProgress
if (!$?) {
    Uninstall
}

Register-ServiceFabricApplicationType VotingApplication
if (!$?) {
    Uninstall
}

New-ServiceFabricApplication -ApplicationName fabric:/VotingApplication -ApplicationTypeName VotingApplicationType --app-version $version
if (!$?) {
    Uninstall
}

Copy-ServiceFabricApplicationPackage -ApplicationPackagePath ../VotingApplication -ApplicationPackagePathInImageStore VotingApplication -ShowProgress