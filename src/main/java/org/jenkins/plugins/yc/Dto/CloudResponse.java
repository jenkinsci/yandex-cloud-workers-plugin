package org.jenkins.plugins.yc.Dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class CloudResponse {
    List<Cloud> clouds;

    @Data
    public static class Cloud{
        String id;
        String createdAt;
        String name;
        String description;
        String organizationId;
        Map<String, String> labels;

    }
}
