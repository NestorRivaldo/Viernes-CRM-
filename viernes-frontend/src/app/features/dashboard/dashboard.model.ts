export interface DashboardResumenResponse {
  clientes?: { total: number };
  leads?: { total: number; porEstado: any[] };
  oportunidades?: {
    total: number;
    abiertas: number;
    ganadas: number;
    perdidas: number;
    valorPipeline: number;
    valorGanado: number;
    porEtapa: any[];
  };
  actividades?: {
    pendientes: number;
    enProceso: number;
    completadas: number;
    canceladas: number;
    vencidas: number;
    porTipo: any[];
    porEstado: any[];
  };
  propuestas?: {
    total: number;
    porEstado: any[];
    valorTotalAprobadas: number;
  };
}
