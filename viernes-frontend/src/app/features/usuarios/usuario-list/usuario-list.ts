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
import { UsuariosService } from '../usuarios.service';
import { UsuarioResponse, EstadoUsuario, RolUsuario } from '../usuarios.model';
import { UsuarioDialogResult, UsuarioForm } from '../usuario-form/usuario-form';
import { sortRows } from '../../../shared/table-sort';

@Component({
  selector: 'app-usuario-list',
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
  templateUrl: './usuario-list.html',
  styleUrl: './usuario-list.css'
})
export class UsuarioList implements OnInit {
  private usuariosService = inject(UsuariosService);
  private dialog = inject(MatDialog);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  usuarios = signal<UsuarioResponse[]>([]);
  filtroTexto = signal('');
  loading = signal(false);
  error = signal('');

  displayedColumns: string[] = ['id', 'nombre', 'correo', 'rol', 'estado', 'acciones'];

  sortState = signal<Sort>({ active: 'id', direction: 'asc' });

  usuariosFiltrados = computed(() => {
    const q = this.filtroTexto().toLowerCase().trim();
    return this.usuarios().filter(u => {
      return !q ||
        u.nombre?.toLowerCase().includes(q) ||
        u.apellido?.toLowerCase().includes(q) ||
        u.correo?.toLowerCase().includes(q) ||
        u.rol?.toLowerCase().includes(q);
    });
  });

  usuariosOrdenados = computed(() => sortRows(this.usuariosFiltrados(), this.sortState()));


  onSortChange(sort: Sort) {

    this.sortState.set(sort);

  }


  hasUsuarios = computed(() => this.usuariosFiltrados().length > 0 && !this.loading());
  isEmpty = computed(() => !this.loading() && this.usuariosFiltrados().length === 0 && !this.error());

  ngOnInit() {
    this.cargarUsuarios();

    if (this.route.snapshot.data['abrirNuevoUsuario']) {
      this.abrirDialogo();
      return;
    }

    const idParam = this.route.snapshot.paramMap.get('id');
    if (this.route.snapshot.data['abrirEdicionUsuario'] && idParam) {
      const id = Number(idParam);
      if (Number.isFinite(id) && id > 0) {
        this.abrirDialogo(id);
      }
    }
  }

  cargarUsuarios() {
    this.loading.set(true);
    this.error.set('');
    this.usuariosService.listar().subscribe({
      next: (data) => {
        this.usuarios.set(data || []);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Error al cargar usuarios.');
        this.loading.set(false);
      }
    });
  }

  cambiarEstado(usuario: UsuarioResponse) {
    const esActivo = usuario.estado === EstadoUsuario.ACTIVO;
    const accion = esActivo ? 'desactivar' : 'activar';
    if (!confirm(`¿Está seguro de ${accion} al usuario ${usuario.nombre} ${usuario.apellido}?`)) {
      return;
    }
    const req$ = esActivo
      ? this.usuariosService.desactivar(usuario.id)
      : this.usuariosService.activar(usuario.id);

    req$.subscribe({
      next: () => this.cargarUsuarios(),
      error: () => alert(`Error al ${accion} usuario`)
    });
  }

  getRolBadgeClass(rol: RolUsuario | string): string {
    switch (rol) {
      case 'ADMIN': return 'purple';
      case 'JEFE': return 'blue';
      case 'VENDEDOR': return 'emerald';
      default: return 'neutral';
    }
  }

  nuevoUsuario() {
    this.abrirDialogo();
  }

  editar(id: number) {
    this.abrirDialogo(id);
  }

  private abrirDialogo(id?: number) {
    const enRutaDirecta = !!(
      this.route.snapshot.data['abrirNuevoUsuario'] || this.route.snapshot.data['abrirEdicionUsuario']
    );

    this.dialog
      .open(UsuarioForm, {
        data: { id: id ?? null },
        panelClass: 'entity-dialog-panel',
        backdropClass: 'entity-dialog-backdrop',
        width: '840px',
        maxWidth: '95vw',
        disableClose: false,
        autoFocus: false,
      })
      .afterClosed()
      .subscribe((result: UsuarioDialogResult | undefined) => {
        if (result?.saved) {
          this.cargarUsuarios();
        }
        if (enRutaDirecta) {
          this.router.navigate(['/usuarios'], { replaceUrl: true });
        }
      });
  }
}
