import {
  Component,
  ElementRef,
  OnDestroy,
  OnInit,
  ViewChild,
  computed,
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import cytoscape, { Core, EventObject, LayoutOptions, NodeSingular } from 'cytoscape';
import { RelationshipMapService } from './relationship-map.service';
import { GraphEdgeDto, GraphNodeDto, NodeType, RelationshipMapResponse } from './relationship-map.model';
import { ClientesService } from '../clientes/clientes.service';
import { ClienteResponse } from '../clientes/clientes.model';

interface HistoryItem {
  id: string;
  type: NodeType;
  entityId: number;
  label: string;
}

@Component({
  selector: 'app-relationship-map',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
  ],
  templateUrl: './relationship-map.html',
  styleUrl: './relationship-map.css',
})
export class RelationshipMap implements OnInit, OnDestroy {
  @ViewChild('cyContainer', { static: false }) cyContainer!: ElementRef<HTMLDivElement>;

  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private mapService = inject(RelationshipMapService);
  private clientesService = inject(ClientesService);

  private cy: Core | null = null;

  // Estado con Signals
  readonly loading = signal<boolean>(false);
  readonly error = signal<string | null>(null);
  readonly rootNodeId = signal<string | null>(null);
  readonly selectedNode = signal<GraphNodeDto | null>(null);
  readonly expandingNodeId = signal<string | null>(null);

  // Control de expansión y colapso de ramas
  readonly expandedNodeIds = signal<Set<string>>(new Set());
  readonly fullyExpandedNodeIds = signal<Set<string>>(new Set());
  private expansionAddedNodes = new Map<string, Set<string>>();

  // Toast / Feedback dinámico
  readonly toastMensaje = signal<string | null>(null);
  private toastTimeout: any = null;

  // Historial de navegación contextual
  readonly history = signal<HistoryItem[]>([]);

  // Filtros
  readonly filtroTexto = signal<string>('');
  readonly filtroTipo = signal<string>('TODOS');
  readonly nodosCoincidentesCount = signal<number>(0);

  // Nodos y aristas cargados en memoria
  private loadedNodes = new Map<string, GraphNodeDto>();
  private loadedEdges = new Map<string, GraphEdgeDto>();

  // Directorio de clientes para cambio rápido de contexto
  readonly listaClientes = signal<ClienteResponse[]>([]);
  readonly buscandoClientes = signal<boolean>(false);
  readonly menuSelectorClienteAbierto = signal<boolean>(false);

  // Estadísticas del lienzo
  readonly totalNodesCount = signal<number>(0);
  readonly totalEdgesCount = signal<number>(0);

  // Paleta de colores e iconos por tipo de entidad (estilo Apple & Blender Node)
  readonly entityConfig: Record<NodeType, { color: string; bg: string; border: string; icon: string; label: string }> = {
    CLIENTE: { color: '#1d4ed8', bg: '#eff6ff', border: '#3b82f6', icon: 'business', label: 'Cliente' },
    CONTACTO: { color: '#c2410c', bg: '#fff7ed', border: '#f97316', icon: 'person', label: 'Contacto' },
    OPORTUNIDAD: { color: '#6d28d9', bg: '#f5f3ff', border: '#8b5cf6', icon: 'monetization_on', label: 'Oportunidad' },
    ACTIVIDAD: { color: '#047857', bg: '#ecfdf5', border: '#10b981', icon: 'event_note', label: 'Actividad' },
    PROPUESTA: { color: '#be123c', bg: '#fff1f2', border: '#f43f5e', icon: 'description', label: 'Propuesta' },
    PRODUCTO: { color: '#0e7490', bg: '#ecfeff', border: '#06b6d4', icon: 'inventory_2', label: 'Producto' },
    USUARIO: { color: '#4338ca', bg: '#eef2ff', border: '#6366f1', icon: 'account_circle', label: 'Colaborador' },
  };

