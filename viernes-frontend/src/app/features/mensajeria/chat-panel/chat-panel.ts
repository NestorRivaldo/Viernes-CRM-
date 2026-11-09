import {
  Component,
  ElementRef,
  HostListener,
  afterNextRender,
  effect,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MensajeriaService } from '../mensajeria.service';
import { MensajeResponse } from '../mensajeria.model';

@Component({
  selector: 'app-chat-panel',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  templateUrl: './chat-panel.html',
  styleUrl: './chat-panel.css',
})
export class ChatPanel {
  readonly mensajeria = inject(MensajeriaService);
  readonly texto = signal('');

  private readonly scrollRef = viewChild<ElementRef<HTMLDivElement>>('scroll');
  private readonly composerRef = viewChild<ElementRef<HTMLTextAreaElement>>('composer');

  constructor() {
    effect(() => {
      this.mensajeria.mensajes();
      setTimeout(() => this.scrollToBottom(), 20);
    });

    afterNextRender(() => this.composerRef()?.nativeElement.focus());
  }

  enviar(event?: Event) {
    event?.preventDefault();

    const texto = this.texto().trim();
    if (!texto) {
      return;
    }

    const peticion = this.mensajeria.enviar(texto);
    if (!peticion) {
      return;
    }

    peticion.subscribe({
      next: () => {
        this.texto.set('');
        const composer = this.composerRef()?.nativeElement;
        if (composer) {
          composer.style.height = 'auto';
          composer.focus();
        }
      },
    });
  }

  onInput(event: Event) {
    const target = event.target as HTMLTextAreaElement;
    this.texto.set(target.value);
    target.style.height = 'auto';
    target.style.height = `${Math.min(target.scrollHeight, 96)}px`;
  }

  onEnter(event: Event) {
    const keyboard = event as KeyboardEvent;
    if (!keyboard.shiftKey) {
      event.preventDefault();
      this.enviar();
    }
  }

  cerrar() {
    this.mensajeria.cerrarPanel();
  }

  @HostListener('document:keydown.escape')
  onEscape() {
    this.cerrar();
  }

  hora(mensaje: MensajeResponse): string {
    const fecha = new Date(mensaje.creadoEn);
    const hoy = new Date();
    const mismoDia = fecha.toDateString() === hoy.toDateString();

    return new Intl.DateTimeFormat('es-PE', {
      hour12: false,
      ...(mismoDia
        ? { hour: '2-digit', minute: '2-digit' }
        : { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' }),
    }).format(fecha);
  }

  iniciales(nombre: string | null | undefined): string {
    return (
      (nombre || '')
        .trim()
        .split(/\s+/)
        .filter(Boolean)
        .slice(0, 2)
        .map((parte) => parte[0]?.toUpperCase())
        .join('') || '?'
    );
  }

  private scrollToBottom() {
    const contenedor = this.scrollRef()?.nativeElement;
    if (contenedor) {
      contenedor.scrollTop = contenedor.scrollHeight;
    }
  }
}
