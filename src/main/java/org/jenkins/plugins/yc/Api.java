package org.jenkins.plugins.yc;

import yandex.cloud.api.compute.v1.InstanceOuterClass;
import yandex.cloud.api.compute.v1.InstanceServiceOuterClass;
import yandex.cloud.api.operation.OperationOuterClass;

public class Api {

    public static OperationOuterClass.Operation createInstanceResponse(YandexTemplate template, InstanceServiceOuterClass.CreateInstanceRequest instanceRequest) throws Exception {
        return template.getInstanceServiceBlockingStub().create(instanceRequest);
    }

    public static void startInstance(YandexTemplate template, String instanceId) throws Exception {
        template.getInstanceServiceBlockingStub().start(InstanceServiceOuterClass.StartInstanceRequest.newBuilder()
                .setInstanceId(instanceId)
                .build());
    }


    public static void stopInstance(String instanceId, YandexTemplate template) throws Exception {
        if(template != null) {
            template.getInstanceServiceBlockingStub().stop(InstanceServiceOuterClass.StopInstanceRequest.newBuilder()
                    .setInstanceId(instanceId)
                    .build());
        }
    }

    public static InstanceOuterClass.Instance getInstanceResponse(String instanceId, YandexTemplate template) throws Exception {
        if(template != null) {
            return template.getInstanceServiceBlockingStub().get(InstanceServiceOuterClass.GetInstanceRequest.newBuilder()
                    .setInstanceId(instanceId)
                    .build());
        }
        return null;
    }

    public static InstanceServiceOuterClass.ListInstancesResponse getFilterInstanceResponse(YandexTemplate template, String folderId) throws Exception {
        return template.getInstanceServiceBlockingStub().list(InstanceServiceOuterClass.ListInstancesRequest.newBuilder()
                .setFolderId(folderId)
                .setFilter("name=\"".concat(template.getVmName()).concat("\""))
                .build());
    }


    public static void deleteInstanceResponse(String instanceId, YandexTemplate template) throws Exception {
        if(template != null) {
            template.getInstanceServiceBlockingStub().delete(InstanceServiceOuterClass.DeleteInstanceRequest.newBuilder()
                    .setInstanceId(instanceId)
                    .build());
        }
    }

}
