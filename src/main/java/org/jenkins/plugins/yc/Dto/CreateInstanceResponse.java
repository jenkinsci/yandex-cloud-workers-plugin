package org.jenkins.plugins.yc.Dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateInstanceResponse {

    private String id;
    private String description;
    private String createdAt;
    private String createdBy;
    private String modifiedAt;
    private boolean done;
    private Metadata metadata;
    private Error error;

    @Data
    public static class Error{
        private Integer code;
        private String message;
        private List<Detail> details;

        @Data
        public static class Detail{

            @JsonProperty("@type")
            private String type;
            private String requestId;

            private String locale;

            private String message;
        }
    }

    @Data
    public static class Metadata{
        @JsonProperty("@type")
        private String type;
        private String instanceId;
    }

    private String response;

    @Override
    public String toString() {
        return "CreateInstanceResponse{" +
                "id='" + id + '\'' +
                ", description='" + description + '\'' +
                ", createdAt='" + createdAt + '\'' +
                ", createdBy='" + createdBy + '\'' +
                ", modifiedAt='" + modifiedAt + '\'' +
                ", done=" + done +
                ", metadata=" + metadata +
                ", error=" + error +
                ", response='" + response + '\'' +
                '}';
    }
}
