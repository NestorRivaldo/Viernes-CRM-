import { ChangeDetectorRef, Component, DestroyRef, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { debounceTime } from 'rxjs';
import { DraftService } from '../../../core/services/draft.service';
import { ActivatedRoute, Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ProductosService } from '../productos.service';
import { EstadoProducto, ProductoRequest } from '../producto.model';

export interface ProductoDialogData {
  id?: number | null;
}

export interface ProductoDialogResult {
  saved: boolean;
}

@Component({
  selector: 'app-producto-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './producto-form.html',
  styleUrls: ['./producto-form.css', '../../../shared/styles/entity-dialog.css']
})
export class ProductoForm implements OnInit {
  private fb = inject(FormBuilder);
  private productosService = inject(ProductosService);
  private cdr = inject(ChangeDetectorRef);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private dialogRef = inject<MatDialogRef<ProductoForm, ProductoDialogResult> | null>(MatDialogRef, {
    optional: true,
  });
  private dialogData = inject<ProductoDialogData | null>(MAT_DIALOG_DATA, { optional: true });
  private draft = inject(DraftService);
  private destroyRef = inject(DestroyRef);

  readonly esDialogo = this.dialogRef !== null;
  readonly estados = Object.values(EstadoProducto);

  form: FormGroup;
  isEdit = false;
  id!: number;
  loading = false;
  cargandoProducto = false;
  serverError = '';

  private submitted = false;

  constructor() {
    this.form = this.fb.group({
      codigo: ['', [Validators.required, Validators.maxLength(50)]],
      nombre: ['', [Validators.required, Validators.maxLength(200)]],
      descripcion: ['', Validators.maxLength(1000)],
      categoria: ['', Validators.maxLength(100)],
      precioBase: [null, [Validators.required, Validators.min(0)]],
      estado: [EstadoProducto.ACTIVO, Validators.required],
    });
  }

  get draftKey(): string {
    return `producto:${this.isEdit ? this.id : 'nuevo'}`;
  }

  ngOnInit() {
    const id = this.dialogData?.id ?? this.idDeRuta();

    if (id) {
      this.isEdit = true;
      this.id = id;
      this.cargarProducto();
    } else {
      this.restaurarBorrador();
    }

    if (this.dialogRef) {
      this.dialogRef.backdropClick().subscribe(() => {
        if (this.form.dirty) this.draft.save(this.draftKey, this.form.getRawValue());
      });
    }

    this.form.valueChanges
      .pipe(debounceTime(600), takeUntilDestroyed(this.destroyRef))
      .subscribe((v) => {
        if (this.form.dirty) this.draft.save(this.draftKey, v);
      });
  }

  private restaurarBorrador() {
    const borrador = this.draft.load(this.draftKey);
    if (borrador) {
      const restaurar = confirm('Se encontró un borrador sin guardar. ¿Deseas restaurarlo?');
      if (restaurar) {
        this.form.patchValue(borrador);
        this.form.markAsDirty();
      } else {
        this.draft.clear(this.draftKey);
      }
    }
  }

  private idDeRuta(): number | null {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (!idParam) return null;

    const id = Number(idParam);
    return Number.isFinite(id) && id > 0 ? id : null;
  }

  cargarProducto() {
    this.cargandoProducto = true;

    this.productosService.obtener(this.id).subscribe({
      next: (producto) => {
        this.form.patchValue(producto);
        this.cargandoProducto = false;
        this.cdr.markForCheck();
        const borrador = this.draft.load(this.draftKey);
        if (borrador) {
          const restaurar = confirm('Se encontró un borrador sin guardar. ¿Deseas restaurarlo?');
          if (restaurar) {
            this.form.patchValue(borrador);
            this.form.markAsDirty();
          } else {
            this.draft.clear(this.draftKey);
          }
        }
      },
      error: (err) => {
        this.cargandoProducto = false;
        this.cdr.markForCheck();
        alert(err?.friendlyMessage || 'Error al cargar el producto');
        this.cerrar(false);
      },
    });
  }

  guardar() {
    this.submitted = true;
    this.serverError = '';

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.cdr.markForCheck();
      return;
    }

    this.loading = true;
    const request = this.form.getRawValue() as ProductoRequest;

    const req$ = this.isEdit
      ? this.productosService.actualizar(this.id, request)
      : this.productosService.crear(request);

    req$.subscribe({
      next: () => {
        this.draft.clear(this.draftKey);
        this.cerrar(true);
      },
      error: (err) => {
        this.serverError = err?.friendlyMessage || 'Error al guardar el producto.';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  mostrarError(campo: string): boolean {
    const control = this.form.get(campo);
    return !!control && control.invalid && (control.touched || this.submitted);
  }

  mensajeError(campo: string): string {
    const errors = this.form.get(campo)?.errors;
    if (!errors) return '';

    if (errors['required']) return 'Este campo es obligatorio.';
    if (errors['min']) return `El valor mínimo es ${errors['min'].min}.`;
    if (errors['maxlength']) return `Máximo ${errors['maxlength'].requiredLength} caracteres.`;
    return 'Revisa este campo.';
  }

  cerrar(saved = false) {
    if (this.dialogRef) {
      this.dialogRef.close({ saved });
      return;
    }
    this.router.navigate(['/productos']);
  }
}
