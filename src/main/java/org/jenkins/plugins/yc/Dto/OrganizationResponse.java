package org.jenkins.plugins.yc.Dto;

import lombok.Data;

import java.util.List;

@Data
public class OrganizationResponse {
    List<Organization> organizations;

    @Data
    public static class Organization{
        String id;
        String createdAt;
        String name;
        String title;
    }
}