  ngOnInit(): void {
    this.cargarDirectorioClientes();

    this.route.params.subscribe((params) => {
      const clienteId = params['clienteId'] ? Number(params['clienteId']) : null;
      const oportunidadId = params['oportunidadId'] ? Number(params['oportunidadId']) : null;

      if (clienteId) {
        this.cargarMapaCliente(clienteId);
      } else if (oportunidadId) {
        this.cargarMapaOportunidad(oportunidadId);
      } else {
        // Por defecto, buscar el primer cliente activo disponible
        this.cargarPrimerCliente();
      }
    });
  }

  ngOnDestroy(): void {
    if (this.toastTimeout) {
      clearTimeout(this.toastTimeout);
    }
    if (this.cy) {
      this.cy.destroy();
      this.cy = null;
    }
  }

  // --- Feedback / Notificaciones Toast ---

  mostrarToast(mensaje: string): void {
    if (this.toastTimeout) {
      clearTimeout(this.toastTimeout);
    }
    this.toastMensaje.set(mensaje);
    this.toastTimeout = setTimeout(() => {
      this.toastMensaje.set(null);
      this.toastTimeout = null;
    }, 3200);
  }

  // --- Carga Inicial de Datos ---

  cargarPrimerCliente(): void {
    this.loading.set(true);
    this.clientesService.listar().subscribe({
      next: (clientes) => {
        if (clientes.length > 0) {
          const primero = clientes[0];
          this.router.navigate(['/relaciones/clientes', primero.id], { replaceUrl: true });
        } else {
          this.loading.set(false);
          this.error.set('No hay clientes registrados en el sistema para generar un mapa.');
        }
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set('Error al consultar clientes: ' + (err.message || 'Error de conexión'));
      },
    });
  }

  private cargarDirectorioClientes(): void {
    this.buscandoClientes.set(true);
    this.clientesService.listar().subscribe({
      next: (clientes) => {
        this.listaClientes.set(clientes);
        this.buscandoClientes.set(false);
      },
      error: () => this.buscandoClientes.set(false),
    });
  }

  cargarMapaCliente(clienteId: number): void {
    this.loading.set(true);
    this.error.set(null);
    this.mapService.obtenerMapaCliente(clienteId).subscribe({
      next: (res) => {
        this.procesarRespuestaInicial(res);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set('No se pudo cargar el mapa del cliente: ' + (err.error?.message || err.message));
      },
    });
  }

  cargarMapaOportunidad(oportunidadId: number): void {
    this.loading.set(true);
    this.error.set(null);
    this.mapService.obtenerMapaOportunidad(oportunidadId).subscribe({
      next: (res) => {
        this.procesarRespuestaInicial(res);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set('No se pudo cargar el mapa de la oportunidad: ' + (err.error?.message || err.message));
      },
    });
  }

  private procesarRespuestaInicial(res: RelationshipMapResponse): void {
    this.loadedNodes.clear();
    this.loadedEdges.clear();
    this.expansionAddedNodes.clear();
    this.expandedNodeIds.set(new Set([res.rootNodeId]));
    this.fullyExpandedNodeIds.set(new Set());
    this.filtroTexto.set('');
    this.nodosCoincidentesCount.set(0);

    this.rootNodeId.set(res.rootNodeId);

    res.nodes.forEach((n) => this.loadedNodes.set(n.id, n));
    res.edges.forEach((e) => this.loadedEdges.set(e.id, e));

    const initialChildren = new Set<string>();
    res.nodes.forEach((n) => {
      if (n.id !== res.rootNodeId) {
        initialChildren.add(n.id);
      }
    });
    this.expansionAddedNodes.set(res.rootNodeId, initialChildren);

    const rootNode = this.loadedNodes.get(res.rootNodeId);
    if (rootNode) {
      this.selectedNode.set(rootNode);
      this.actualizarHistorial(rootNode);
    }

    setTimeout(() => {
      this.renderCytoscape();
    }, 50);
  }

