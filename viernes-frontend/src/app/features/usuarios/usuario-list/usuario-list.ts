import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { UsuariosService } from '../usuarios.service';
import { UsuarioResponse, EstadoUsuario, RolUsuario } from '../usuarios.model';

@Component({
  selector: 'app-usuario-list',
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
  templateUrl: './usuario-list.html',
  styleUrl: './usuario-list.css'
})
export class UsuarioList implements OnInit {
  private usuariosService = inject(UsuariosService);
  private router = inject(Router);

  usuarios = signal<UsuarioResponse[]>([]);
  filtroTexto = signal('');
  loading = signal(false);
  error = signal('');

  displayedColumns: string[] = ['id', 'nombre', 'correo', 'rol', 'estado', 'acciones'];

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

  hasUsuarios = computed(() => this.usuariosFiltrados().length > 0 && !this.loading());
  isEmpty = computed(() => !this.loading() && this.usuariosFiltrados().length === 0 && !this.error());

  ngOnInit() {
    this.cargarUsuarios();
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

  editar(id: number) {
    this.router.navigate(['/usuarios', id, 'editar']);
  }
}
