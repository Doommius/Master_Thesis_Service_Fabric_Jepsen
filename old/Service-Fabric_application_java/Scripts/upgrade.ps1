Param(
  [Parameter(Mandatory=$true)]
  [string]$version
)

$AppPath = "$DIR/../VotingApplication"
Copy-ServiceFabricApplicationPackage -ApplicationPackagePath $AppPath -ApplicationPackagePathInImageStore "VotingApplication\$version" -ShowProgress
Register-ServiceFabricApplicationType -ApplicationPathInImageStore "VotingApplication\$version"
Start-ServiceFabricApplicationUpgrade -ApplicationName fabric:/VotingApplication -ApplicationTypeVersion $version -FailureAction Rollback -Monitored
