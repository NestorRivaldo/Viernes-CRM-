import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { PropuestasService } from '../propuestas.service';
import { PropuestaResponse, EstadoPropuesta } from '../propuestas.model';
import { PropuestaDialogResult, PropuestaForm } from '../propuesta-form/propuesta-form';
import { sortRows } from '../../../shared/table-sort';

@Component({
  selector: 'app-propuesta-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatTableModule,
    MatSortModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatDialogModule
  ],
  templateUrl: './propuesta-list.html',
  styleUrl: './propuesta-list.css'
})
export class PropuestaList implements OnInit {
  private propuestasService = inject(PropuestasService);
  private dialog = inject(MatDialog);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  propuestas = signal<PropuestaResponse[]>([]);
  filtroTexto = signal('');
  idOportunidadFiltro = signal<number | null>(null);
  loading = signal(false);
  error = signal('');

  displayedColumns: string[] = ['numeroPropuesta', 'cliente', 'estado', 'total', 'fechaEmision', 'acciones'];

  sortState = signal<Sort>({ active: 'numeroPropuesta', direction: 'asc' });

  propuestasFiltradas = computed(() => {
    const q = this.filtroTexto().toLowerCase().trim();
    return this.propuestas().filter(p => {
      return !q ||
        p.numeroPropuesta?.toLowerCase().includes(q) ||
        p.clienteNombre?.toLowerCase().includes(q) ||
        p.responsable?.toLowerCase().includes(q) ||
        p.estado?.toLowerCase().includes(q);
    });
  });

  totalPropuestasValor = computed(() => {
    return this.propuestasFiltradas().reduce((acc, p) => acc + (p.total || 0), 0);
  });

  propuestasOrdenadas = computed(() => sortRows(this.propuestasFiltradas(), this.sortState()));


  onSortChange(sort: Sort) {

    this.sortState.set(sort);

  }


  hasPropuestas = computed(() => this.propuestasFiltradas().length > 0 && !this.loading());
  isEmpty = computed(() => !this.loading() && this.propuestasFiltradas().length === 0 && !this.error());

  ngOnInit() {
    this.route.queryParamMap.subscribe((params) => {
      const raw = params.get('idOportunidad');
      const id = raw ? Number(raw) : null;
      this.idOportunidadFiltro.set(id && Number.isFinite(id) ? id : null);
      this.cargarPropuestas();
    });

    if (this.route.snapshot.data['abrirNuevaPropuesta']) {
      this.abrirDialogo();
      return;
    }

    const idParam = this.route.snapshot.paramMap.get('id');
    if (this.route.snapshot.data['abrirEdicionPropuesta'] && idParam) {
      const id = Number(idParam);
      if (Number.isFinite(id) && id > 0) {
        this.abrirDialogo(id);
      }
    }
  }

  limpiarFiltroOportunidad() {
    this.router.navigate(['/propuestas']);
  }

  cargarPropuestas() {
    this.loading.set(true);
    this.error.set('');
    this.propuestasService.listar(this.idOportunidadFiltro() ?? undefined).subscribe({
      next: (data) => {
        this.propuestas.set(data || []);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Error al cargar propuestas.');
        this.loading.set(false);
      }
    });
  }

  getEstadoClass(estado: EstadoPropuesta | string): string {
    switch (estado) {
      case EstadoPropuesta.BORRADOR: return 'warning';
      case EstadoPropuesta.ENVIADA: return 'blue';
      case EstadoPropuesta.APROBADA: return 'success';
      case EstadoPropuesta.RECHAZADA: return 'danger';
      case EstadoPropuesta.CANCELADA: return 'neutral';
      default: return 'neutral';
    }
  }

  formatearSoles(valor: number): string {
    return new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN', maximumFractionDigits: 0 }).format(valor || 0);
  }

  eliminar(id: number) {
    if (confirm('¿Está seguro de eliminar esta propuesta? (Solo si está en BORRADOR)')) {
      this.propuestasService.eliminar(id).subscribe({
        next: () => this.cargarPropuestas(),
        error: (err) => alert(err?.friendlyMessage || 'Error al eliminar, es posible que no esté en estado BORRADOR')
      });
    }
  }

  nuevaPropuesta() {
    this.abrirDialogo();
  }

  editar(id: number) {
    this.abrirDialogo(id);
  }

  private abrirDialogo(id?: number) {
    const enRutaDirecta = !!(
      this.route.snapshot.data['abrirNuevaPropuesta'] || this.route.snapshot.data['abrirEdicionPropuesta']
    );

    this.dialog
      .open(PropuestaForm, {
        data: { id: id ?? null },
        panelClass: 'entity-dialog-panel',
        backdropClass: 'entity-dialog-backdrop',
        width: '980px',
        maxWidth: '96vw',
        disableClose: false,
        autoFocus: false,
      })
      .afterClosed()
      .subscribe((result: PropuestaDialogResult | undefined) => {
        if (result?.saved) {
          this.cargarPropuestas();
        }

        if (enRutaDirecta) {
          const idOportunidad = this.idOportunidadFiltro();
          this.router.navigate(['/propuestas'], {
            replaceUrl: true,
            queryParams: idOportunidad ? { idOportunidad } : {},
          });
        }
      });
  }
}
