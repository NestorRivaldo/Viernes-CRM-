import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { OportunidadesService } from '../oportunidades.service';
import { OportunidadResponse, EtapaOportunidad } from '../oportunidades.model';

export interface ColumnaKanban {
  id: EtapaOportunidad;
  titulo: string;
  colorClass: string;
}

@Component({
  selector: 'app-oportunidad-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    RouterLink,
    MatProgressSpinnerModule
  ],
  templateUrl: './oportunidad-list.html',
  styleUrl: './oportunidad-list.css'
})
export class OportunidadList implements OnInit {
  private oportunidadesService = inject(OportunidadesService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  oportunidades = signal<OportunidadResponse[]>([]);
  filtroTexto = signal('');
  vistaActual = signal<'lista' | 'pipeline'>('lista');
  loading = signal(false);
  error = signal('');

  displayedColumns: string[] = ['id', 'nombre', 'clienteNombre', 'valor', 'probabilidad', 'etapa', 'acciones'];

  columnasKanban: ColumnaKanban[] = [
    { id: EtapaOportunidad.PROSPECCION, titulo: 'Prospección', colorClass: 'blue' },
    { id: EtapaOportunidad.CALIFICACION, titulo: 'Calificación', colorClass: 'purple' },
    { id: EtapaOportunidad.PROPUESTA, titulo: 'Propuesta', colorClass: 'orange' },
    { id: EtapaOportunidad.NEGOCIACION, titulo: 'Negociación', colorClass: 'emerald' },
    { id: EtapaOportunidad.GANADA, titulo: 'Ganada', colorClass: 'teal' },
    { id: EtapaOportunidad.PERDIDA, titulo: 'Perdida', colorClass: 'rose' }
  ];

  oportunidadesFiltradas = computed(() => {
    const q = this.filtroTexto().toLowerCase().trim();
    return this.oportunidades().filter(op => {
      return !q || 
        op.nombre?.toLowerCase().includes(q) || 
        op.clienteNombre?.toLowerCase().includes(q) || 
        op.responsable?.toLowerCase().includes(q) ||
        op.etapa?.toLowerCase().includes(q);
    });
  });

  totalPipeline = computed(() => {
    return this.oportunidadesFiltradas().reduce((acc, o) => acc + (o.valor || 0), 0);
  });

  hasOportunidades = computed(() => this.oportunidadesFiltradas().length > 0 && !this.loading());
  isEmpty = computed(() => !this.loading() && this.oportunidadesFiltradas().length === 0 && !this.error());

  ngOnInit() {
    this.route.queryParamMap.subscribe(params => {
      const v = params.get('vista');
      if (v === 'pipeline') {
        this.vistaActual.set('pipeline');
      } else {
        this.vistaActual.set('lista');
      }
    });

    this.cargarOportunidades();
  }

  setVista(v: 'lista' | 'pipeline') {
    this.vistaActual.set(v);
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: v === 'pipeline' ? { vista: 'pipeline' } : {},
      queryParamsHandling: ''
    });
  }

  cargarOportunidades() {
    this.loading.set(true);
    this.error.set('');
    this.oportunidadesService.listar().subscribe({
      next: (data) => {
        this.oportunidades.set(data || []);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Error al cargar oportunidades. Verifique que el backend esté disponible.');
        this.loading.set(false);
      }
    });
  }

  getOportunidadesPorEtapa(etapa: EtapaOportunidad): OportunidadResponse[] {
    return this.oportunidadesFiltradas().filter(o => o.etapa === etapa);
  }

  getTotalEtapa(etapa: EtapaOportunidad): number {
    return this.getOportunidadesPorEtapa(etapa).reduce((acc, o) => acc + (o.valor || 0), 0);
  }

  getEtapaClass(etapa: EtapaOportunidad | string): string {
    switch (etapa) {
      case EtapaOportunidad.PROSPECCION: return 'blue';
      case EtapaOportunidad.CALIFICACION: return 'purple';
      case EtapaOportunidad.PROPUESTA: return 'orange';
      case EtapaOportunidad.NEGOCIACION: return 'emerald';
      case EtapaOportunidad.GANADA: return 'success';
      case EtapaOportunidad.PERDIDA: return 'danger';
      default: return 'neutral';
    }
  }

  formatearSoles(valor: number): string {
    return new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN', maximumFractionDigits: 0 }).format(valor || 0);
  }

  eliminar(id: number) {
    if (confirm('¿Está seguro de eliminar esta oportunidad?')) {
      this.oportunidadesService.eliminar(id).subscribe({
        next: () => this.cargarOportunidades(),
        error: () => alert('Error al eliminar')
      });
    }
  }

  editar(id: number) {
    this.router.navigate(['/oportunidades', id, 'editar']);
  }
}
