import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, finalize, tap } from 'rxjs';
import { ApiService } from '../../core/services/api-service';
import { AuthService } from '../../core/auth/auth.service';
import { MensajeResponse } from './mensajeria.model';

@Injectable({ providedIn: 'root' })
export class MensajeriaService {
  private readonly path = '/mensajeria';
  private readonly intervaloMs = 5000;
  private readonly maxMensajes = 100;

  private http = inject(HttpClient);
  private apiService = inject(ApiService);
  private authService = inject(AuthService);

  readonly mensajes = signal<MensajeResponse[]>([]);
  readonly cargando = signal(false);
  readonly enviando = signal(false);
  readonly error = signal('');
  readonly noLeidos = signal(0);
  readonly panelAbierto = signal(false);
  readonly conectado = signal(false);

  private timer: ReturnType<typeof setInterval> | null = null;
  private ultimoId = 0;
  private ultimoLeidoId = 0;
  private usuarioActual = '';

  iniciar() {
    const correo = this.correoActual();
    if (this.usuarioActual !== correo) {
      this.usuarioActual = correo;
      this.mensajes.set([]);
      this.ultimoId = 0;
      this.ultimoLeidoId = 0;
      this.noLeidos.set(0);
      this.panelAbierto.set(false);
    }

    this.detener();
    this.cargarRecientes(true);
    this.timer = setInterval(() => this.poll(), this.intervaloMs);
  }

  detener() {
    if (this.timer) {
      clearInterval(this.timer);
      this.timer = null;
    }
    this.panelAbierto.set(false);
  }

  abrirPanel() {
    this.panelAbierto.set(true);

    if (!this.cargando() && this.mensajes().length === 0 && !this.conectado()) {
      this.cargarRecientes(true);
    }

    this.marcarLeido();
  }

  cerrarPanel() {
    this.panelAbierto.set(false);
  }

  alternarPanel() {
    if (this.panelAbierto()) {
      this.cerrarPanel();
    } else {
      this.abrirPanel();
    }
  }

  cargarRecientes(inicial = false) {
    this.cargando.set(true);
    this.error.set('');

    const params = new HttpParams().set('limite', String(this.maxMensajes));
    this.http.get<MensajeResponse[]>(this.url(), { params }).subscribe({
      next: (data) => {
        this.merge(data);
        this.cargando.set(false);
        this.conectado.set(true);

        if (inicial) {
          this.inicializarLeido();
        }

        if (this.panelAbierto()) {
          this.marcarLeido();
        } else {
          this.actualizarNoLeidos();
        }
      },
      error: (err) => {
        this.cargando.set(false);
        this.conectado.set(false);
        this.error.set(this.mensajeError(err, 'No se pudieron cargar los mensajes.'));
      },
    });
  }

  enviar(contenido: string): Observable<MensajeResponse> | null {
    const texto = contenido.trim();
    if (!texto || this.enviando()) {
      return null;
    }

    this.enviando.set(true);
    this.error.set('');

    return this.http.post<MensajeResponse>(this.url(), { contenido: texto }).pipe(
      tap({
        next: (mensaje) => {
          this.merge([mensaje]);
          this.marcarLeido();
        },
        error: (err) => {
          this.error.set(this.mensajeError(err, 'No se pudo enviar el mensaje.'));
        },
      }),
      finalize(() => this.enviando.set(false))
    );
  }

  marcarLeido() {
    if (this.mensajes().length === 0) {
      this.noLeidos.set(0);
      return;
    }

    this.ultimoLeidoId = Math.max(...this.mensajes().map((m) => m.id));
    localStorage.setItem(this.storageKey(), String(this.ultimoLeidoId));
    this.noLeidos.set(0);
  }

  esMio(mensaje: MensajeResponse): boolean {
    return (mensaje.usuarioCorreo || '').toLowerCase() === this.correoActual().toLowerCase();
  }

  private poll() {
    const despuesDeId = this.ultimoId;
    const params = new HttpParams()
      .set('despuesDeId', String(despuesDeId))
      .set('limite', String(this.maxMensajes));

    this.http.get<MensajeResponse[]>(this.url(), { params }).subscribe({
      next: (data) => {
        this.conectado.set(true);
        this.error.set('');
        if (data.length === 0) {
          return;
        }

        this.merge(data);
        if (this.panelAbierto()) {
          this.marcarLeido();
        } else {
          this.actualizarNoLeidos();
        }
      },
      error: () => {
        this.conectado.set(false);
      },
    });
  }

  private inicializarLeido() {
    const guardado = Number(localStorage.getItem(this.storageKey()) || 0);
    this.ultimoLeidoId = guardado > 0 ? guardado : this.ultimoId;

    if (!guardado && this.ultimoId > 0) {
      localStorage.setItem(this.storageKey(), String(this.ultimoId));
    }

    this.actualizarNoLeidos();
  }

  private actualizarNoLeidos() {
    const correo = this.correoActual().toLowerCase();
    const total = this.mensajes().filter(
      (m) => m.id > this.ultimoLeidoId && (m.usuarioCorreo || '').toLowerCase() !== correo
    ).length;
    this.noLeidos.set(total);
  }

  private merge(nuevos: MensajeResponse[]) {
    if (nuevos.length === 0) {
      return;
    }

    const mapa = new Map(this.mensajes().map((m) => [m.id, m]));
    for (const mensaje of nuevos) {
      mapa.set(mensaje.id, mensaje);
    }

    const ordenados = [...mapa.values()]
      .sort((a, b) => a.id - b.id)
      .slice(-this.maxMensajes);

    this.mensajes.set(ordenados);
    this.ultimoId = Math.max(this.ultimoId, ...nuevos.map((m) => m.id));
  }

  private correoActual(): string {
    return this.authService.getUsuario()?.correo || '';
  }

  private storageKey(): string {
    return `viernes_chat_last_read_${encodeURIComponent(this.correoActual() || 'anon')}`;
  }

  private url(): string {
    return this.apiService.buildUrl(this.path);
  }

  private mensajeError(err: any, fallback: string): string {
    return err?.friendlyMessage || err?.error?.message || fallback;
  }
}
