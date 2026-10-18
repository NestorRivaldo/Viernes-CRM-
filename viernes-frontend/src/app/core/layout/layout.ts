import { Component } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive, Router } from '@angular/router';
import { AuthService, UsuarioSesion } from '../auth/auth.service';

@Component({
  selector: 'app-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './layout.html',
  styleUrl: './layout.css',
})
export class Layout {
  constructor(private authService: AuthService, private router: Router) {}

  get usuario(): UsuarioSesion | null {
    return this.authService.getUsuario();
  }

  get esAdmin(): boolean {
    return this.authService.hasRole('ADMIN');
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