  // --- Renderizado y Configuración de Cytoscape.js ---

  private renderCytoscape(): void {
    if (!this.cyContainer) return;

    if (this.cy) {
      this.cy.destroy();
    }

    const elements = this.generarElementosCytoscape();

    this.cy = cytoscape({
      container: this.cyContainer.nativeElement,
      elements: elements,
      style: [
        {
          selector: 'node',
          style: {
            shape: 'round-rectangle',
            width: '190px',
            height: '68px',
            'background-color': 'data(bgColor)',
            'border-width': '2px',
            'border-color': 'data(borderColor)',
            'border-opacity': 0.85,
            label: 'data(displayLabel)',
            'text-valign': 'center',
            'text-halign': 'center',
            'text-wrap': 'wrap',
            'text-max-width': '170px',
            'font-family': '-apple-system, BlinkMacSystemFont, "SF Pro Text", "Segoe UI", Roboto, sans-serif',
            'font-size': '11px',
            'font-weight': 600,
            color: '#1e293b',
            'text-background-opacity': 0,
            'transition-property': 'border-width, border-color, opacity',
            'transition-duration': 0.2,
          },
        },
        {
          selector: 'node:selected',
          style: {
            'border-width': '3.5px',
            'border-color': '#0284c7',
          },
        },
        {
          selector: 'node.root-node',
          style: {
            'border-width': '3px',
            'border-color': '#2563eb',
            'font-size': '12px',
            width: '210px',
            height: '76px',
          },
        },
        {
          selector: 'node.search-match',
          style: {
            'border-width': '4px',
            'border-color': '#0284c7',
            opacity: 1,
          },
        },
        {
          selector: 'node.search-dimmed',
          style: {
            opacity: 0.22,
          },
        },
        {
          selector: 'edge',
          style: {
            width: 2,
            'line-color': 'data(lineColor)',
            'curve-style': 'bezier',
            'target-arrow-shape': 'triangle',
            'target-arrow-color': 'data(arrowColor)',
            'arrow-scale': 0.9,
            label: 'data(label)',
            'font-size': '9px',
            'font-weight': 500,
            color: '#475569',
            'text-rotation': 'autorotate',
            'text-background-color': '#ffffff',
            'text-background-opacity': 0.9,
            'text-background-padding': '3px',
            'text-background-shape': 'roundrectangle',
            'transition-property': 'opacity, line-color, target-arrow-color',
            'transition-duration': 0.2,
          },
        },
        {
          selector: 'edge:selected',
          style: {
            width: 3.5,
            'line-color': '#0284c7',
            'target-arrow-color': '#0284c7',
            color: '#0284c7',
          },
        },
        {
          selector: 'edge.search-dimmed',
          style: {
            opacity: 0.15,
          },
        },
      ] as any,
      layout: this.getLayoutConfig(),
      wheelSensitivity: 0.25,
      minZoom: 0.3,
      maxZoom: 2.5,
    });

    this.totalNodesCount.set(this.cy.nodes().length);
    this.totalEdgesCount.set(this.cy.edges().length);

    // Eventos del Lienzo
    this.cy.on('tap', 'node', (evt: EventObject) => {
      const node = evt.target as NodeSingular;
      const nodeId = node.id();
      const nodeData = this.loadedNodes.get(nodeId);
      if (nodeData) {
        this.selectedNode.set(nodeData);
      }
    });

    this.cy.on('dbltap', 'node', (evt: EventObject) => {
      const node = evt.target as NodeSingular;
      const nodeId = node.id();
      const nodeData = this.loadedNodes.get(nodeId);
      if (nodeData) {
        this.toggleExpandirContraer(nodeData);
      }
    });

    this.cy.on('tap', (evt: EventObject) => {
      if (evt.target === this.cy) {
        // Clic en el fondo del lienzo deselecciona
        const root = this.rootNodeId();
        if (root && this.loadedNodes.has(root)) {
          this.selectedNode.set(this.loadedNodes.get(root)!);
        }
      }
    });
  }

