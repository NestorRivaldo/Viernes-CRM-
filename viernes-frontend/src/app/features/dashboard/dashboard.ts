import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { DashboardService } from './dashboard.service';
import { DashboardResumenResponse } from './dashboard.model';
import { AuthService } from '../../core/auth/auth.service';
import { ActividadesService } from '../actividades/actividades.service';
import { ActividadResponse } from '../actividades/actividades.model';
import { OportunidadesService } from '../oportunidades/oportunidades.service';
import { OportunidadResponse, EtapaOportunidad } from '../oportunidades/oportunidades.model';
import { ClientesService } from '../clientes/clientes.service';
import { ClienteResponse } from '../clientes/clientes.model';
import { LeadsService } from '../leads/leads.service';
import { LeadResponse } from '../leads/leads.model';
import { PropuestasService } from '../propuestas/propuestas.service';
import { PropuestaResponse } from '../propuestas/propuestas.model';

interface Tendencia {
  texto: string;
  flecha: string;
  color: string;
}

interface MesPipeline {
  clave: string;
  etiqueta: string;
  valor: number;
  cantidad: number;
}

interface PuntoPipeline extends MesPipeline {
  x: number;
  y: number;
}

export interface EtapaProgreso {
  nombre: string;
  cantidad: number;
  valor: number;
  porcentaje: number;
  colorClass: string;
}

export interface ActividadDashboardItem {
  id: number;
  dia: string;
  mes: string;
  tipo: string;
  icon: string;
  colorClass: string;
  titulo: string;
  cliente: string;
  hora: string;
  tiempo: string;
  detalle: string;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css'
})
export class Dashboard implements OnInit {
  private dashboardService = inject(DashboardService);
  private authService = inject(AuthService);
  private actividadesService = inject(ActividadesService);
  private oportunidadesService = inject(OportunidadesService);
  private clientesService = inject(ClientesService);
  private leadsService = inject(LeadsService);
  private propuestasService = inject(PropuestasService);

  private readonly MESES_VISIBLES = 6;

  resumen = signal<DashboardResumenResponse | null>(null);
  actividades = signal<ActividadResponse[]>([]);
  oportunidades = signal<OportunidadResponse[]>([]);
  clientes = signal<ClienteResponse[]>([]);
  leads = signal<LeadResponse[]>([]);
  propuestas = signal<PropuestaResponse[]>([]);
  oportunidadesListas = signal(false);
  tendenciasListas = signal(false);
  loading = signal(false);
  error = signal('');

  private tendenciasPendientes = 3;

  hasResumen = computed(() => this.resumen() !== null && !this.loading());

  // Greeting & User Name
  saludo = computed(() => {
    const hora = new Date().getHours();
    if (hora < 12) return 'Buenos días';
    if (hora < 19) return 'Buenas tardes';
    return 'Buenas noches';
  });

  nombreUsuario = computed(() => {
    const u = this.authService.getUsuario();
    if (!u) return 'Comercial';
    return u.nombre || u.correo?.split('@')[0] || 'Comercial';
  });

  fechaActualFormateada = computed(() => {
    const hoy = new Date();
    const opciones: Intl.DateTimeFormatOptions = { 
      weekday: 'long', 
      day: 'numeric', 
      month: 'long', 
      year: 'numeric' 
    };
    const str = hoy.toLocaleDateString('es-ES', opciones);
    return str.charAt(0).toUpperCase() + str.slice(1);
  });

  // KPI Metrics direct from real database
  totalClientes = computed(() => this.resumen()?.clientes?.total ?? 0);
  totalLeads = computed(() => this.resumen()?.leads?.total ?? 0);
  totalOportunidades = computed(() => this.resumen()?.oportunidades?.total ?? 0);
  totalPropuestas = computed(() => this.resumen()?.propuestas?.total ?? 0);
  
  tasaConversion = computed(() => {
    const leads = this.totalLeads();
    const op = this.totalOportunidades();
    const total = leads + op;
    if (total === 0) return '0%';
    const pct = ((op / total) * 100).toFixed(1);
    return `${pct}%`;
  });

  oportunidadesGanadas = computed(() =>
    this.oportunidades().filter(o => o.etapa === EtapaOportunidad.GANADA).length
  );

  oportunidadesPerdidas = computed(() =>
    this.oportunidades().filter(o => o.etapa === EtapaOportunidad.PERDIDA).length
  );

