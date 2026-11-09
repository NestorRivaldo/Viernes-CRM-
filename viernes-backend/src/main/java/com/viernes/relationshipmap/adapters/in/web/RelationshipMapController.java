package com.viernes.relationshipmap.adapters.in.web;

import com.viernes.relationshipmap.application.dto.GraphNodeDto;
import com.viernes.relationshipmap.application.dto.RelationshipMapResponse;
import com.viernes.relationshipmap.application.port.in.ManageRelationshipMapUseCase;
import com.viernes.relationshipmap.domain.NodeType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/relationship-map")
@RequiredArgsConstructor
@Tag(name = "Relationship Map", description = "Endpoints para el mapa contextual e interactivo de relaciones comerciales")
public class RelationshipMapController {

    private final ManageRelationshipMapUseCase mapUseCase;

    @GetMapping("/clientes/{id}")
    @Operation(summary = "Obtener mapa contextual de un cliente", description = "Devuelve el grafo inicial centrado en un cliente con sus contactos, oportunidades, actividades y propuestas")
    public ResponseEntity<RelationshipMapResponse> obtenerMapaCliente(@PathVariable Long id) {
        return ResponseEntity.ok(mapUseCase.obtenerMapaCliente(id));
    }

    @GetMapping("/oportunidades/{id}")
    @Operation(summary = "Obtener mapa contextual de una oportunidad", description = "Devuelve el grafo inicial centrado en una oportunidad con cliente, contactos, productos, colaboradores, actividades y propuestas")
    public ResponseEntity<RelationshipMapResponse> obtenerMapaOportunidad(@PathVariable Long id) {
        return ResponseEntity.ok(mapUseCase.obtenerMapaOportunidad(id));
    }

    @GetMapping("/nodes/{type}/{id}/connections")
    @Operation(summary = "Expandir conexiones de un nodo", description = "Devuelve las conexiones asociadas a un nodo específico para su expansión progresiva en el lienzo")
    public ResponseEntity<RelationshipMapResponse> expandirConexiones(
            @PathVariable String type,
            @PathVariable Long id) {
        NodeType nodeType = NodeType.valueOf(type.toUpperCase());
        return ResponseEntity.ok(mapUseCase.expandirNodo(nodeType, id));
    }

    @GetMapping("/nodes/{type}/{id}")
    @Operation(summary = "Obtener detalle de un nodo", description = "Devuelve los metadatos completos de un nodo para inspección en el panel lateral")
    public ResponseEntity<GraphNodeDto> obtenerDetalleNodo(
            @PathVariable String type,
            @PathVariable Long id) {
        NodeType nodeType = NodeType.valueOf(type.toUpperCase());
        return ResponseEntity.ok(mapUseCase.obtenerDetalleNodo(nodeType, id));
    }
}
