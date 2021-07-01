$ClusterName= "jepsen.northeurope.cloudapp.azure.com:19080"

$Certthumprint = "E58AE7E7077522C401927883CFC3E610411134B1"

Connect-ServiceFabricCluster -ConnectionEndpoint $ClusterName -KeepAliveIntervalInSec 10 `

     -X509Credential `

     -ServerCertThumbprint $Certthumprint  `

     -FindType FindByThumbprint `

     -FindValue $Certthumprint `

     -StoreLocation CurrentUser `

     -StoreName My

$ApplicationName = "fabric:/Jepsen"

 

foreach($node in Get-ServiceFabricNode)

{  

 [void](Get-ServiceFabricDeployedReplica -NodeName $node.NodeName -ApplicationName $ApplicationName | Remove-ServiceFabricReplica -NodeName $node.NodeName -ForceRemove)

}

Remove-ServiceFabricApplication -ApplicationName $ApplicationName -Force

 