  // Tendencias reales: mes actual vs. mes anterior según la fecha de creación.
  tendenciaClientes = computed(() =>
    this.tendenciasListas() ? this.calcularTendencia(this.clientes()) : this.tendenciaPendiente()
  );
  tendenciaLeads = computed(() =>
    this.tendenciasListas() ? this.calcularTendencia(this.leads()) : this.tendenciaPendiente()
  );
  tendenciaOportunidades = computed(() =>
    this.oportunidadesListas() ? this.calcularTendencia(this.oportunidades()) : this.tendenciaPendiente()
  );
  tendenciaPropuestas = computed(() =>
    this.tendenciasListas() ? this.calcularTendencia(this.propuestas()) : this.tendenciaPendiente()
  );

  resumenConversion = computed(() => {
    if (!this.oportunidadesListas()) return '—';
    return `${this.oportunidadesGanadas()} ganadas · ${this.oportunidadesPerdidas()} perdidas`;
  });

  valorPipelineFormateado = computed(() => {
    const val = this.resumen()?.oportunidades?.valorPipeline ?? 0;
    return new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN', maximumFractionDigits: 0 }).format(val);
  });

  // Serie real de los últimos 6 meses a partir de las oportunidades registradas.
  seriePipeline = computed<MesPipeline[]>(() => {
    const hoy = new Date();
    const meses: MesPipeline[] = [];

    for (let i = this.MESES_VISIBLES - 1; i >= 0; i--) {
      const fecha = new Date(hoy.getFullYear(), hoy.getMonth() - i, 1);
      meses.push({
        clave: `${fecha.getFullYear()}-${String(fecha.getMonth() + 1).padStart(2, '0')}`,
        etiqueta: fecha.toLocaleDateString('es-PE', { month: 'short' }).replace('.', ''),
        valor: 0,
        cantidad: 0
      });
    }

    const porClave = new Map(meses.map(m => [m.clave, m]));

    for (const op of this.oportunidades()) {
      if (!op.creadoEn) continue;
      const fecha = new Date(op.creadoEn);
      if (isNaN(fecha.getTime())) continue;

      const clave = `${fecha.getFullYear()}-${String(fecha.getMonth() + 1).padStart(2, '0')}`;
      const mes = porClave.get(clave);
      if (!mes) continue;

      mes.valor += Number(op.valor) || 0;
      mes.cantidad += 1;
    }

    return meses;
  });

  hayDatosPipeline = computed(() => this.seriePipeline().some(m => m.cantidad > 0));

  escalaPipeline = computed(() => {
    const max = Math.max(...this.seriePipeline().map(m => m.valor), 0);
    return max > 0 ? this.redondearEscala(max) : 1;
  });

  etiquetasY = computed(() => {
    const max = this.escalaPipeline();
    return [1, 0.75, 0.5, 0.25, 0].map(f => this.formatearEje(max * f));
  });

  puntosPipeline = computed<PuntoPipeline[]>(() => {
    const serie = this.seriePipeline();
    const max = this.escalaPipeline();
    const xInicio = 30;
    const xFin = 510;
    const yTope = 20;
    const yBase = 170;
    const paso = serie.length > 1 ? (xFin - xInicio) / (serie.length - 1) : 0;

    return serie.map((mes, i) => ({
      ...mes,
      x: xInicio + i * paso,
      y: yBase - (mes.valor / max) * (yBase - yTope)
    }));
  });

  pathLinea = computed(() => this.construirPath(this.puntosPipeline()));

  pathArea = computed(() => {
    const puntos = this.puntosPipeline();
    if (!puntos.length) return '';
    const ultimo = puntos[puntos.length - 1];
    return `${this.construirPath(puntos)} L ${ultimo.x} 178 L ${puntos[0].x} 178 Z`;
  });

  ultimoPunto = computed<PuntoPipeline | null>(() => {
    const puntos = this.puntosPipeline();
    return puntos.length ? puntos[puntos.length - 1] : null;
  });

