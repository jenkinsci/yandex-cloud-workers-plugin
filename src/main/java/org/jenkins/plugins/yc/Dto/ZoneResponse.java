package org.jenkins.plugins.yc.Dto;

import lombok.Data;

import java.util.List;

@Data
public class ZoneResponse {
    List<Zone> zones;

    @Data
    public static class Zone{
        String id;
        String regionId;
        String status;
    }
}
