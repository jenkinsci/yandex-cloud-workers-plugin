package org.jenkins.plugins.yc.Dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class ListInstancesResponse {

    private List<Instance> instances;

    private Error error;

    @Data
    public static class Instance{
        private String id;
        private String folderId;
        private String createdAt;
        private String name;
        private String description;
        private String labels;
        private String zoneId;
        private String platformId;
        private Resources resources;
        private String status;
        private String metadata;
        private MetadataOptions metadataOptions;
        private BootDisk bootDisk;
        private List<SecondaryDisk> secondaryDisks;
        private List<LocalDisk> localDisks;
        private List<Filesystem> filesystems;
        private List<NetworkInterface> networkInterfaces;
        private String fqdn;
        private SchedulingPolicy schedulingPolicy;
        private String serviceAccountId;
        private NetworkSettings networkSettings;
        private PlacementPolicy placementPolicy;

        @Data
        public static class Resources{
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
        public static class BootDisk{
            private String mode;
            private String deviceName;
            private boolean autoDelete;
            private String diskId;
        }

        @Data
        public static class SecondaryDisk{
            private String mode;
            private String deviceName;
            private boolean autoDelete;
            private String diskId;
        }

        @Data
        public static class LocalDisk{
            private String size;
            private String deviceName;
        }

        @Data
        public static class Filesystem{
            private String mode;
            private String deviceName;
            private String filesystemId;
        }

        @Data
        public static class NetworkInterface{
            private String index;
            private String macAddress;
            private String subnetId;
            private PrimaryV4Address primaryV4Address;
            private PrimaryV6Address primaryV6Address;
            private List<String> securityGroupIds;

            @Data
            public static class PrimaryV4Address{
                private String address;
                private OneToOneNat oneToOneNat;
                private List<DnsRecord> dnsRecords;

                @Data
                public static class OneToOneNat{
                    private String address;
                    private String ipVersion;
                    private List<DnsRecord> dnsRecords;

                    @Data
                    public static class DnsRecord{
                        private String fqdn;
                        private String dnsZoneId;
                        private String ttl;
                        private boolean ptr;
                    }
                }

                @Data
                public static class DnsRecord{
                    private String fqdn;
                    private String dnsZoneId;
                    private String ttl;
                    private boolean ptr;
                }
            }

            @Data
            public static class PrimaryV6Address{
                private String address;
                private OneToOneNat oneToOneNat;
                private List<DnsRecord> dnsRecords;

                @Data
                public static class DnsRecord{
                    private String fqdn;
                    private String dnsZoneId;
                    private String ttl;
                    private boolean ptr;
                }

                @Data
                public static class OneToOneNat{
                    private String address;
                    private String ipVersion;
                    private List<DnsRecord> dnsRecords;

                    @Data
                    public static class DnsRecord{
                        private String fqdn;
                        private String dnsZoneId;
                        private String ttl;
                        private boolean ptr;
                    }
                }
            }
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
            private List<HostAffinityRule> hostAffinityRules;

            @Data
            public static class HostAffinityRule{
                private String key;
                private String op;
                private List<String> values;
            }
        }

    }


    @Data
    public static class Error{
        private Integer code;
        private String message;
        private List<ListInstancesResponse.Error.Detail> details;

        @Data
        public static class Detail{

            @JsonProperty("@type")
            private String type;
            private String requestId;

            private String locale;
        }
    }

}
