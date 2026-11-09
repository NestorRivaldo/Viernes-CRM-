import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTableModule } from '@angular/material/table';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ActividadesService } from '../actividades.service';
import { ActividadResponse, EstadoActividad, TipoActividad } from '../actividades.model';
import { ActividadDialogResult, ActividadForm } from '../actividad-form/actividad-form';
import { AuthService } from '../../../core/auth/auth.service';
import { EtiquetaResponse } from '../../etiquetas/etiquetas.model';
import { EtiquetasService } from '../../etiquetas/etiquetas.service';
import { EtiquetaManagerDialog } from '../../etiquetas/etiqueta-manager-dialog/etiqueta-manager-dialog';
import { sortRows } from '../../../shared/table-sort';

@Component({
  selector: 'app-actividad-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatTableModule,
    MatSortModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './actividad-list.html',
  styleUrl: './actividad-list.css'
})
export class ActividadList implements OnInit {
  private actividadesService = inject(ActividadesService);
  private etiquetasService = inject(EtiquetasService);
  private authService = inject(AuthService);
  private dialog = inject(MatDialog);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  actividades = signal<ActividadResponse[]>([]);
  etiquetas = signal<EtiquetaResponse[]>([]);
  filtroTexto = signal('');
  filtroEtiquetaId = signal<number | null>(null);
  loading = signal(false);
  error = signal('');

  displayedColumns: string[] = ['id', 'titulo', 'tipo', 'estado', 'fecha', 'acciones'];

  sortState = signal<Sort>({ active: 'id', direction: 'desc' });

  actividadesFiltradas = computed(() => {
    const q = this.filtroTexto().toLowerCase().trim();
    const idEtiqueta = this.filtroEtiquetaId();
    return this.actividades().filter(a => {
      const coincideEtiqueta = !idEtiqueta ||
        (a.etiquetas || []).some(etiqueta => etiqueta.id === idEtiqueta);
      if (!coincideEtiqueta) return false;

      return !q ||
        a.titulo?.toLowerCase().includes(q) ||
        a.tipo?.toLowerCase().includes(q) ||
        a.estado?.toLowerCase().includes(q) ||
        a.descripcion?.toLowerCase().includes(q) ||
        (a.etiquetas || []).some(etiqueta => etiqueta.nombre.toLowerCase().includes(q));
    });
  });

  actividadesOrdenadas = computed(() => sortRows(this.actividadesFiltradas(), this.sortState()));


  onSortChange(sort: Sort) {

    this.sortState.set(sort);

  }


  hasActividades = computed(() => this.actividadesFiltradas().length > 0 && !this.loading());
  isEmpty = computed(() => !this.loading() && this.actividadesFiltradas().length === 0 && !this.error());

  ngOnInit() {
    this.cargarEtiquetas();
    this.cargarActividades();

    if (this.route.snapshot.data['abrirNuevaActividad']) {
      this.abrirDialogo();
      return;
    }

    const idParam = this.route.snapshot.paramMap.get('id');
    if (this.route.snapshot.data['abrirEdicion'] && idParam) {
      const id = Number(idParam);
      if (Number.isFinite(id) && id > 0) {
        this.abrirDialogo(id);
      }
    }
  }

  cargarActividades() {
    this.loading.set(true);
    this.error.set('');
    this.actividadesService.listar().subscribe({
      next: (data) => {
        this.actividades.set(data || []);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Error al cargar actividades.');
        this.loading.set(false);
      }
    });
  }

  cargarEtiquetas() {
    this.etiquetasService.listar(true).subscribe({
      next: (data) => {
        const catalogo = data || [];
        this.etiquetas.set(catalogo);
        const filtroActual = this.filtroEtiquetaId();
        if (filtroActual !== null && !catalogo.some(etiqueta => etiqueta.id === filtroActual)) {
          this.filtroEtiquetaId.set(null);
        }
      },
      error: () => this.etiquetas.set([])
    });
  }

  onFiltroEtiquetaChange(valor: number | string | null) {
    if (valor === null || valor === '' || valor === undefined) {
      this.filtroEtiquetaId.set(null);
      return;
    }
    const id = Number(valor);
    this.filtroEtiquetaId.set(Number.isFinite(id) ? id : null);
  }

  limpiarFiltroEtiqueta() {
    this.filtroEtiquetaId.set(null);
  }

