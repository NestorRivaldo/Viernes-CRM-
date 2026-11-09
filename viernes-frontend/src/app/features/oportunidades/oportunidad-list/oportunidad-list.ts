import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatMenuModule } from '@angular/material/menu';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { DragDropModule, CdkDragDrop } from '@angular/cdk/drag-drop';
import { OportunidadesService } from '../oportunidades.service';
import { OportunidadResponse, EtapaOportunidad } from '../oportunidades.model';
import {
  OportunidadDialog,
  OportunidadDialogResult,
} from '../oportunidad-dialog/oportunidad-dialog';
import { sortRows } from '../../../shared/table-sort';

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
    MatSortModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatDialogModule,
    DragDropModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './oportunidad-list.html',
  styleUrl: './oportunidad-list.css'
})
export class OportunidadList implements OnInit {
  private oportunidadesService = inject(OportunidadesService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private dialog = inject(MatDialog);

  oportunidades = signal<OportunidadResponse[]>([]);
  filtroTexto = signal('');
  vistaActual = signal<'lista' | 'pipeline'>('lista');
  loading = signal(false);
  error = signal('');

  displayedColumns: string[] = ['id', 'nombre', 'clienteNombre', 'valor', 'probabilidad', 'etapa', 'acciones'];

  sortState = signal<Sort>({ active: 'id', direction: 'asc' });

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

  oportunidadesOrdenadas = computed(() => sortRows(this.oportunidadesFiltradas(), this.sortState()));


  onSortChange(sort: Sort) {

    this.sortState.set(sort);

  }


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

  readonly etapasOrdenadas: EtapaOportunidad[] = [
    EtapaOportunidad.PROSPECCION,
    EtapaOportunidad.CALIFICACION,
    EtapaOportunidad.PROPUESTA,
    EtapaOportunidad.NEGOCIACION,
    EtapaOportunidad.GANADA,
    EtapaOportunidad.PERDIDA
  ];

  esPrimeraEtapa(etapa: EtapaOportunidad | string): boolean {
    return this.etapasOrdenadas.indexOf(etapa as EtapaOportunidad) <= 0;
  }

  esUltimaEtapa(etapa: EtapaOportunidad | string): boolean {
    const idx = this.etapasOrdenadas.indexOf(etapa as EtapaOportunidad);
    return idx < 0 || idx >= this.etapasOrdenadas.length - 1;
  }

  moverEtapaAnterior(op: OportunidadResponse, event?: Event) {
    if (event) event.stopPropagation();
    const idx = this.etapasOrdenadas.indexOf(op.etapa as EtapaOportunidad);
    if (idx > 0) {
      this.moverEtapa(op, this.etapasOrdenadas[idx - 1]);
    }
  }

  moverEtapaSiguiente(op: OportunidadResponse, event?: Event) {
    if (event) event.stopPropagation();
    const idx = this.etapasOrdenadas.indexOf(op.etapa as EtapaOportunidad);
    if (idx >= 0 && idx < this.etapasOrdenadas.length - 1) {
      this.moverEtapa(op, this.etapasOrdenadas[idx + 1]);
    }
  }

  moverEtapa(op: OportunidadResponse, nuevaEtapa: EtapaOportunidad, event?: Event) {
    if (event) event.stopPropagation();
    if (op.etapa === nuevaEtapa) return;

    const etapaPrevia = op.etapa;
    // Actualización optimista local
    this.oportunidades.update(ops => 
      ops.map(o => o.id === op.id ? { ...o, etapa: nuevaEtapa } : o)
    );

    this.oportunidadesService.cambiarEtapa(op.id, nuevaEtapa).subscribe({
      next: (actualizada) => {
        this.oportunidades.update(ops => 
          ops.map(o => o.id === op.id ? { ...o, etapa: actualizada.etapa } : o)
        );
      },
      error: (err) => {
        console.error('Error al cambiar etapa:', err);
        // Revertir en caso de falla
        this.oportunidades.update(ops => 
          ops.map(o => o.id === op.id ? { ...o, etapa: etapaPrevia } : o)
        );
        alert('No se pudo actualizar la etapa de la oportunidad en el servidor.');
      }
    });
  }

  onCardDrop(event: CdkDragDrop<EtapaOportunidad>) {
    if (event.previousContainer === event.container) {
      return;
    }
    const op = event.item.data as OportunidadResponse;
    const nuevaEtapa = event.container.data;
    if (op && nuevaEtapa) {
      this.moverEtapa(op, nuevaEtapa);
    }
  }

  editar(id: number) {
    this.abrirDialogo(id);
  }

  nuevaOportunidad() {
    this.abrirDialogo();
  }

  private abrirDialogo(id?: number) {
    this.dialog
      .open(OportunidadDialog, {
        data: { id: id ?? null },
        panelClass: 'opp-dialog-panel',
        backdropClass: 'opp-dialog-backdrop',
        width: '780px',
        maxWidth: '95vw',
        disableClose: false,
        autoFocus: false,
      })
      .afterClosed()
      .subscribe((result: OportunidadDialogResult | undefined) => {
        if (result?.saved) {
          this.cargarOportunidades();
        }
      });
  }

  verDetalle(id: number) {
    this.router.navigate(['/oportunidades', id]);
  }
}