  // --- Temas Visuales Dinámicos por Estado Comercial (Fase 3) ---

  getNodeVisualTheme(node: GraphNodeDto): { color: string; bg: string; border: string; icon: string; label: string } {
    const base = this.entityConfig[node.type];
    const status = (node.status || '').toUpperCase();
    const data = node.data || {};

    if (node.type === 'PROPUESTA') {
      switch (status) {
        case 'APROBADA':
          return { color: '#065f46', bg: '#ecfdf5', border: '#10b981', icon: 'task_alt', label: 'Propuesta Aprobada' };
        case 'RECHAZADA':
          return { color: '#9f1239', bg: '#fff1f2', border: '#f43f5e', icon: 'cancel', label: 'Propuesta Rechazada' };
        case 'CANCELADA':
          return { color: '#475569', bg: '#f1f5f9', border: '#cbd5e1', icon: 'block', label: 'Propuesta Cancelada' };
        case 'ENVIADA':
          return { color: '#92400e', bg: '#fffbeb', border: '#f59e0b', icon: 'send', label: 'Propuesta Enviada' };
        case 'BORRADOR':
        default:
          return { color: '#475569', bg: '#f8fafc', border: '#94a3b8', icon: 'edit_note', label: 'Propuesta Borrador' };
      }
    }

    if (node.type === 'ACTIVIDAD') {
      const tipo = String(data['tipo'] || '').toUpperCase();
      let icon = 'event_note';
      if (tipo === 'LLAMADA') icon = 'phone_in_talk';
      else if (tipo === 'REUNION') icon = 'groups';
      else if (tipo === 'TAREA') icon = 'assignment_turned_in';
      else if (tipo === 'EMAIL') icon = 'mark_email_read';

      const vencida = Boolean(data['vencida']);
      if (vencida) {
        return { color: '#991b1b', bg: '#fef2f2', border: '#ef4444', icon: 'warning', label: 'Actividad Vencida' };
      }

      switch (status) {
        case 'COMPLETADA':
          return { color: '#065f46', bg: '#ecfdf5', border: '#10b981', icon, label: 'Actividad Completada' };
        case 'CANCELADA':
          return { color: '#475569', bg: '#f1f5f9', border: '#cbd5e1', icon: 'block', label: 'Actividad Cancelada' };
        case 'PENDIENTE':
        default:
          return { color: '#047857', bg: '#f0fdf4', border: '#22c55e', icon, label: 'Actividad Pendiente' };
      }
    }

    if (node.type === 'OPORTUNIDAD') {
      switch (status) {
        case 'CERRADA_GANADA':
          return { color: '#065f46', bg: '#ecfdf5', border: '#10b981', icon: 'emoji_events', label: 'Ganada' };
        case 'CERRADA_PERDIDA':
          return { color: '#991b1b', bg: '#fef2f2', border: '#f87171', icon: 'thumb_down', label: 'Perdida' };
        case 'NEGOCIACION':
          return { color: '#92400e', bg: '#fffbeb', border: '#f59e0b', icon: 'handshake', label: 'Negociación' };
        case 'PROPUESTA':
          return { color: '#6d28d9', bg: '#f5f3ff', border: '#8b5cf6', icon: 'request_quote', label: 'En Propuesta' };
        default:
          return base;
      }
    }

    return base;
  }

