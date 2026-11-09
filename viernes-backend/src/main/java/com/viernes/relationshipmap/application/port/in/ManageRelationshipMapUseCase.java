package com.viernes.relationshipmap.application.port.in;

import com.viernes.relationshipmap.application.dto.GraphNodeDto;
import com.viernes.relationshipmap.application.dto.RelationshipMapResponse;
import com.viernes.relationshipmap.domain.NodeType;

public interface ManageRelationshipMapUseCase {
    RelationshipMapResponse obtenerMapaCliente(Long clienteId);
    RelationshipMapResponse obtenerMapaOportunidad(Long oportunidadId);
    RelationshipMapResponse expandirNodo(NodeType tipo, Long id);
    GraphNodeDto obtenerDetalleNodo(NodeType tipo, Long id);
}