  limpiarFiltros() {
    this.filtroTexto.set('');
    this.filtroEtiquetaId.set(null);
  }

  get puedeGestionarEtiquetas(): boolean {
    return this.authService.hasRole('ADMIN', 'JEFE');
  }

  abrirGestionEtiquetas() {
    this.dialog
      .open(EtiquetaManagerDialog, {
        panelClass: 'entity-dialog-panel',
        backdropClass: 'entity-dialog-backdrop',
        width: '640px',
        maxWidth: '95vw',
        autoFocus: false
      })
      .afterClosed()
      .subscribe(() => {
        this.cargarEtiquetas();
        this.cargarActividades();
      });
  }

  getEtiquetasVisibles(actividad: ActividadResponse): EtiquetaResponse[] {
    return (actividad.etiquetas || []).slice(0, 2);
  }

  getEtiquetasOcultas(actividad: ActividadResponse): number {
    return Math.max(0, (actividad.etiquetas?.length || 0) - 2);
  }

  colorEtiqueta(etiqueta: EtiquetaResponse): string {
    return etiqueta.color || '#2563EB';
  }

  completar(id: number) {
    this.actividadesService.cambiarEstado(id, EstadoActividad.COMPLETADA).subscribe({
      next: () => this.cargarActividades(),
      error: () => alert('Error al completar actividad')
    });
  }

  getEstadoClass(estado: EstadoActividad | string): string {
    switch (estado) {
      case EstadoActividad.PENDIENTE: return 'warning';
      case EstadoActividad.EN_PROCESO: return 'blue';
      case EstadoActividad.COMPLETADA: return 'success';
      case EstadoActividad.CANCELADA: return 'neutral';
      default: return 'neutral';
    }
  }

  getEstadoLabel(estado: EstadoActividad | string): string {
    switch (estado) {
      case EstadoActividad.PENDIENTE: return 'Pendiente';
      case EstadoActividad.EN_PROCESO: return 'En proceso';
      case EstadoActividad.COMPLETADA: return 'Completada';
      case EstadoActividad.CANCELADA: return 'Cancelada';
      default: return estado;
    }
  }

  getTipoLabel(tipo: TipoActividad | string): string {
    switch (tipo) {
      case TipoActividad.LLAMADA: return 'Llamada';
      case TipoActividad.REUNION: return 'Reunión';
      case TipoActividad.TAREA: return 'Tarea';
      case TipoActividad.SEGUIMIENTO: return 'Seguimiento';
      default: return tipo;
    }
  }

  getTipoIcon(tipo: TipoActividad | string): string {
    switch (tipo) {
      case TipoActividad.LLAMADA: return 'call';
      case TipoActividad.REUNION: return 'groups';
      case TipoActividad.TAREA: return 'task_alt';
      case TipoActividad.SEGUIMIENTO: return 'sync';
      default: return 'event';
    }
  }

  getTipoClass(tipo: TipoActividad | string): string {
    switch (tipo) {
      case TipoActividad.LLAMADA: return 'llamada';
      case TipoActividad.REUNION: return 'reunion';
      case TipoActividad.TAREA: return 'tarea';
      case TipoActividad.SEGUIMIENTO: return 'seguimiento';
      default: return 'general';
    }
  }

  eliminar(id: number) {
    if (confirm('¿Está seguro de eliminar esta actividad?')) {
      this.actividadesService.eliminar(id).subscribe({
        next: () => this.cargarActividades(),
        error: () => alert('Error al eliminar')
      });
    }
  }

  editar(id: number) {
    this.abrirDialogo(id);
  }

  nuevaActividad() {
    this.abrirDialogo();
  }

  private abrirDialogo(id?: number) {
    const enRutaDirecta = !!(
      this.route.snapshot.data['abrirNuevaActividad'] || this.route.snapshot.data['abrirEdicion']
    );

    this.dialog
      .open(ActividadForm, {
        data: { id: id ?? null },
        panelClass: 'actividad-dialog-panel',
        backdropClass: 'actividad-dialog-backdrop',
        width: '980px',
        maxWidth: '95vw',
        disableClose: false,
        autoFocus: false,
      })
      .afterClosed()
      .subscribe((result: ActividadDialogResult | undefined) => {
        if (result?.saved) {
          this.cargarActividades();
        }
        if (enRutaDirecta) {
          this.router.navigate(['/actividades'], { replaceUrl: true });
        }
      });
  }
}