  getEdgeVisualColor(edge: GraphEdgeDto): { lineColor: string; arrowColor: string } {
    const srcNode = this.loadedNodes.get(edge.source);
    const tgtNode = this.loadedNodes.get(edge.target);

    const relevantNode = tgtNode?.type === 'PROPUESTA' || tgtNode?.type === 'ACTIVIDAD' || tgtNode?.type === 'OPORTUNIDAD'
      ? tgtNode
      : (srcNode?.type === 'PROPUESTA' || srcNode?.type === 'ACTIVIDAD' || srcNode?.type === 'OPORTUNIDAD' ? srcNode : null);

    if (relevantNode) {
      const status = (relevantNode.status || '').toUpperCase();
      if (status === 'APROBADA' || status === 'CERRADA_GANADA' || status === 'COMPLETADA') {
        return { lineColor: '#10b981', arrowColor: '#059669' };
      }
      if (status === 'RECHAZADA' || status === 'CERRADA_PERDIDA' || relevantNode.data?.['vencida']) {
        return { lineColor: '#f43f5e', arrowColor: '#e11d48' };
      }
      if (status === 'ENVIADA' || status === 'NEGOCIACION' || status === 'PENDIENTE') {
        return { lineColor: '#f59e0b', arrowColor: '#d97706' };
      }
    }

    if (edge.type === 'PROPUESTA_PRODUCTO' || edge.type === 'OPORTUNIDAD_PRODUCTO') {
      return { lineColor: '#06b6d4', arrowColor: '#0891b2' };
    }
    if (edge.type === 'OPORTUNIDAD_COLABORADOR' || edge.type === 'ACTIVIDAD_PARTICIPANTE') {
      return { lineColor: '#818cf8', arrowColor: '#6366f1' };
    }

    return { lineColor: '#cbd5e1', arrowColor: '#94a3b8' };
  }

  private generarElementosCytoscape(): cytoscape.ElementDefinition[] {
    const elements: cytoscape.ElementDefinition[] = [];

    const root = this.rootNodeId();
    const filtro = this.filtroTipo();

    this.loadedNodes.forEach((node) => {
      if (filtro !== 'TODOS' && node.type !== filtro && node.id !== root) {
        return; // Filtrar por tipo si no es la raíz
      }

      const theme = this.getNodeVisualTheme(node);
      const isRoot = node.id === root;
      const subtitle = node.sublabel ? `\n${node.sublabel}` : '';
      const badge = node.badge ? ` • [${node.badge}]` : '';

      elements.push({
        data: {
          id: node.id,
          label: node.label,
          displayLabel: `${node.label}${badge}${subtitle}`,
          type: node.type,
          bgColor: theme.bg,
          borderColor: theme.border,
          color: theme.color,
        },
        classes: isRoot ? 'root-node' : '',
      });
    });

    const activeNodeIds = new Set(elements.map((e) => e.data.id));

    this.loadedEdges.forEach((edge) => {
      if (activeNodeIds.has(edge.source) && activeNodeIds.has(edge.target)) {
        const edgeTheme = this.getEdgeVisualColor(edge);
        elements.push({
          data: {
            id: edge.id,
            source: edge.source,
            target: edge.target,
            label: edge.label || '',
            lineColor: edgeTheme.lineColor,
            arrowColor: edgeTheme.arrowColor,
          },
        });
      }
    });

    return elements;
  }

  private getLayoutConfig(): LayoutOptions {
    return {
      name: 'concentric',
      fit: true,
      padding: 50,
      startAngle: (3 / 2) * Math.PI,
      clockwise: true,
      equidistant: false,
      minNodeSpacing: 35,
      concentric: (node: NodeSingular) => {
        return node.id() === this.rootNodeId() ? 10 : 2;
      },
      levelWidth: () => 1,
      animate: true,
      animationDuration: 500,
    } as any;
  }

  // --- Consultas de Estado de Nodos ---

  isNodoExpandido(nodeId: string): boolean {
    return this.expandedNodeIds().has(nodeId);
  }

  isNodoCompletamenteExpandido(nodeId: string): boolean {
    return this.fullyExpandedNodeIds().has(nodeId);
  }

