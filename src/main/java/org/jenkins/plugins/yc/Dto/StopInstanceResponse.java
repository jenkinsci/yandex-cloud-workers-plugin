package org.jenkins.plugins.yc.Dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class StopInstanceResponse {

    private String id;
    private String description;
    private String createdAt;
    private String createdBy;
    private String modifiedAt;
    private boolean done;
    private String metadata;
    private Error error;
    private String response;

    @Data
    public static class Error{
        private Integer code;
        private String message;
        private List<StopInstanceResponse.Error.Detail> details;

        @Data
        public static class Detail{

            @JsonProperty("@type")
            private String type;
            private String requestId;

            private String locale;
        }
    }

}
