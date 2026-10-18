import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { UsuariosService } from '../usuarios.service';
import { UsuarioResponse, EstadoUsuario } from '../usuarios.model';

@Component({
  selector: 'app-usuario-list',
  standalone: true,
  imports: [CommonModule, MatTableModule, MatButtonModule, MatIconModule, RouterLink, MatProgressSpinnerModule],
  templateUrl: './usuario-list.html',
  styleUrl: './usuario-list.css'
})
export class UsuarioList implements OnInit {
  private usuariosService = inject(UsuariosService);
  private router = inject(Router);

  usuarios = signal<UsuarioResponse[]>([]);
  displayedColumns: string[] = ['id', 'nombre', 'correo', 'rol', 'estado', 'acciones'];
  loading = signal(false);
  error = signal('');

  hasUsuarios = computed(() => this.usuarios().length > 0 && !this.loading());
  isEmpty = computed(() => !this.loading() && this.usuarios().length === 0 && !this.error());

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
      error: (err) => alert(err?.friendlyMessage || `Error al ${accion} el usuario`)
    });
  }

  editar(id: number) {
    this.router.navigate(['/usuarios', id, 'editar']);
  }
}