  // Etapas del Pipeline para desglose real (sin datos inventados)
  etapasDesglose = computed<EtapaProgreso[]>(() => {
    const porEtapa = this.resumen()?.oportunidades?.porEtapa || [];
    const etapasRef: { [key: string]: { label: string; color: string } } = {
      'PROSPECCION': { label: 'Prospección', color: 'blue' },
      'CALIFICACION': { label: 'Calificación', color: 'purple' },
      'PROPUESTA': { label: 'Propuesta', color: 'orange' },
      'NEGOCIACION': { label: 'Negociación', color: 'emerald' },
      'GANADA': { label: 'Cerrada / Ganada', color: 'teal' }
    };

    const cantidades = porEtapa.map((p: any) => Number(p.cantidad) || 0);
    const maxQty = Math.max(...cantidades, 1);

    return Object.keys(etapasRef).map(key => {
      const match = porEtapa.find((p: any) => p.etapa === key);
      const ref = etapasRef[key];
      const cantidad = match?.cantidad ?? 0;
      const valor = match?.valorTotal ?? 0;
      const porcentaje = Math.min(100, Math.round((cantidad / maxQty) * 100));

      return {
        nombre: ref.label,
        cantidad,
        valor,
        porcentaje,
        colorClass: ref.color
      };
    });
  });

  // Próximas actividades reales desde la API
  proximasActividades = computed<ActividadDashboardItem[]>(() => {
    const acts = this.actividades()
      .filter(a => a.estado === 'PENDIENTE' || a.estado === 'EN_PROCESO')
      .slice(0, 5);
    return acts.map(a => this.mapearActividad(a));
  });

  // Actividades recientes ordenadas por fecha
  actividadesRecientes = computed<ActividadDashboardItem[]>(() => {
    const acts = [...this.actividades()]
      .sort((a, b) => new Date(b.fecha || b.creadoEn || 0).getTime() - new Date(a.fecha || a.creadoEn || 0).getTime())
      .slice(0, 5);
    return acts.map(a => this.mapearActividad(a));
  });

  ngOnInit() {
    this.cargarDashboard();
  }

  cargarDashboard() {
    this.loading.set(true);
    this.error.set('');
    this.oportunidadesListas.set(false);
    this.tendenciasListas.set(false);
    this.tendenciasPendientes = 3;

    this.dashboardService.getResumen().subscribe({
      next: (res) => {
        this.resumen.set(res);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No se pudieron cargar las métricas en vivo del backend.');
        this.loading.set(false);
      }
    });

    this.actividadesService.listar().subscribe({
      next: (acts) => {
        this.actividades.set(acts || []);
      },
      error: (err) => {
        console.warn('Aviso: no se pudieron cargar actividades en tiempo real para el dashboard', err);
      }
    });

    this.oportunidadesService.listar().subscribe({
      next: (ops) => {
        this.oportunidades.set(ops || []);
        this.oportunidadesListas.set(true);
      },
      error: (err) => {
        console.warn('Aviso: no se pudieron cargar oportunidades para la evolución del pipeline', err);
        this.oportunidadesListas.set(true);
      }
    });

    this.clientesService.listar().subscribe({
      next: (data) => {
        this.clientes.set(data || []);
        this.marcarTendenciaLista();
      },
      error: (err) => {
        console.warn('Aviso: no se pudieron cargar clientes para las tendencias', err);
        this.marcarTendenciaLista();
      }
    });

    this.leadsService.listar().subscribe({
      next: (data) => {
        this.leads.set(data || []);
        this.marcarTendenciaLista();
      },
      error: (err) => {
        console.warn('Aviso: no se pudieron cargar leads para las tendencias', err);
        this.marcarTendenciaLista();
      }
    });

    this.propuestasService.listar().subscribe({
      next: (data) => {
        this.propuestas.set(data || []);
        this.marcarTendenciaLista();
      },
      error: (err) => {
        console.warn('Aviso: no se pudieron cargar propuestas para las tendencias', err);
        this.marcarTendenciaLista();
      }
    });
  }

  private construirPath(puntos: { x: number; y: number }[]): string {
    if (!puntos.length) return '';
    if (puntos.length === 1) return `M ${puntos[0].x} ${puntos[0].y}`;

    let path = `M ${puntos[0].x} ${puntos[0].y}`;
    for (let i = 0; i < puntos.length - 1; i++) {
      const anterior = puntos[i - 1] ?? puntos[i];
      const actual = puntos[i];
      const siguiente = puntos[i + 1];
      const posterior = puntos[i + 2] ?? siguiente;

      const cp1x = actual.x + (siguiente.x - anterior.x) / 6;
      const cp1y = actual.y + (siguiente.y - anterior.y) / 6;
      const cp2x = siguiente.x - (posterior.x - actual.x) / 6;
      const cp2y = siguiente.y - (posterior.y - actual.y) / 6;

      path += ` C ${cp1x} ${cp1y}, ${cp2x} ${cp2y}, ${siguiente.x} ${siguiente.y}`;
    }
    return path;
  }

