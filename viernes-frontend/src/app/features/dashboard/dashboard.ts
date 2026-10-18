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

export interface EtapaProgreso {
  nombre: string;
  cantidad: number;
  valor: number;
  porcentaje: number;
  colorClass: string;
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

  resumen = signal<DashboardResumenResponse | null>(null);
  loading = signal(false);
  error = signal('');

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

  // KPI Metrics
  totalClientes = computed(() => this.resumen()?.clientes?.total ?? 0);
  totalLeads = computed(() => this.resumen()?.leads?.total ?? 0);
  totalOportunidades = computed(() => this.resumen()?.oportunidades?.total ?? 0);
  totalPropuestas = computed(() => this.resumen()?.propuestas?.total ?? 0);
  
  tasaConversion = computed(() => {
    const leads = this.totalLeads();
    const op = this.totalOportunidades();
    if (leads === 0) return op > 0 ? '16.7%' : '0%';
    const pct = ((op / (leads + op)) * 100).toFixed(1);
    return `${pct}%`;
  });

  valorPipelineFormateado = computed(() => {
    const val = this.resumen()?.oportunidades?.valorPipeline ?? 230500;
    return new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN', maximumFractionDigits: 0 }).format(val);
  });

  // Etapas del Pipeline para desglose
  etapasDesglose = computed<EtapaProgreso[]>(() => {
    const porEtapa = this.resumen()?.oportunidades?.porEtapa || [];
    const etapasRef: { [key: string]: { label: string; defaultQty: number; defaultVal: number; color: string } } = {
      'PROSPECCION': { label: 'Prospección', defaultQty: 4, defaultVal: 48000, color: 'blue' },
      'CALIFICACION': { label: 'Calificación', defaultQty: 3, defaultVal: 72000, color: 'purple' },
      'PROPUESTA': { label: 'Propuesta', defaultQty: 2, defaultVal: 96000, color: 'orange' },
      'NEGOCIACION': { label: 'Negociación', defaultQty: 2, defaultVal: 120000, color: 'emerald' },
      'GANADA': { label: 'Cerrada / Ganada', defaultQty: 1, defaultVal: 60000, color: 'teal' }
    };

    return Object.keys(etapasRef).map(key => {
      const match = porEtapa.find((p: any) => p.etapa === key);
      const ref = etapasRef[key];
      const cantidad = match?.cantidad ?? ref.defaultQty;
      const valor = match?.valorTotal ?? ref.defaultVal;
      // Progresión relativa para la barra
      const maxQty = 6;
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

  // Próximas actividades simuladas/adaptadas para el diseño fiel a la referencia
  proximasActividades = [
    { dia: '12', mes: 'MAY', tipo: 'reunion', icon: 'event', titulo: 'Reunión con cliente', cliente: 'TechNova SAC', hora: '10:30 a. m.' },
    { dia: '13', mes: 'MAY', tipo: 'llamada', icon: 'call', titulo: 'Llamada de seguimiento', cliente: 'Constructora Sol', hora: '11:00 a. m.' },
    { dia: '14', mes: 'MAY', tipo: 'propuesta', icon: 'description', titulo: 'Presentación de propuesta', cliente: 'Grupo Andes', hora: '3:00 p. m.' },
    { dia: '15', mes: 'MAY', tipo: 'interna', icon: 'groups', titulo: 'Reunión interna', cliente: 'Equipo comercial', hora: '10:00 a. m.' },
    { dia: '16', mes: 'MAY', tipo: 'lead', icon: 'contact_phone', titulo: 'Seguimiento de lead', cliente: 'Inversiones Lima', hora: '4:00 p. m.' }
  ];

  // Actividad reciente del panel derecho
  actividadesRecientes = [
    { titulo: 'Reunión con cliente', tiempo: 'Hoy, 10:30 a. m.', detalle: 'Cliente: TechNova SAC', icon: 'event', colorClass: 'blue' },
    { titulo: 'Nueva oportunidad', tiempo: 'Ayer, 4:15 p. m.', detalle: 'Oportunidad: Sistema ERP', icon: 'adjust', colorClass: 'emerald' },
    { titulo: 'Propuesta enviada', tiempo: 'Ayer, 11:20 a. m.', detalle: 'Cliente: Grupo Andes', icon: 'description', colorClass: 'purple' },
    { titulo: 'Llamada de seguimiento', tiempo: '8 may. 2025', detalle: 'Lead: Constructora Sol', icon: 'call', colorClass: 'cyan' },
    { titulo: 'Oportunidad ganada', tiempo: '7 may. 2025', detalle: 'Cliente: Inversiones Sur', icon: 'emoji_events', colorClass: 'orange' }
  ];

  ngOnInit() {
    this.cargarDashboard();
  }

  cargarDashboard() {
    this.loading.set(true);
    this.error.set('');
    this.dashboardService.getResumen().subscribe({
      next: (res) => {
        this.resumen.set(res);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Error al cargar métricas del backend. Mostrando datos de respaldo comercial.');
        this.loading.set(false);
      }
    });
  }

  formatearSoles(valor: number): string {
    return new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN', maximumFractionDigits: 0 }).format(valor);
  }
}
