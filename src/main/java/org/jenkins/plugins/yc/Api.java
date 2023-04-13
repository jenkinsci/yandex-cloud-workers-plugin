package org.jenkins.plugins.yc;

import yandex.cloud.api.compute.v1.InstanceOuterClass;
import yandex.cloud.api.compute.v1.InstanceServiceOuterClass;
import yandex.cloud.api.operation.OperationOuterClass;

public class Api {

    public static OperationOuterClass.Operation createInstanceResponse(YandexTemplate template, InstanceServiceOuterClass.CreateInstanceRequest instanceRequest) throws Exception {
        return template.parent.getInstanceServiceBlockingStub().create(instanceRequest);
    }

    public static void startInstance(YandexTemplate template, String instanceId) throws Exception {
        template.parent.getInstanceServiceBlockingStub().start(InstanceServiceOuterClass.StartInstanceRequest.newBuilder()
                .setInstanceId(instanceId)
                .build());
    }


    public static void stopInstance(String instanceId, AbstractCloud cloud) throws Exception {
        cloud.getInstanceServiceBlockingStub().stop(InstanceServiceOuterClass.StopInstanceRequest.newBuilder()
                .setInstanceId(instanceId)
                .build());
    }

    public static InstanceOuterClass.Instance getInstanceResponse(String instanceId, AbstractCloud cloud) throws Exception {
        return cloud.getInstanceServiceBlockingStub().get(InstanceServiceOuterClass.GetInstanceRequest.newBuilder()
                .setInstanceId(instanceId)
                .build());
    }

    public static InstanceServiceOuterClass.ListInstancesResponse getFilterInstanceResponse(YandexTemplate template) throws Exception {
        return template.parent.getInstanceServiceBlockingStub().list(InstanceServiceOuterClass.ListInstancesRequest.newBuilder()
                .setFolderId(template.parent.folderId)
                .setFilter("name=\"".concat(template.parent.name).concat("\""))
                .build());
    }


    public static void deleteInstanceResponse(String instanceId, AbstractCloud cloud) throws Exception {
        cloud.getInstanceServiceBlockingStub().delete(InstanceServiceOuterClass.DeleteInstanceRequest.newBuilder()
                .setInstanceId(instanceId)
                .build());
    }

}
