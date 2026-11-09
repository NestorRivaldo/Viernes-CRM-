import {
  ChangeDetectorRef,
  Component,
  ElementRef,
  EventEmitter,
  HostListener,
  Input,
  Output,
  inject
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { EtiquetaResponse } from '../etiquetas.model';
import { EtiquetasService } from '../etiquetas.service';

@Component({
  selector: 'app-etiqueta-selector',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  templateUrl: './etiqueta-selector.html',
  styleUrl: './etiqueta-selector.css'
})
export class EtiquetaSelectorComponent {
  @Input() catalogo: EtiquetaResponse[] = [];
  @Input() seleccionadas: EtiquetaResponse[] = [];
  @Input() max = 8;
  @Input() disabled = false;
  @Input() cargando = false;
  @Input() errorCarga = '';

  @Output() seleccionadasChange = new EventEmitter<EtiquetaResponse[]>();
  @Output() etiquetaCreada = new EventEmitter<EtiquetaResponse>();

  busqueda = '';
  abierto = false;
  creando = false;
  errorCreacion = '';
  indiceActivo = -1;
  draggingId: number | null = null;
  dragOverId: number | null = null;
  dropUp = false;
  creacionRapida = false;

  private readonly elementRef = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly etiquetasService = inject(EtiquetasService);
  private readonly cdr = inject(ChangeDetectorRef);

  private readonly paleta = [
    '#2563EB',
    '#7C3AED',
    '#059669',
    '#D97706',
    '#DC2626',
    '#0891B2',
    '#DB2777',
    '#4F46E5'
  ];

  get alcanzoMaximo(): boolean {
    return this.seleccionadas.length >= this.max;
  }

  get etiquetasDisponibles(): EtiquetaResponse[] {
    const q = this.normalizar(this.busqueda);
    return (this.catalogo || [])
      .filter((etiqueta) => etiqueta.activa !== false)
      .filter((etiqueta) => !this.seleccionadas.some((seleccionada) => seleccionada.id === etiqueta.id))
      .filter((etiqueta) => !q || this.normalizar(etiqueta.nombre).includes(q))
      .slice()
      .sort((a, b) => a.nombre.localeCompare(b.nombre, 'es'));
  }

  get puedeCrear(): boolean {
    const nombre = this.busqueda.trim();
    if (!nombre || this.alcanzoMaximo || this.cargando || this.creando) {
      return false;
    }
    return !this.catalogo.some((etiqueta) => this.normalizar(etiqueta.nombre) === this.normalizar(nombre));
  }

  get mostrarDropdown(): boolean {
    return this.abierto && !this.disabled;
  }

  get placeholderInput(): string {
    if (this.alcanzoMaximo) return 'Máximo de etiquetas alcanzado';
    return this.creacionRapida
      ? 'Escribe el nombre de la nueva etiqueta...'
      : 'Buscar o crear etiqueta...';
  }

  onBusqueda(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.busqueda = input.value;
    this.abierto = true;
    this.actualizarDireccion();
    this.indiceActivo = -1;
    this.errorCreacion = '';
    this.cdr.markForCheck();
  }

  abrir(): void {
    if (this.disabled) return;
    this.abierto = true;
    this.actualizarDireccion();
    this.cdr.markForCheck();
  }

  iniciarCreacionRapida(): void {
    if (this.disabled || this.alcanzoMaximo || this.creando) return;

    this.creacionRapida = true;
    this.busqueda = '';
    this.abierto = true;
    this.indiceActivo = -1;
    this.errorCreacion = '';
    this.actualizarDireccion();
    this.cdr.markForCheck();

    const input = this.elementRef.nativeElement.querySelector<HTMLInputElement>('.tag-input');
    input?.focus();
  }

  private actualizarDireccion(): void {
    const host = this.elementRef.nativeElement.getBoundingClientRect();
    const contenedor = this.elementRef.nativeElement.closest('.dialog-body') as HTMLElement | null;
    const limiteInferior = contenedor ? contenedor.getBoundingClientRect().bottom : window.innerHeight;
    const limiteSuperior = contenedor ? contenedor.getBoundingClientRect().top : 0;
    const espacioAbajo = limiteInferior - host.bottom;
    const espacioArriba = host.top - limiteSuperior;

    this.dropUp = espacioAbajo < 240 && espacioArriba > espacioAbajo;
  }

  onKeydown(event: KeyboardEvent): void {
    if (this.disabled) return;

    if (event.key === 'Escape') {
      this.abierto = false;
      this.creacionRapida = false;
      this.cdr.markForCheck();
      return;
    }

    if (event.key === 'Backspace' && !this.busqueda && this.seleccionadas.length > 0) {
      event.preventDefault();
      this.quitar(this.seleccionadas[this.seleccionadas.length - 1]);
      return;
    }

    const disponibles = this.etiquetasDisponibles;

    if (event.key === 'ArrowDown') {
      event.preventDefault();
      if (!disponibles.length) return;
      this.abierto = true;
      this.indiceActivo = Math.min(this.indiceActivo + 1, disponibles.length - 1);
      this.cdr.markForCheck();
      return;
    }

    if (event.key === 'ArrowUp') {
      event.preventDefault();
      if (!disponibles.length) return;
      this.abierto = true;
      this.indiceActivo = Math.max(this.indiceActivo - 1, 0);
      this.cdr.markForCheck();
      return;
    }

    if (event.key === 'Enter') {
      event.preventDefault();
      if (this.indiceActivo >= 0 && disponibles[this.indiceActivo]) {
        this.seleccionar(disponibles[this.indiceActivo]);
      } else if (disponibles[0]) {
        this.seleccionar(disponibles[0]);
      } else if (this.puedeCrear) {
        this.crear();
      }
    }
  }

  seleccionar(etiqueta: EtiquetaResponse): void {
    if (this.disabled || this.alcanzoMaximo) return;
    if (this.seleccionadas.some((seleccionada) => seleccionada.id === etiqueta.id)) return;

    this.seleccionadasChange.emit([...this.seleccionadas, etiqueta]);
    this.busqueda = '';
    this.abierto = false;
    this.creacionRapida = false;
    this.indiceActivo = -1;
    this.errorCreacion = '';
    this.cdr.markForCheck();
  }

  quitar(etiqueta: EtiquetaResponse): void {
    if (this.disabled) return;
    this.seleccionadasChange.emit(this.seleccionadas.filter((seleccionada) => seleccionada.id !== etiqueta.id));
    this.cdr.markForCheck();
  }

  onDragStart(etiqueta: EtiquetaResponse, event: DragEvent): void {
    if (this.disabled || (event.target as HTMLElement).closest('.tag-remove')) {
      event.preventDefault();
      return;
    }

    this.draggingId = etiqueta.id;
    this.dragOverId = null;
    if (event.dataTransfer) {
      event.dataTransfer.effectAllowed = 'move';
      event.dataTransfer.setData('text/plain', String(etiqueta.id));
    }
    this.cdr.markForCheck();
  }

  onDragOver(etiqueta: EtiquetaResponse, event: DragEvent): void {
    if (this.disabled || this.draggingId === null || this.draggingId === etiqueta.id) {
      return;
    }

    event.preventDefault();
    if (event.dataTransfer) {
      event.dataTransfer.dropEffect = 'move';
    }
    if (this.dragOverId !== etiqueta.id) {
      this.dragOverId = etiqueta.id;
      this.cdr.markForCheck();
    }
  }

  onDrop(destino: EtiquetaResponse, event: DragEvent): void {
    event.preventDefault();
    if (this.disabled || this.draggingId === null || this.draggingId === destino.id) {
      this.onDragEnd();
      return;
    }

    const movidas = [...this.seleccionadas];
    const desde = movidas.findIndex((etiqueta) => etiqueta.id === this.draggingId);
    const hasta = movidas.findIndex((etiqueta) => etiqueta.id === destino.id);

    if (desde >= 0 && hasta >= 0 && desde !== hasta) {
      const [etiqueta] = movidas.splice(desde, 1);
      movidas.splice(hasta, 0, etiqueta);
      this.seleccionadasChange.emit(movidas);
    }

    this.onDragEnd();
  }

  onDragEnd(): void {
    this.draggingId = null;
    this.dragOverId = null;
    this.cdr.markForCheck();
  }

  crear(): void {
    if (this.creando || this.alcanzoMaximo) return;

    const nombre = this.busqueda.trim().replace(/\s+/g, ' ');
    if (!nombre) return;

    const existente = this.catalogo.find(
      (etiqueta) => this.normalizar(etiqueta.nombre) === this.normalizar(nombre)
    );
    if (existente) {
      this.seleccionar(existente);
      return;
    }

    this.creando = true;
    this.errorCreacion = '';
    this.etiquetasService.crear({ nombre, color: this.colorPara(nombre) }).subscribe({
      next: (etiqueta) => {
        this.creando = false;
        this.busqueda = '';
        this.abierto = false;
        this.creacionRapida = false;
        this.indiceActivo = -1;
        this.etiquetaCreada.emit(etiqueta);
        if (!this.alcanzoMaximo) {
          this.seleccionadasChange.emit([...this.seleccionadas, etiqueta]);
        }
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.creando = false;
        this.errorCreacion = this.mensajeError(err, 'No se pudo crear la etiqueta.');
        this.cdr.markForCheck();
      }
    });
  }

  colorEtiqueta(etiqueta: EtiquetaResponse): string {
    return etiqueta.color || '#2563EB';
  }

  colorConAlfa(etiqueta: EtiquetaResponse, alfa: string): string {
    return `${this.colorEtiqueta(etiqueta)}${alfa}`;
  }

  private colorPara(nombre: string): string {
    let suma = 0;
    for (let i = 0; i < nombre.length; i++) {
      suma += nombre.charCodeAt(i);
    }
    return this.paleta[suma % this.paleta.length];
  }

  private normalizar(valor: string): string {
    return (valor || '')
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .trim()
      .replace(/\s+/g, ' ')
      .toLowerCase();
  }

  private mensajeError(err: any, fallback: string): string {
    return err?.error?.message || err?.error?.error || fallback;
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!this.elementRef.nativeElement.contains(event.target as Node)) {
      this.abierto = false;
      this.creacionRapida = false;
      this.cdr.markForCheck();
    }
  }
}