  toggleExpandirContraer(node: GraphNodeDto): void {
    if (this.isNodoExpandido(node.id) && node.id !== this.rootNodeId()) {
      this.contraerNodo(node);
    } else {
      this.expandirNodo(node);
    }
  }

  // --- Acciones de Expansión Progresiva ---

  expandirNodo(node: GraphNodeDto): void {
    if (this.expandingNodeId()) return;

    this.expandingNodeId.set(node.id);

    this.mapService.expandirConexiones(node.type, node.entityId).subscribe({
      next: (res) => {
        const newlyAdded = new Set<string>();
        res.nodes.forEach((n) => {
          if (!this.loadedNodes.has(n.id) && n.id !== node.id) {
            this.loadedNodes.set(n.id, n);
            newlyAdded.add(n.id);
          }
        });

        res.edges.forEach((e) => {
          this.loadedEdges.set(e.id, e);
        });

        this.expansionAddedNodes.set(node.id, newlyAdded);

        this.expandedNodeIds.update((set) => {
          const next = new Set(set);
          next.add(node.id);
          return next;
        });

        if (newlyAdded.size === 0) {
          this.fullyExpandedNodeIds.update((set) => {
            const next = new Set(set);
            next.add(node.id);
            return next;
          });
          this.mostrarToast(`"${node.label}" ya tiene todas sus conexiones visibles.`);
        } else {
          this.mostrarToast(`Se agregaron ${newlyAdded.size} conexiones a "${node.label}".`);
        }

        this.actualizarHistorial(node);

        if (this.cy) {
          const nuevosElementos = this.generarElementosCytoscape();
          this.cy.elements().remove();
          this.cy.add(nuevosElementos);
          this.cy.layout(this.getLayoutConfig()).run();

          this.totalNodesCount.set(this.cy.nodes().length);
          this.totalEdgesCount.set(this.cy.edges().length);

          if (this.filtroTexto()) {
            this.ejecutarFiltroTextoEnLienzo(this.filtroTexto());
          }

          const targetCyNode = this.cy.getElementById(node.id);
          if (targetCyNode) {
            targetCyNode.select();
            this.cy.animate({ center: { eles: targetCyNode }, zoom: 1.1, duration: 400 });
          }
        }

        this.expandingNodeId.set(null);
      },
      error: (err) => {
        this.expandingNodeId.set(null);
        this.error.set('Error al expandir conexiones: ' + (err.error?.message || err.message));
      },
    });
  }

  // --- Contracción / Colapso de Ramas ---

  private obtenerTodosLosDescendientes(nodeId: string, visitados = new Set<string>()): Set<string> {
    const directChildren = this.expansionAddedNodes.get(nodeId);
    if (!directChildren) return visitados;
    for (const childId of directChildren) {
      if (!visitados.has(childId)) {
        visitados.add(childId);
        this.obtenerTodosLosDescendientes(childId, visitados);
      }
    }
    return visitados;
  }

