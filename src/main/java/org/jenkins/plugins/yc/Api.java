package org.jenkins.plugins.yc;

import yandex.cloud.api.compute.v1.InstanceOuterClass;
import yandex.cloud.api.compute.v1.InstanceServiceGrpc;
import yandex.cloud.api.compute.v1.InstanceServiceOuterClass;
import yandex.cloud.api.compute.v1.instancegroup.InstanceGroupOuterClass;
import yandex.cloud.api.compute.v1.instancegroup.InstanceGroupServiceGrpc;
import yandex.cloud.api.compute.v1.instancegroup.InstanceGroupServiceOuterClass;
import yandex.cloud.api.operation.OperationOuterClass;

public class Api {

    public static InstanceGroupOuterClass.InstanceGroup getInstanceGroup(YandexTemplate template) throws Exception {
        var getGroupInstanceRequest = InstanceGroupServiceOuterClass.GetInstanceGroupRequest.newBuilder().setInstanceGroupId(template.parent.groupId).build();
        return template.parent.getServiceFactory().create(InstanceGroupServiceGrpc.InstanceGroupServiceBlockingStub.class, InstanceGroupServiceGrpc::newBlockingStub).get(getGroupInstanceRequest);
    }

    public static OperationOuterClass.Operation createInstanceResponse(YandexTemplate template, InstanceServiceOuterClass.CreateInstanceRequest instanceRequest) throws Exception {
        return template.parent.getServiceFactory().create(InstanceServiceGrpc.InstanceServiceBlockingStub.class, InstanceServiceGrpc::newBlockingStub).create(instanceRequest);
    }

    public static OperationOuterClass.Operation startInstance(YandexTemplate template, String instanceId) throws Exception {
        return template.parent.getServiceFactory().create(InstanceServiceGrpc.InstanceServiceBlockingStub.class, InstanceServiceGrpc::newBlockingStub).start(InstanceServiceOuterClass.StartInstanceRequest.newBuilder()
                .setInstanceId(instanceId)
                .build());
    }


    public static OperationOuterClass.Operation stopInstance(String instanceId, AbstractCloud cloud) throws Exception {
        return cloud.getServiceFactory().create(InstanceServiceGrpc.InstanceServiceBlockingStub.class, InstanceServiceGrpc::newBlockingStub).stop(InstanceServiceOuterClass.StopInstanceRequest.newBuilder()
                .setInstanceId(instanceId)
                .build());
    }

    public static InstanceOuterClass.Instance getInstanceResponse(String instanceId, AbstractCloud cloud) throws Exception {
        return cloud.getServiceFactory().create(InstanceServiceGrpc.InstanceServiceBlockingStub.class, InstanceServiceGrpc::newBlockingStub).get(InstanceServiceOuterClass.GetInstanceRequest.newBuilder()
                .setInstanceId(instanceId)
                .build());
    }

    public static InstanceServiceOuterClass.ListInstancesResponse getFilterInstanceResponse(YandexTemplate template) throws Exception {
        return template.parent.getServiceFactory().create(InstanceServiceGrpc.InstanceServiceBlockingStub.class, InstanceServiceGrpc::newBlockingStub).list(InstanceServiceOuterClass.ListInstancesRequest.newBuilder()
                .setFolderId(template.parent.folderId)
                .setFilter("name=\"".concat(template.parent.name).concat("\""))
                .build());
    }


    public static OperationOuterClass.Operation deleteInstanceResponse(String instanceId, AbstractCloud cloud) throws Exception {
        return cloud.getServiceFactory().create(InstanceServiceGrpc.InstanceServiceBlockingStub.class, InstanceServiceGrpc::newBlockingStub).delete(InstanceServiceOuterClass.DeleteInstanceRequest.newBuilder()
                .setInstanceId(instanceId)
                .build());
    }

}
