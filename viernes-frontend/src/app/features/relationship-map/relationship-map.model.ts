export type NodeType =
  | 'CLIENTE'
  | 'CONTACTO'
  | 'OPORTUNIDAD'
  | 'ACTIVIDAD'
  | 'PROPUESTA'
  | 'PRODUCTO'
  | 'USUARIO';

export type EdgeType =
  | 'CLIENTE_CONTACTO'
  | 'CLIENTE_OPORTUNIDAD'
  | 'CLIENTE_ACTIVIDAD'
  | 'CLIENTE_PROPUESTA'
  | 'OPORTUNIDAD_CONTACTO'
  | 'OPORTUNIDAD_PRODUCTO'
  | 'OPORTUNIDAD_COLABORADOR'
  | 'OPORTUNIDAD_ACTIVIDAD'
  | 'OPORTUNIDAD_PROPUESTA'
  | 'PROPUESTA_PRODUCTO'
  | 'ACTIVIDAD_PARTICIPANTE';

export interface GraphNodeDto {
  id: string; // Composite ID: TYPE:ID, e.g. CLIENTE:62
  entityId: number;
  type: NodeType;
  label: string;
  sublabel?: string;
  status?: string;
  badge?: string;
  hasMoreConnections: boolean;
  connectionCount: number;
  data: Record<string, any>;
}

export interface GraphEdgeDto {
  id: string;
  source: string;
  target: string;
  type: EdgeType;
  label?: string;
  data: Record<string, any>;
}

export interface RelationshipMapResponse {
  rootNodeId: string;
  nodes: GraphNodeDto[];
  edges: GraphEdgeDto[];
  totalNodes: number;
  totalEdges: number;
}
