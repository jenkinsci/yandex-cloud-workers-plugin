package org.jenkins.plugins.yc.Dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class FolderResponse {
    List<Folder> folders;

    @Data
    public static class Folder{
        String id;
        String cloudId;
        String createdAt;
        String name;
        String description;
        Map<String, String> labels;
        String status;
    }
}
