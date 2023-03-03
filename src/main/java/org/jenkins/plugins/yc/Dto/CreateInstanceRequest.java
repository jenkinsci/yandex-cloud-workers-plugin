package org.jenkins.plugins.yc.Dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.Map;

@Data
public class CreateInstanceRequest {

    private String folderId;
    private String name;
    private String description;
    private Map<String, String> labels;
    private String zoneId;
    private String platformId;
    private ResourcesSpec resourcesSpec;
    private Metadata metadata;
    private MetadataOptions metadataOptions;
    private BootDiskSpec bootDiskSpec;
    private ArrayList<SecondaryDiskSpec> secondaryDiskSpecs;
    private ArrayList<LocalDiskSpec> localDiskSpecs;
    private ArrayList<FilesystemSpec> filesystemSpecs;
    private ArrayList<NetworkInterfaceSpec> networkInterfaceSpecs;
    private String hostname;
    private SchedulingPolicy schedulingPolicy;
    private String serviceAccountId;
    private NetworkSettings networkSettings;
    private PlacementPolicy placementPolicy;

    @Data
    public static class ResourcesSpec{
        private String memory;
        private String cores;
        private String coreFraction;
        private String gpus;
    }

    @Data
    public static class MetadataOptions{
        private String gceHttpEndpoint;
        private String awsV1HttpEndpoint;
        private String gceHttpToken;
        private String awsV1HttpToken;
    }

    @Data
    public static class BootDiskSpec{
        private String mode;
        private String deviceName;
        private boolean autoDelete;
        private DiskSpec diskSpec;
        private String diskId;

        @Data
        public static class DiskSpec{
            private String name;
            private String description;
            private String typeId;
            private String size;
            private String blockSize;
            private DiskPlacementPolicy diskPlacementPolicy;
            private String imageId;
            private String snapshotId;

            @Data
            public static class DiskPlacementPolicy{
                private String placementGroupId;
            }
        }

    }

    @Data
    public static class SecondaryDiskSpec{
        private String mode;
        private String deviceName;
        private boolean autoDelete;
        private DiskSpec diskSpec;
        private String diskId;

        @Data
        public static class DiskSpec{
            private String name;
            private String description;
            private String typeId;
            private String size;
            private String blockSize;
            private BootDiskSpec.DiskSpec.DiskPlacementPolicy diskPlacementPolicy;
            private String imageId;
            private String snapshotId;

            @Data
            public static class DiskPlacementPolicy{
                private String placementGroupId;
            }
        }
    }

    @Data
    public static class LocalDiskSpec{
        private String size;
    }

    @Data
    public static class FilesystemSpec{
        private String mode;
        private String deviceName;
        private String filesystemId;
    }

    @Data
    public static class NetworkInterfaceSpec{
        private String subnetId;
        private PrimaryV4AddressSpec primaryV4AddressSpec;
        private PrimaryV6AddressSpec primaryV6AddressSpec;
        private ArrayList<String> securityGroupIds;

        @Data
        public static class PrimaryV4AddressSpec{
            private String address;
            private OneToOneNatSpec oneToOneNatSpec;
            private ArrayList<DnsRecordSpec> dnsRecordSpecs;

            @Data
            public static class OneToOneNatSpec{
                private String ipVersion;
                private String address;
                private ArrayList<DnsRecordSpec> dnsRecordSpecs;

                @Data
                public static class DnsRecordSpec{
                    private String fqdn;
                    private String dnsZoneId;
                    private String ttl;
                    private boolean ptr;
                }
            }

            @Data
            public static class DnsRecordSpec{
                private String fqdn;
                private String dnsZoneId;
                private String ttl;
                private boolean ptr;
            }
        }

        @Data
        public static class PrimaryV6AddressSpec{
            private String address;
            private OneToOneNatSpec oneToOneNatSpec;
            private ArrayList<DnsRecordSpec> dnsRecordSpecs;

            @Data
            public static class DnsRecordSpec{
                private String fqdn;
                private String dnsZoneId;
                private String ttl;
                private boolean ptr;
            }

            @Data
            public static class OneToOneNatSpec{
                private String ipVersion;
                private String address;
                private ArrayList<DnsRecordSpec> dnsRecordSpecs;

                @Data
                public static class DnsRecordSpec{
                    private String fqdn;
                    private String dnsZoneId;
                    private String ttl;
                    private boolean ptr;
                }
            }
        }
    }

    @Data
    public static class Metadata{
        @JsonProperty("user-data")
        private String userData;
    }

    @Data
    public static class SchedulingPolicy{
        private boolean preemptible;
    }

    @Data
    public static class NetworkSettings{
        private String type;
    }

    @Data
    public static class PlacementPolicy{
        private String placementGroupId;
        private ArrayList<HostAffinityRule> hostAffinityRules;

        @Data
        public static class HostAffinityRule{
            private String key;
            private String op;
            private ArrayList<String> values;
        }
    }

}
