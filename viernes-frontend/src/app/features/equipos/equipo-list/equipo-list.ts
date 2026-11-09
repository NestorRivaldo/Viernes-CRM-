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
import { EquiposService } from '../equipos.service';
import { EquipoResponse } from '../equipo.model';
import { EquipoDialogResult, EquipoForm } from '../equipo-form/equipo-form';
import { sortRows } from '../../../shared/table-sort';

@Component({
  selector: 'app-equipo-list',
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
  templateUrl: './equipo-list.html',
  styleUrl: './equipo-list.css'
})
export class EquipoList implements OnInit {
  private equiposService = inject(EquiposService);
  private dialog = inject(MatDialog);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  equipos = signal<EquipoResponse[]>([]);
  filtroTexto = signal('');
  filtroActivo = signal<string>('TODOS');
  loading = signal(false);
  error = signal('');

  displayedColumns: string[] = ['id', 'nombre', 'miembros', 'estado', 'acciones'];

  sortState = signal<Sort>({ active: 'id', direction: 'asc' });

  equiposFiltrados = computed(() => {
    const q = this.filtroTexto().toLowerCase().trim();
    return this.equipos().filter(equipo => {
      return !q ||
        equipo.nombre?.toLowerCase().includes(q) ||
        equipo.descripcion?.toLowerCase().includes(q);
    });
  });

  equiposOrdenados = computed(() => sortRows(this.equiposFiltrados(), this.sortState()));


  onSortChange(sort: Sort) {

    this.sortState.set(sort);

  }


  hasEquipos = computed(() => this.equiposFiltrados().length > 0 && !this.loading());
  isEmpty = computed(() => !this.loading() && this.equiposFiltrados().length === 0 && !this.error());

  ngOnInit() {
    this.cargarEquipos();

    if (this.route.snapshot.data['abrirNuevoEquipo']) {
      this.abrirDialogo();
      return;
    }

    const idParam = this.route.snapshot.paramMap.get('id');
    if (this.route.snapshot.data['abrirEdicionEquipo'] && idParam) {
      const id = Number(idParam);
      if (Number.isFinite(id) && id > 0) {
        this.abrirDialogo(id);
      }
    }
  }

  cargarEquipos() {
    this.loading.set(true);
    this.error.set('');

    const filtro = this.filtroActivo();
    const activo = filtro === 'TODOS' ? undefined : filtro === 'ACTIVOS';

    this.equiposService.listar(activo).subscribe({
      next: (data) => {
        this.equipos.set(data || []);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Error al cargar equipos. Verifique que el backend esté disponible.');
        this.loading.set(false);
      }
    });
  }

  cambiarEstado(equipo: EquipoResponse) {
    const accion = equipo.activo ? 'desactivar' : 'activar';
    if (!confirm(`¿Está seguro de ${accion} el equipo ${equipo.nombre}?`)) {
      return;
    }
    const req$ = equipo.activo
      ? this.equiposService.desactivar(equipo.id)
      : this.equiposService.activar(equipo.id);

    req$.subscribe({
      next: () => this.cargarEquipos(),
      error: (err) => alert(err?.friendlyMessage || `Error al ${accion} el equipo`)
    });
  }

  eliminar(equipo: EquipoResponse) {
    if (confirm(`¿Está seguro de eliminar el equipo ${equipo.nombre}?`)) {
      this.equiposService.eliminar(equipo.id).subscribe({
        next: () => this.cargarEquipos(),
        error: (err) => alert(err?.friendlyMessage || 'Error al eliminar el equipo')
      });
    }
  }

  nuevoEquipo() {
    this.abrirDialogo();
  }

  editar(id: number) {
    this.abrirDialogo(id);
  }

  private abrirDialogo(id?: number) {
    const enRutaDirecta = !!(
      this.route.snapshot.data['abrirNuevoEquipo'] || this.route.snapshot.data['abrirEdicionEquipo']
    );

    this.dialog
      .open(EquipoForm, {
        data: { id: id ?? null },
        panelClass: 'entity-dialog-panel',
        backdropClass: 'entity-dialog-backdrop',
        width: '900px',
        maxWidth: '95vw',
        disableClose: false,
        autoFocus: false,
      })
      .afterClosed()
      .subscribe((result: EquipoDialogResult | undefined) => {
        if (result?.saved) {
          this.cargarEquipos();
        }
        if (enRutaDirecta) {
          this.router.navigate(['/equipos'], { replaceUrl: true });
        }
      });
  }
}
