package io.jenkins.plugins.yc;

import yandex.cloud.api.compute.v1.InstanceOuterClass;

import java.util.Optional;

public class YCHostAddressProvider {

    public static String getPrivateIpAddress(YCComputer computer) throws Exception {
        YCAbstractSlave abstractSlave = computer.getNode();
        if(abstractSlave != null && abstractSlave.getInstanceId() != null) {
            YandexTemplate template = computer.getSlaveTemplate();
            if(template != null) {
                InstanceOuterClass.Instance instance = template.getInstanceResponse(abstractSlave.getInstanceId());
                if (instance != null) {
                    Optional<InstanceOuterClass.NetworkInterface> networkInterface = instance.getNetworkInterfacesList().stream().findFirst();
                    if (networkInterface.isPresent()) {
                        String nat_ipaddr = networkInterface.get().getPrimaryV4Address().getOneToOneNat().getAddress();
                        if (nat_ipaddr != null)
                            return nat_ipaddr;
                        else
                            return networkInterface.get().getPrimaryV4Address().getAddress();
                    }
                }
            }
        }
        return "0.0.0.0";
    }
}
