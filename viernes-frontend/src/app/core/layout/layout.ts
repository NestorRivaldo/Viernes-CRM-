import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, RouterLink, RouterLinkActive, Router } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { AuthService, UsuarioSesion } from '../auth/auth.service';
import { MensajeriaService } from '../../features/mensajeria/mensajeria.service';
import { ChatPanel } from '../../features/mensajeria/chat-panel/chat-panel';

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
    MatDividerModule,
    ChatPanel
  ],
  templateUrl: './layout.html',
  styleUrl: './layout.css',
})
export class Layout implements OnInit, OnDestroy {
  readonly mensajeria = inject(MensajeriaService);
  sidebarOpen = true;

  constructor(private authService: AuthService, private router: Router) {}

  ngOnInit() {
    this.mensajeria.iniciar();
  }

  ngOnDestroy() {
    this.mensajeria.detener();
  }

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

  get esJefe(): boolean {
    return this.authService.hasRole('JEFE');
  }

  get puedeVerAdministracion(): boolean {
    return this.esAdmin || this.esJefe;
  }

  toggleSidebar() {
    this.sidebarOpen = !this.sidebarOpen;
  }

  async logout() {
    try {
      await this.authService.logout();
    } catch {
      await this.router.navigate(['/login']);
    }
  }
}