  contraerNodo(node: GraphNodeDto): void {
    const root = this.rootNodeId();
    const descendientes = this.obtenerTodosLosDescendientes(node.id);

    if (descendientes.size === 0) {
      this.expandedNodeIds.update((set) => {
        const next = new Set(set);
        next.delete(node.id);
        return next;
      });
      this.mostrarToast(`No hay nodos dependientes para colapsar en "${node.label}".`);
      return;
    }

    // Eliminar descendientes de memoria (excepto la raíz)
    for (const descId of descendientes) {
      if (descId !== root) {
        this.loadedNodes.delete(descId);
        this.expansionAddedNodes.delete(descId);
        this.expandedNodeIds.update((set) => {
          const next = new Set(set);
          next.delete(descId);
          return next;
        });
        this.fullyExpandedNodeIds.update((set) => {
          const next = new Set(set);
          next.delete(descId);
          return next;
        });
      }
    }

    // Limpiar aristas huérfanas
    const loadedKeys = new Set(this.loadedNodes.keys());
    for (const [edgeId, edge] of Array.from(this.loadedEdges.entries())) {
      if (!loadedKeys.has(edge.source) || !loadedKeys.has(edge.target)) {
        this.loadedEdges.delete(edgeId);
      }
    }

    this.expansionAddedNodes.delete(node.id);
    this.expandedNodeIds.update((set) => {
      const next = new Set(set);
      next.delete(node.id);
      return next;
    });

    if (this.selectedNode() && descendientes.has(this.selectedNode()!.id)) {
      this.selectedNode.set(node);
    }

    if (this.cy) {
      const nuevosElementos = this.generarElementosCytoscape();
      this.cy.elements().remove();
      this.cy.add(nuevosElementos);
      this.cy.layout(this.getLayoutConfig()).run();

      this.totalNodesCount.set(this.cy.nodes().length);
      this.totalEdgesCount.set(this.cy.edges().length);

      if (this.filtroTexto()) {
        this.ejecutarFiltroTextoEnLienzo(this.filtroTexto());
      }

      const targetCyNode = this.cy.getElementById(node.id);
      if (targetCyNode) {
        targetCyNode.select();
        this.cy.animate({ center: { eles: targetCyNode }, zoom: 1.1, duration: 400 });
      }
    }

    this.mostrarToast(`Rama colapsada: se removieron ${descendientes.size} nodos dependientes.`);
  }

  // --- Volver al Registro Principal ---

  volverAlPrincipal(): void {
    const rootId = this.rootNodeId();
    if (!rootId) return;

    const rootNode = this.loadedNodes.get(rootId);
    if (rootNode) {
      this.selectedNode.set(rootNode);
      this.centrarEnNodo(rootId);
      this.mostrarToast(`Vista orientada en el registro principal: "${rootNode.label}"`);
    }
  }

  // --- Controles de Cámara / Lienzo ---

  centrarLienzo(): void {
    if (!this.cy) return;
    this.cy.animate({
      fit: { eles: this.cy.elements(), padding: 40 },
      duration: 350,
    });
  }

  centrarEnNodo(nodeId: string): void {
    if (!this.cy) return;
    const cyNode = this.cy.getElementById(nodeId);
    if (cyNode && cyNode.length > 0) {
      cyNode.select();
      this.cy.animate({
        center: { eles: cyNode },
        zoom: 1.25,
        duration: 400,
      });
    }
  }

  zoomIn(): void {
    if (!this.cy) return;
    this.cy.zoom({
      level: this.cy.zoom() * 1.25,
      renderedPosition: { x: this.cy.width() / 2, y: this.cy.height() / 2 },
    });
  }

  zoomOut(): void {
    if (!this.cy) return;
    this.cy.zoom({
      level: this.cy.zoom() * 0.8,
      renderedPosition: { x: this.cy.width() / 2, y: this.cy.height() / 2 },
    });
  }

  reorganizarLayout(): void {
    if (!this.cy) return;
    this.cy.layout(this.getLayoutConfig()).run();
  }

  // --- Historial y Filtros ---

  private actualizarHistorial(node: GraphNodeDto): void {
    const list = this.history();
    const existe = list.some((item) => item.id === node.id);
    if (!existe) {
      this.history.set([
        ...list,
        { id: node.id, type: node.type, entityId: node.entityId, label: node.label },
      ]);
    }
  }

  seleccionarDesdeHistorial(item: HistoryItem): void {
    const node = this.loadedNodes.get(item.id);
    if (node) {
      this.selectedNode.set(node);
      this.centrarEnNodo(item.id);
    } else {
      if (item.type === 'CLIENTE') {
        this.cargarMapaCliente(item.entityId);
      } else if (item.type === 'OPORTUNIDAD') {
        this.cargarMapaOportunidad(item.entityId);
      }
    }
  }

