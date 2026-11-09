import { Component, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [],
  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class Login implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly loading = signal(false);
  readonly error = signal('');
  readonly year = new Date().getFullYear();

  ngOnInit(): void {
    if (this.authService.hasToken()) {
      void this.router.navigate(['/dashboard']);
    }
  }

  async entrar(): Promise<void> {
    if (this.loading()) return;

    this.loading.set(true);
    this.error.set('');

    try {
      await this.authService.login();
    } catch (error) {
      console.error('[Login] No se pudo iniciar el flujo de Keycloak:', error);
      this.error.set(
        'No pudimos conectar con el servicio de identidad. Verifica que Keycloak esté disponible e inténtalo nuevamente.'
      );
      this.loading.set(false);
    }
  }
}
