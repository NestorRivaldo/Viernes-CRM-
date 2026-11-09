package com.viernes.relationshipmap.application.dto;

import com.viernes.relationshipmap.domain.NodeType;
import java.util.Map;

public record GraphNodeDto(
    String id,
    Long entityId,
    NodeType type,
    String label,
    String sublabel,
    String status,
    String badge,
    boolean hasMoreConnections,
    int connectionCount,
    Map<String, Object> data
) {}
