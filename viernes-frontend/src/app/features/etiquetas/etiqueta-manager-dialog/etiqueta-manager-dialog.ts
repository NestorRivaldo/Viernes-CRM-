import { ChangeDetectorRef, Component, DestroyRef, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { debounceTime } from 'rxjs';
import { DraftService } from '../../../core/services/draft.service';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { EtiquetaRequest, EtiquetaResponse } from '../etiquetas.model';
import { EtiquetasService } from '../etiquetas.service';

@Component({
  selector: 'app-etiqueta-manager-dialog',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatDialogModule, MatIconModule],
  templateUrl: './etiqueta-manager-dialog.html',
  styleUrl: './etiqueta-manager-dialog.css'
})
export class EtiquetaManagerDialog implements OnInit {
  private readonly dialogRef = inject(MatDialogRef<EtiquetaManagerDialog>);
  private readonly etiquetasService = inject(EtiquetasService);
  private readonly fb = inject(FormBuilder);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly draft = inject(DraftService);
  private readonly destroyRef = inject(DestroyRef);

  etiquetas: EtiquetaResponse[] = [];
  cargando = false;
  guardando = false;
  editandoId: number | null = null;
  error = '';
  mensaje = '';

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

  form = this.fb.group({
    nombre: ['', [Validators.required, Validators.maxLength(40)]],
    color: ['#2563EB', [Validators.required, Validators.pattern(/^#[0-9A-Fa-f]{6}$/)]],
    descripcion: ['', [Validators.maxLength(160)]],
    activa: [true]
  });

  ngOnInit(): void {
    this.cargar();
    this.form.valueChanges
      .pipe(debounceTime(600), takeUntilDestroyed(this.destroyRef))
      .subscribe((v) => {
        if (this.form.dirty) this.draft.save('etiqueta:nueva', v);
      });
  }

  cargar(): void {
    this.cargando = true;
    this.cdr.markForCheck();
    this.etiquetasService.listar().subscribe({
      next: (etiquetas) => {
        this.etiquetas = etiquetas || [];
        this.cargando = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = this.mensajeError(err, 'No se pudieron cargar las etiquetas.');
        this.cargando = false;
        this.cdr.markForCheck();
      }
    });
  }

  editar(etiqueta: EtiquetaResponse): void {
    this.editandoId = etiqueta.id;
    this.form.patchValue({
      nombre: etiqueta.nombre,
      color: etiqueta.color || '#2563EB',
      descripcion: etiqueta.descripcion || '',
      activa: etiqueta.activa
    });
    this.error = '';
    this.mensaje = '';
    this.cdr.markForCheck();
  }

  cancelarEdicion(): void {
    this.editandoId = null;
    this.form.reset({
      nombre: '',
      color: this.siguienteColor(),
      descripcion: '',
      activa: true
    });
    this.error = '';
    this.cdr.markForCheck();
  }

  guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const valores = this.form.getRawValue();
    const request: EtiquetaRequest = {
      nombre: (valores.nombre || '').trim(),
      color: valores.color || '#2563EB',
      descripcion: (valores.descripcion || '').trim() || null,
      activa: valores.activa ?? true
    };

    this.guardando = true;
    this.error = '';
    this.mensaje = '';

    const peticion$ = this.editandoId
      ? this.etiquetasService.actualizar(this.editandoId, request)
      : this.etiquetasService.crear(request);

    peticion$.subscribe({
      next: () => {
        this.guardando = false;
        this.draft.clear('etiqueta:nueva');
        this.mensaje = this.editandoId ? 'Etiqueta actualizada.' : 'Etiqueta creada.';
        this.editandoId = null;
        this.form.reset({
          nombre: '',
          color: this.siguienteColor(),
          descripcion: '',
          activa: true
        });
        this.cargar();
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.guardando = false;
        this.error = this.mensajeError(err, 'No se pudo guardar la etiqueta.');
        this.cdr.markForCheck();
      }
    });
  }

  archivar(etiqueta: EtiquetaResponse): void {
    if (!window.confirm(`¿Archivar la etiqueta “${etiqueta.nombre}”? Las actividades que ya la usan conservarán su etiqueta.`)) {
      return;
    }
    this.etiquetasService.eliminar(etiqueta.id).subscribe({
      next: () => {
        this.mensaje = 'Etiqueta archivada.';
        this.cargar();
      },
      error: (err) => {
        this.error = this.mensajeError(err, 'No se pudo archivar la etiqueta.');
        this.cdr.markForCheck();
      }
    });
  }

  reactivar(etiqueta: EtiquetaResponse): void {
    this.etiquetasService.actualizar(etiqueta.id, {
      nombre: etiqueta.nombre,
      color: etiqueta.color,
      descripcion: etiqueta.descripcion || null,
      activa: true
    }).subscribe({
      next: () => {
        this.mensaje = 'Etiqueta reactivada.';
        this.cargar();
      },
      error: (err) => {
        this.error = this.mensajeError(err, 'No se pudo reactivar la etiqueta.');
        this.cdr.markForCheck();
      }
    });
  }

  cerrar(): void {
    this.dialogRef.close();
  }

  colorEtiqueta(etiqueta: EtiquetaResponse): string {
    return etiqueta.color || '#2563EB';
  }

  private siguienteColor(): string {
    return this.paleta[this.etiquetas.length % this.paleta.length];
  }

  private mensajeError(err: any, fallback: string): string {
    return err?.error?.message || err?.error?.error || fallback;
  }
}
