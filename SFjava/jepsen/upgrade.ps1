Param(
  [Parameter(Mandatory=$true)]
  [string]$version
)

$AppPath = "$PSScriptRoot\jepsen"
Copy-ServiceFabricApplicationPackage -ApplicationPackagePath $AppPath -ApplicationPackagePathInImageStore "jepsen\$version" -ShowProgress
Register-ServiceFabricApplicationType -ApplicationPathInImageStore "jepsen\$version"
Start-ServiceFabricApplicationUpgrade -ApplicationName fabric:/jepsen -ApplicationTypeVersion $version -FailureAction Rollback -Monitored