  private redondearEscala(valor: number): number {
    if (valor <= 0) return 1;
    const magnitud = Math.pow(10, Math.floor(Math.log10(valor)));
    const normalizado = valor / magnitud;
    const paso = normalizado <= 1 ? 1 : normalizado <= 2 ? 2 : normalizado <= 2.5 ? 2.5 : normalizado <= 5 ? 5 : 10;
    return paso * magnitud;
  }

  private formatearEje(valor: number): string {
    if (valor >= 1000000) {
      const millones = valor / 1000000;
      return `S/ ${Number.isInteger(millones) ? millones : millones.toFixed(1)}M`;
    }
    if (valor >= 1000) {
      const miles = valor / 1000;
      return `S/ ${Number.isInteger(miles) ? miles : miles.toFixed(1)}k`;
    }
    return `S/ ${Math.round(valor)}`;
  }

  private tendenciaPendiente(): Tendencia {
    return { texto: '—', flecha: '', color: '#94a3b8' };
  }

  private marcarTendenciaLista() {
    this.tendenciasPendientes = Math.max(0, this.tendenciasPendientes - 1);
    if (this.tendenciasPendientes === 0) this.tendenciasListas.set(true);
  }

  private calcularTendencia(items: { creadoEn?: string | null }[]): Tendencia {
    const ahora = new Date();
    const mesActual = ahora.getFullYear() * 12 + ahora.getMonth();
    let actual = 0;
    let anterior = 0;

    for (const item of items) {
      if (!item.creadoEn) continue;
      const fecha = new Date(item.creadoEn);
      if (isNaN(fecha.getTime())) continue;
      const mes = fecha.getFullYear() * 12 + fecha.getMonth();
      if (mes === mesActual) actual++;
      else if (mes === mesActual - 1) anterior++;
    }

    if (anterior === 0) {
      if (actual === 0) return { texto: '0%', flecha: '→', color: '#64748b' };
      return { texto: `+${actual}`, flecha: '↑', color: '#16a34a' };
    }

    const pct = Math.round(((actual - anterior) / anterior) * 1000) / 10;
    if (pct > 0) return { texto: `+${pct}%`, flecha: '↑', color: '#16a34a' };
    if (pct < 0) return { texto: `${pct}%`, flecha: '↓', color: '#dc2626' };
    return { texto: '0%', flecha: '→', color: '#64748b' };
  }

  private mapearActividad(act: ActividadResponse): ActividadDashboardItem {
    const fecha = act.fecha ? new Date(act.fecha) : new Date();
    const dia = isNaN(fecha.getDate()) ? '01' : fecha.getDate().toString().padStart(2, '0');
    const mes = isNaN(fecha.getTime()) 
      ? 'ACT' 
      : fecha.toLocaleDateString('es-ES', { month: 'short' }).replace('.', '').toUpperCase();
    const hora = isNaN(fecha.getTime())
      ? '' 
      : fecha.toLocaleTimeString('es-ES', { hour: '2-digit', minute: '2-digit', hour12: true });

    const iconMap: Record<string, string> = {
      'LLAMADA': 'call',
      'REUNION': 'event',
      'TAREA': 'task_alt',
      'SEGUIMIENTO': 'track_changes'
    };
    const colorMap: Record<string, string> = {
      'LLAMADA': 'cyan',
      'REUNION': 'blue',
      'TAREA': 'emerald',
      'SEGUIMIENTO': 'purple'
    };

    const tipoStr = act.tipo?.toString() || 'TAREA';
    return {
      id: act.id,
      dia,
      mes,
      tipo: tipoStr.toLowerCase(),
      icon: iconMap[tipoStr] || 'event',
      colorClass: colorMap[tipoStr] || 'blue',
      titulo: act.titulo,
      cliente: act.descripcion || `Actividad #${act.id}`,
      hora,
      tiempo: isNaN(fecha.getTime()) ? 'Reciente' : fecha.toLocaleDateString('es-ES', { day: 'numeric', month: 'short' }),
      detalle: `Estado: ${act.estado}`
    };
  }

  formatearSoles(valor: number): string {
    return new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN', maximumFractionDigits: 0 }).format(valor);
  }
}
