package org.jenkins.plugins.yc.Dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class ListGroupInstancesResponse {

    private List<Instance> instances;
    private Error error;

    @Data
    public static class Instance{
        private String id;
        private String status;
        private String instanceId;
        private String fqdn;
        private String name;
        private String statusMessage;
        private String zoneId;
        private List<NetworkInterface> networkInterfaces;
        private String statusChangedAt;

        @Data
        public static class NetworkInterface{
            private String index;
            private String macAddress;
            private String subnetId;
            private PrimaryV4Address primaryV4Address;
            private PrimaryV6Address primaryV6Address;

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
