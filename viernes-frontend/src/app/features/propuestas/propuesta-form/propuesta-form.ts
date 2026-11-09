import { ChangeDetectorRef, Component, DestroyRef, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormArray, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Observable } from 'rxjs';
import { debounceTime } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DraftService } from '../../../core/services/draft.service';
import { PropuestasService } from '../propuestas.service';
import { EstadoPropuesta, PropuestaRequest, PropuestaResponse } from '../propuestas.model';
import { ProductosService } from '../../productos/productos.service';
import { ProductoResponse } from '../../productos/producto.model';
import { OportunidadesService } from '../../oportunidades/oportunidades.service';
import { OportunidadResponse } from '../../oportunidades/oportunidades.model';
import { ClientesService } from '../../clientes/clientes.service';
import { ClienteResponse } from '../../clientes/clientes.model';

export interface PropuestaDialogData {
  id?: number | null;
}

export interface PropuestaDialogResult {
  saved: boolean;
}

@Component({
  selector: 'app-propuesta-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './propuesta-form.html',
  styleUrls: ['./propuesta-form.css', '../../../shared/styles/entity-dialog.css']
})
export class PropuestaForm implements OnInit {
  private fb = inject(FormBuilder);
  private propuestasService = inject(PropuestasService);
  private productosService = inject(ProductosService);
  private oportunidadesService = inject(OportunidadesService);
  private clientesService = inject(ClientesService);
  private cdr = inject(ChangeDetectorRef);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private dialogRef = inject<MatDialogRef<PropuestaForm, PropuestaDialogResult> | null>(MatDialogRef, {
    optional: true,
  });
  private dialogData = inject<PropuestaDialogData | null>(MAT_DIALOG_DATA, { optional: true });
  private draft = inject(DraftService);
  private destroyRef = inject(DestroyRef);

  readonly esDialogo = this.dialogRef !== null;

  form: FormGroup;
  isEdit = false;
  id!: number;
  loading = false;
  cargandoPropuesta = false;
  estadoActual: EstadoPropuesta = EstadoPropuesta.BORRADOR;
  productos: ProductoResponse[] = [];
  oportunidades: OportunidadResponse[] = [];
  clientes: ClienteResponse[] = [];
  serverError = '';
  validationError = '';

  private submitted = false;
  private cambiosEstado = false;

  constructor() {
    this.form = this.fb.group({
      idOportunidad: [null, [Validators.required, Validators.min(1)]],
      idCliente: [null, [Validators.required, Validators.min(1)]],
      clienteNombre: [''],
      responsable: [''],
      fechaEmision: ['', Validators.required],
      fechaVencimiento: [''],
      descuento: [0, Validators.min(0)],
      observaciones: [''],
      detalles: this.fb.array([]),
    });
    this.agregarDetalle();
  }

  get detalles(): FormArray {
    return this.form.controls['detalles'] as FormArray;
  }

  get esBorrador(): boolean {
    return this.estadoActual === EstadoPropuesta.BORRADOR;
  }

  get subtotalEstimado(): number {
    return this.detalles.controls.reduce((acc, detalle) => {
      const cantidad = Number(detalle.get('cantidad')?.value) || 0;
      const precio = Number(detalle.get('precioUnitario')?.value) || 0;
      return acc + cantidad * precio;
    }, 0);
  }

  get descuentoActual(): number {
    return Number(this.form.get('descuento')?.value) || 0;
  }

  get totalEstimado(): number {
    return this.subtotalEstimado - this.descuentoActual;
  }

  get titulo(): string {
    return this.isEdit ? 'Editar propuesta' : 'Nueva propuesta';
  }

  get subtitulo(): string {
    if (!this.isEdit) return 'Crea una cotización y agrega los productos o servicios ofrecidos.';
    if (!this.esBorrador) return 'Consulta la propuesta y gestiona su flujo de aprobación.';
    return 'Actualiza los datos, los detalles y el estado de la propuesta.';
  }

  get estadoClass(): string {
    switch (this.estadoActual) {
      case EstadoPropuesta.BORRADOR: return 'status-borrador';
      case EstadoPropuesta.ENVIADA: return 'status-enviada';
      case EstadoPropuesta.APROBADA: return 'status-aprobada';
      case EstadoPropuesta.RECHAZADA: return 'status-rechazada';
      case EstadoPropuesta.CANCELADA: return 'status-cancelada';
      default: return '';
    }
  }

  get draftKey(): string {
    return `propuesta:${this.isEdit ? this.id : 'nuevo'}`;
  }

