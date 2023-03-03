package org.jenkins.plugins.yc.Dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class InstanceGroupResponse {

    private String id;
    private String folderId;
    private String createdAt;
    private String name;
    private String description;
    private Map<String, String> labels;
    private InstanceTemplate instanceTemplate;
    private ScalePolicy scalePolicy;
    private DeployPolicy deployPolicy;
    private AllocationPolicy allocationPolicy;
    private LoadBalancerState loadBalancerState;
    private ManagedInstancesState managedInstancesState;
    private LoadBalancerSpec loadBalancerSpec;
    private HealthChecksSpec healthChecksSpec;
    private String serviceAccountId;
    private String status;
    private ArrayList<Variable> variables;
    private String deletionProtection;
    private ApplicationLoadBalancerSpec applicationLoadBalancerSpec;
    private ApplicationLoadBalancerState applicationLoadBalancerState;

    private Error error;

    @Data
    public static class InstanceTemplate{
        private String description;
        private Map<String, String> labels;
        private String platformId;
        private ResourcesSpec resourcesSpec;
        private Map<String, String> metadata;
        private BootDiskSpec bootDiskSpec;
        private ArrayList<SecondaryDiskSpec> secondaryDiskSpecs;
        private ArrayList<NetworkInterfaceSpec> networkInterfaceSpecs;
        private SchedulingPolicy schedulingPolicy;
        private String serviceAccountId;
        private NetworkSettings networkSettings;
        private String name;
        private String hostname;
        private PlacementPolicy placementPolicy;
        private ArrayList<FilesystemSpec> filesystemSpecs;

        @Data
        public static class ResourcesSpec{
            private String memory;
            private String cores;
            private String coreFraction;
            private String gpus;
        }

        @Data
        public static class BootDiskSpec{
            private String mode;
            private String deviceName;
            private DiskSpec diskSpec;
            private String diskId;

            @Data
            public static class DiskSpec{
                private String description;
                private String typeId;
                private String size;
                private String preserveAfterInstanceDelete;
                private String imageId;
                private String snapshotId;
            }
        }

        @Data
        public static class SecondaryDiskSpec{
            private String mode;
            private String deviceName;
            private DiskSpec diskSpec;
            private String diskId;

            @Data
            public static class DiskSpec{
                private String description;
                private String typeId;
                private String size;
                private String preserveAfterInstanceDelete;
                private String imageId;
                private String snapshotId;
            }
        }

        @Data
        public static class NetworkInterfaceSpec{
            private String networkId;
            private ArrayList<String> subnetIds;
            private PrimaryV4AddressSpec primaryV4AddressSpec;
            private PrimaryV6AddressSpec primaryV6AddressSpec;
            private ArrayList<String> securityGroupIds;

            @Data
            public static class PrimaryV6AddressSpec{
                private OneToOneNatSpec oneToOneNatSpec;
                private ArrayList<DnsRecordSpec> dnsRecordSpecs;
                private String address;

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
                        private String ptr;
                    }
                }

                @Data
                public static class DnsRecordSpec{
                    private String fqdn;
                    private String dnsZoneId;
                    private String ttl;
                    private String ptr;
                }
            }

            @Data
            public static class PrimaryV4AddressSpec{
                private OneToOneNatSpec oneToOneNatSpec;
                private ArrayList<DnsRecordSpec> dnsRecordSpecs;
                private String address;

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
                        private String ptr;
                    }
                }

                @Data
                public static class DnsRecordSpec{
                    private String fqdn;
                    private String dnsZoneId;
                    private String ttl;
                    private String ptr;
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
            private ArrayList<HostAffinityRule> hostAffinityRules;

            @Data
            public static class HostAffinityRule{
                private String key;
                private String op;
                private ArrayList<String> values;
            }
        }

        @Data
        public static class FilesystemSpec{
            private String mode;
            private String deviceName;
            private String filesystemId;
        }
    }

    @Data
    public static class ScalePolicy{
        private TestAutoScale testAutoScale;
        private FixedScale fixedScale;
        private AutoScale autoScale;

        @Data
        public static class AutoScale{
            private String minZoneSize;
            private String maxSize;
            private String measurementDuration;
            private String warmupDuration;
            private String stabilizationDuration;
            private String initialSize;
            private CpuUtilizationRule cpuUtilizationRule;
            private ArrayList<CustomRule> customRules;
            private String autoScaleType;

            @Data
            public static class CpuUtilizationRule{
                private String utilizationTarget;
            }

            @Data
            public static class CustomRule{
                private String ruleType;
                private String metricType;
                private String metricName;
                private Map<String, String> labels;
                private String target;
                private String folderId;
                private String service;
            }
        }

        @Data
        public static class TestAutoScale{
            private String minZoneSize;
            private String maxSize;
            private String measurementDuration;
            private String warmupDuration;
            private String stabilizationDuration;
            private String initialSize;
            private CpuUtilizationRule cpuUtilizationRule;
            private ArrayList<CustomRule> customRules;
            private String autoScaleType;

            @Data
            public static class CpuUtilizationRule{
                private String utilizationTarget;
            }

            @Data
            public static class CustomRule{
                private String ruleType;
                private String metricType;
                private String metricName;
                private Map<String, String> labels;
                private String target;
                private String folderId;
                private String service;
            }
        }

        @Data
        public static class FixedScale{
            private String size;
        }
    }

    @Data
    public static class DeployPolicy{
        private String maxUnavailable;
        private String maxDeleting;
        private String maxCreating;
        private String maxExpansion;
        private String startupDuration;
        private String strategy;
    }

    @Data
    public static class AllocationPolicy{
        private ArrayList<Zone> zones;

        @Data
        public static class Zone{
            private String zoneId;
        }
    }

    @Data
    public static class LoadBalancerState{
        private String targetGroupId;
        private String statusMessage;
    }

    @Data
    public static class ManagedInstancesState{
        private String targetSize;
        private String runningActualCount;
        private String runningOutdatedCount;
        private String processingCount;
    }

    @Data
    public static class LoadBalancerSpec{
        private TargetGroupSpec targetGroupSpec;
        private String maxOpeningTrafficDuration;

        @Data
        public static class TargetGroupSpec{
            private String name;
            private String description;
            private Map<String, String> labels;
        }
    }

    @Data
    public static class HealthChecksSpec{
        private ArrayList<HealthCheckSpec> healthCheckSpecs;
        private String maxCheckingHealthDuration;

        @Data
        public static class HealthCheckSpec{
            private String interval;
            private String timeout;
            private String unhealthyThreshold;
            private String healthyThreshold;
            private TcpOptions tcpOptions;
            private HttpOptions httpOptions;

            @Data
            public static class TcpOptions{
                private String port;
            }

            @Data
            public static class HttpOptions{
                private String port;
                private String path;
            }
        }
    }

    @Data
    public static class Variable{
        private String key;
        private String value;
    }

    @Data
    public static class ApplicationLoadBalancerSpec{
        private TargetGroupSpec targetGroupSpec;
        private String maxOpeningTrafficDuration;
        @Data
        public static class TargetGroupSpec{
            private String name;
            private String description;
            private Map<String, String> labels;
        }
    }

    @Data
    public static class ApplicationLoadBalancerState{
        private String targetGroupId;
        private String statusMessage;
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