  aplicarFiltroTipo(tipo: string): void {
    this.filtroTipo.set(tipo);
    if (this.cy) {
      const elementos = this.generarElementosCytoscape();
      this.cy.elements().remove();
      this.cy.add(elementos);
      this.cy.layout(this.getLayoutConfig()).run();
      this.totalNodesCount.set(this.cy.nodes().length);
      this.totalEdgesCount.set(this.cy.edges().length);

      if (this.filtroTexto()) {
        this.ejecutarFiltroTextoEnLienzo(this.filtroTexto());
      }
    }
  }

  aplicarFiltroTexto(query: string): void {
    this.filtroTexto.set(query);
    this.ejecutarFiltroTextoEnLienzo(query);
  }

  private ejecutarFiltroTextoEnLienzo(query: string): void {
    if (!this.cy) return;

    const trimmed = query.trim().toLowerCase();
    if (!trimmed) {
      this.cy.nodes().removeClass('search-match search-dimmed');
      this.cy.edges().removeClass('search-dimmed');
      this.nodosCoincidentesCount.set(0);
      return;
    }

    let matchCount = 0;
    let firstMatch: NodeSingular | null = null;

    this.cy.nodes().forEach((cyNode) => {
      const nodeId = cyNode.id();
      const data = this.loadedNodes.get(nodeId);
      let match = false;

      if (data) {
        if (data.label && data.label.toLowerCase().includes(trimmed)) match = true;
        if (data.sublabel && data.sublabel.toLowerCase().includes(trimmed)) match = true;
        if (data.badge && data.badge.toLowerCase().includes(trimmed)) match = true;
        if (data.type && data.type.toLowerCase().includes(trimmed)) match = true;
        if (data.data) {
          for (const val of Object.values(data.data)) {
            if (val !== null && val !== undefined && String(val).toLowerCase().includes(trimmed)) {
              match = true;
              break;
            }
          }
        }
      }

      if (match) {
        cyNode.addClass('search-match');
        cyNode.removeClass('search-dimmed');
        matchCount++;
        if (!firstMatch) firstMatch = cyNode;
      } else {
        cyNode.removeClass('search-match');
        cyNode.addClass('search-dimmed');
      }
    });

    // Dim edges connecting to dimmed nodes
    this.cy.edges().forEach((cyEdge) => {
      const src = cyEdge.source();
      const tgt = cyEdge.target();
      if (src.hasClass('search-dimmed') || tgt.hasClass('search-dimmed')) {
        cyEdge.addClass('search-dimmed');
      } else {
        cyEdge.removeClass('search-dimmed');
      }
    });

    this.nodosCoincidentesCount.set(matchCount);

    if (firstMatch) {
      this.cy.animate({
        center: { eles: firstMatch },
        zoom: 1.15,
        duration: 350,
      });
    }
  }

  // --- Navegación a Fichas de Origen ---

  navegarARegistroOriginal(node: GraphNodeDto): void {
    switch (node.type) {
      case 'CLIENTE':
        this.router.navigate(['/clientes', node.entityId, 'editar']);
        break;
      case 'OPORTUNIDAD':
        this.router.navigate(['/oportunidades', node.entityId]);
        break;
      case 'ACTIVIDAD':
        this.router.navigate(['/actividades'], { queryParams: { q: node.label } });
        break;
      case 'PROPUESTA':
        this.router.navigate(['/propuestas'], { queryParams: { q: node.label } });
        break;
      case 'PRODUCTO':
        this.router.navigate(['/productos'], { queryParams: { q: node.label } });
        break;
      case 'USUARIO':
        this.router.navigate(['/usuarios'], { queryParams: { q: node.label } });
        break;
      default:
        break;
    }
  }

  cambiarContextoCliente(cliente: ClienteResponse): void {
    this.menuSelectorClienteAbierto.set(false);
    this.router.navigate(['/relaciones/clientes', cliente.id]);
  }
}