  ngOnInit() {
    this.cargarCatalogos();

    const id = this.dialogData?.id ?? this.idDeRuta();
    if (id) {
      this.isEdit = true;
      this.id = id;
      this.cargarPropuesta();
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
        // rebuild detalles array if present
        if (Array.isArray(borrador.detalles)) {
          while (this.detalles.length !== 0) this.detalles.removeAt(0);
          borrador.detalles.forEach((d: any) => {
            this.detalles.push(
              this.fb.group({
                productoId: [d.productoId ?? null],
                descripcion: [d.descripcion, Validators.required],
                cantidad: [d.cantidad, [Validators.required, Validators.min(1)]],
                precioUnitario: [d.precioUnitario, [Validators.required, Validators.min(0)]],
              })
            );
          });
        }
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

  private cargarCatalogos() {
    this.productosService.listar().subscribe({
      next: (productos) => {
        this.productos = productos || [];
        this.cdr.markForCheck();
      },
      error: () => {
        this.productos = [];
        this.cdr.markForCheck();
      },
    });

    this.oportunidadesService.listar().subscribe({
      next: (oportunidades) => {
        this.oportunidades = oportunidades || [];
        this.cdr.markForCheck();
      },
      error: () => {
        this.oportunidades = [];
        this.cdr.markForCheck();
      },
    });

    this.clientesService.listar().subscribe({
      next: (clientes) => {
        this.clientes = clientes || [];
        this.cdr.markForCheck();
      },
      error: () => {
        this.clientes = [];
        this.cdr.markForCheck();
      },
    });
  }

  onOportunidadChange() {
    const opId = Number(this.form.get('idOportunidad')?.value);
    if (!opId) return;

    const op = this.oportunidades.find(o => o.id === opId);
    if (op) {
      if (op.clienteNombre) {
        const cli = this.clientes.find(c =>
          (c.empresa && c.empresa.toLowerCase() === op.clienteNombre.toLowerCase()) ||
          (c.nombre && c.nombre.toLowerCase() === op.clienteNombre.toLowerCase())
        );
        if (cli) {
          const display = cli.empresa ? `${cli.empresa} (${cli.nombre})` : cli.nombre;
          this.form.patchValue({ idCliente: cli.id, clienteNombre: display });
        } else {
          this.form.patchValue({ clienteNombre: op.clienteNombre });
        }
      }

      if (op.responsable && !this.form.get('responsable')?.value) {
        this.form.patchValue({ responsable: op.responsable });
      }

      if (this.detalles.length === 1) {
        const primer = this.detalles.at(0);
        if (!primer.get('descripcion')?.value) {
          primer.patchValue({
            descripcion: `Propuesta para: ${op.nombre}`,
            precioUnitario: op.valor || 0
          });
        }
      }
      this.cdr.markForCheck();
    }
  }

  onClienteChange() {
    const cId = Number(this.form.get('idCliente')?.value);
    if (!cId) return;

    const cli = this.clientes.find(c => c.id === cId);
    if (cli) {
      const display = cli.empresa ? `${cli.empresa} (${cli.nombre})` : cli.nombre;
      this.form.patchValue({ clienteNombre: display });
      if (cli.responsable && !this.form.get('responsable')?.value) {
        this.form.patchValue({ responsable: cli.responsable });
      }
      this.cdr.markForCheck();
    }
  }

  cargarPropuesta() {
    this.cargandoPropuesta = true;

    this.propuestasService.obtener(this.id).subscribe({
      next: (propuesta) => {
        this.estadoActual = propuesta.estado;

        while (this.detalles.length !== 0) {
          this.detalles.removeAt(0);
        }

        if (propuesta.detalles && propuesta.detalles.length > 0) {
          propuesta.detalles.forEach((detalle) => {
            this.detalles.push(
              this.fb.group({
                productoId: [detalle.productoId ?? null],
                descripcion: [detalle.descripcion, Validators.required],
                cantidad: [detalle.cantidad, [Validators.required, Validators.min(1)]],
                precioUnitario: [detalle.precioUnitario, [Validators.required, Validators.min(0)]],
              })
            );
          });
        }

        this.form.patchValue(propuesta);

        if (this.estadoActual !== EstadoPropuesta.BORRADOR) {
          this.form.disable();
        }

        this.cargandoPropuesta = false;
        this.cdr.markForCheck();
        const borrador = this.draft.load(this.draftKey);
        if (borrador) {
          const restaurar = confirm('Se encontró un borrador sin guardar. ¿Deseas restaurarlo?');
          if (restaurar) {
            if (Array.isArray(borrador.detalles)) {
              while (this.detalles.length !== 0) this.detalles.removeAt(0);
              borrador.detalles.forEach((d: any) => {
                this.detalles.push(
                  this.fb.group({
                    productoId: [d.productoId ?? null],
                    descripcion: [d.descripcion, Validators.required],
                    cantidad: [d.cantidad, [Validators.required, Validators.min(1)]],
                    precioUnitario: [d.precioUnitario, [Validators.required, Validators.min(0)]],
                  })
                );
              });
            }
            this.form.patchValue(borrador);
            this.form.markAsDirty();
          } else {
            this.draft.clear(this.draftKey);
          }
        }
      },
      error: (err) => {
        this.cargandoPropuesta = false;
        this.cdr.markForCheck();
        alert(err?.friendlyMessage || 'Error al cargar la propuesta');
        this.cerrar(false);
      },
    });
  }

  agregarDetalle() {
    if (!this.esBorrador) return;

    this.detalles.push(
      this.fb.group({
        productoId: [null],
        descripcion: ['', Validators.required],
        cantidad: [1, [Validators.required, Validators.min(1)]],
        precioUnitario: [0, [Validators.required, Validators.min(0)]],
      })
    );
    this.cdr.markForCheck();
  }

  onProductoChange(index: number) {
    const detalle = this.detalles.at(index);
    const rawProductoId = detalle.get('productoId')?.value;

    if (rawProductoId === null || rawProductoId === undefined || rawProductoId === '') {
      detalle.patchValue({ productoId: null });
      return;
    }

    const productoId = Number(rawProductoId);
    const producto = this.productos.find((p) => p.id === productoId);

    if (producto) {
      detalle.patchValue({
        productoId,
        descripcion: producto.nombre,
        precioUnitario: producto.precioBase,
      });
    }
  }

  eliminarDetalle(index: number) {
    if (this.detalles.length > 1) {
      this.detalles.removeAt(index);
      this.cdr.markForCheck();
    } else {
      alert('Debe haber al menos un detalle');
    }
  }

  guardar() {
    this.submitted = true;
    this.serverError = '';
    this.validationError = '';

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.validationError = 'Revisa los campos marcados antes de guardar la propuesta.';
      this.cdr.markForCheck();
      return;
    }

    this.loading = true;
    const request = this.form.getRawValue() as PropuestaRequest;

    const req$ = this.isEdit
      ? this.propuestasService.actualizar(this.id, request)
      : this.propuestasService.crear(request);

    req$.subscribe({
      next: () => {
        this.draft.clear(this.draftKey);
        this.cerrar(true);
      },
      error: (err) => {
        this.serverError = err?.friendlyMessage || 'Error al guardar la propuesta.';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  cambiarEstado(accion: 'enviar' | 'aprobar' | 'rechazar' | 'cancelar') {
    if (!this.id || this.loading) return;

    this.loading = true;
    this.serverError = '';
    this.validationError = '';

    let req$: Observable<PropuestaResponse>;
    switch (accion) {
      case 'enviar':
        req$ = this.propuestasService.enviar(this.id);
        break;
      case 'aprobar':
        req$ = this.propuestasService.aprobar(this.id);
        break;
      case 'rechazar':
        req$ = this.propuestasService.rechazar(this.id);
        break;
      case 'cancelar':
        req$ = this.propuestasService.cancelar(this.id);
        break;
      default:
        return;
    }

    req$.subscribe({
      next: () => {
        this.loading = false;
        this.cambiosEstado = true;
        this.cargarPropuesta();
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.serverError = err?.friendlyMessage || 'Error al cambiar el estado de la propuesta.';
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
    if (errors['email']) return 'Ingresa un correo electrónico válido.';
    return 'Revisa este campo.';
  }

  mostrarErrorDetalle(index: number, campo: string): boolean {
    const control = this.detalles.at(index)?.get(campo);
    return !!control && control.invalid && (control.touched || this.submitted);
  }

  mensajeErrorDetalle(index: number, campo: string): string {
    const errors = this.detalles.at(index)?.get(campo)?.errors;
    if (!errors) return '';

    if (errors['required']) return 'Obligatorio.';
    if (errors['min']) return `Mínimo ${errors['min'].min}.`;
    return 'Revisa este campo.';
  }

  formatearMonto(valor: number): string {
    return new Intl.NumberFormat('es-PE', {
      style: 'currency',
      currency: 'PEN',
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(valor || 0);
  }

  cerrar(saved = false) {
    if (this.dialogRef) {
      this.dialogRef.close({ saved: saved || this.cambiosEstado });
      return;
    }
    this.router.navigate(['/propuestas']);
  }
}
