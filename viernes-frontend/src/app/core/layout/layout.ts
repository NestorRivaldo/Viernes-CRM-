import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, RouterLink, RouterLinkActive, Router } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { AuthService, UsuarioSesion } from '../auth/auth.service';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatIconModule,
    MatButtonModule,
    MatMenuModule,
    MatDividerModule
  ],
  templateUrl: './layout.html',
  styleUrl: './layout.css',
})
export class Layout {
  sidebarOpen = true;

  constructor(private authService: AuthService, private router: Router) {}

  get usuario(): UsuarioSesion | null {
    return this.authService.getUsuario();
  }

  get nombreCompleto(): string {
    const u = this.usuario;
    if (!u) return 'Usuario';
    if (u.nombre || u.apellido) {
      return `${u.nombre || ''} ${u.apellido || ''}`.trim();
    }
    return u.correo ? u.correo.split('@')[0] : 'Usuario';
  }

  get rolEtiqueta(): string {
    const rol = this.usuario?.rol;
    if (rol === 'ADMIN') return 'Administrador';
    if (rol === 'JEFE') return 'Jefe de Ventas';
    if (rol === 'VENDEDOR') return 'Vendedor';
    return rol || 'Usuario';
  }

  get iniciales(): string {
    const u = this.usuario;
    if (!u) return 'U';
    const n = (u.nombre || u.correo || 'U').charAt(0).toUpperCase();
    const a = (u.apellido || '').charAt(0).toUpperCase();
    return `${n}${a}`.trim() || 'U';
  }

  get esAdmin(): boolean {
    return this.authService.hasRole('ADMIN');
  }

  toggleSidebar() {
    this.sidebarOpen = !this.sidebarOpen;
  }

  onSearch(event: any) {
    const val = event.target?.value?.trim();
    if (val) {
      this.router.navigate(['/clientes'], { queryParams: { q: val } });
    }
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
