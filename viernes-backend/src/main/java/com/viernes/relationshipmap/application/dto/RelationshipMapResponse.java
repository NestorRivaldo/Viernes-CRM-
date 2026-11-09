package com.viernes.relationshipmap.application.dto;

import java.util.List;

public record RelationshipMapResponse(
    String rootNodeId,
    List<GraphNodeDto> nodes,
    List<GraphEdgeDto> edges,
    int totalNodes,
    int totalEdges
) {}
