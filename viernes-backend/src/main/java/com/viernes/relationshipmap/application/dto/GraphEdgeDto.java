package com.viernes.relationshipmap.application.dto;

import com.viernes.relationshipmap.domain.EdgeType;
import java.util.Map;

public record GraphEdgeDto(
    String id,
    String source,
    String target,
    EdgeType type,
    String label,
    Map<String, Object> data
) {}
