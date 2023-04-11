# Description for cloud setting  
ForlderId – required, ID of the folder to create an instance in. The maximum string length in characters is 50.  
InitVmTemplate – required, string in the form of the following pattern  
```
 platform_id: 'standard-v3'  
 resources_spec: {
  memory: 1073741824  
  cores: 2  
  core_fraction: 20  
 }  
 boot_disk_spec: {  
  mode: READ_WRITE  
  disk_spec: {  
   type_id: 'network-hdd'  
   size: 16106127360  
   image_id: 'fd87ap2ld09bjiotu5v0'  
  }  
 auto_delete: true  
 }  
 network_interface_specs: {  
  subnet_id: 'e2l8m8rsiq7mbsusb9ps'  
  primary_v4_address_spec: {  
   one_to_one_nat_spec: {  
    ip_version: IPV4  
   }  
  }  
 }  
 scheduling_policy: {  
  preemptible: true  
 }  
```
Name - required, name of the instance  
Credentials - required, credentials from your service account for connection  
YC Key Pair's Private Key - required, your private ssh for vm connection  
Description - description of the instance  
Labels - label string for job  
InitScript - script to run on a virtual machine at startup  
IdleTerminationMinutes - minutes variables for non stopping vm work. If empty then vm work non-stop  
StopOnTerminate - stop VM or delete  
RemoteUser - VM user, default root