import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { forkJoin } from 'rxjs';
import { debounceTime } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DraftService } from '../../../core/services/draft.service';
import { OportunidadesService } from '../oportunidades.service';
import {
  EtapaOportunidad,
  OportunidadColaboradorResponse,
  OportunidadContactoResponse,
  OportunidadProductoResponse,
  OportunidadRequest,
  OportunidadResponse,
} from '../oportunidades.model';
import { ClientesService } from '../../clientes/clientes.service';
import { ClienteResponse, ContactoResponse } from '../../clientes/clientes.model';
import { ProductosService } from '../../productos/productos.service';
import { ProductoResponse } from '../../productos/producto.model';
import { UsuariosService } from '../../usuarios/usuarios.service';
import { UsuarioResponse } from '../../usuarios/usuarios.model';

export interface OportunidadDialogData {
  id?: number | null;
}

export interface OportunidadDialogResult {
  saved: boolean;
  id?: number;
}

type TabId = 'general' | 'contactos' | 'productos' | 'colaboradores';

@Component({
  selector: 'app-oportunidad-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './oportunidad-dialog.html',
  styleUrl: './oportunidad-dialog.css',
})
export class OportunidadDialog implements OnInit {
  private fb = inject(FormBuilder);
  private dialogRef = inject(MatDialogRef<OportunidadDialog, OportunidadDialogResult>);
  private oportunidadesService = inject(OportunidadesService);
  private clientesService = inject(ClientesService);
  private productosService = inject(ProductosService);
  private usuariosService = inject(UsuariosService);
  private draft = inject(DraftService);
  private destroyRef = inject(DestroyRef);

  readonly data = inject<OportunidadDialogData>(MAT_DIALOG_DATA);

  readonly id = signal<number | null>(this.data?.id ?? null);
  readonly esEdicion = computed(() => this.id() !== null);
  readonly draftKey = computed(() => `oportunidad:${this.id() ?? 'nuevo'}`);
  readonly tabActiva = signal<TabId>('general');

  readonly etapas = Object.values(EtapaOportunidad);
  readonly tabs: { id: TabId; label: string }[] = [
    { id: 'general', label: 'Información general' },
    { id: 'contactos', label: 'Contactos' },
    { id: 'productos', label: 'Productos' },
    { id: 'colaboradores', label: 'Colaboradores' },
  ];

  readonly cargando = signal(false);
  readonly guardando = signal(false);
  readonly error = signal('');

  readonly oportunidad = signal<OportunidadResponse | null>(null);

  readonly clientes = signal<ClienteResponse[]>([]);
  readonly usuarios = signal<UsuarioResponse[]>([]);
  readonly catalogoProductos = signal<ProductoResponse[]>([]);

  readonly contactosCliente = signal<ContactoResponse[]>([]);
  readonly cargandoContactosCliente = signal(false);

  readonly contactos = signal<OportunidadContactoResponse[]>([]);
  readonly productos = signal<OportunidadProductoResponse[]>([]);
  readonly colaboradores = signal<OportunidadColaboradorResponse[]>([]);
  readonly cargandoRelaciones = signal(false);
  readonly errorRelaciones = signal('');

  readonly guardandoContacto = signal(false);
  readonly guardandoProducto = signal(false);
  readonly guardandoColaborador = signal(false);

  readonly form = this.fb.group({
    nombre: ['', [Validators.required, Validators.maxLength(200)]],
    clienteNombre: ['', [Validators.required, Validators.maxLength(150)]],
    valor: [0, [Validators.required, Validators.min(0)]],
    probabilidad: [0, [Validators.required, Validators.min(0), Validators.max(100)]],
    etapa: [EtapaOportunidad.PROSPECCION, Validators.required],
    responsable: ['', [Validators.required, Validators.maxLength(100)]],
    fechaCierre: ['', Validators.required],
    descripcion: ['', Validators.maxLength(500)],
  });

  readonly contactoForm = this.fb.group({
    clienteId: ['', Validators.required],
    contactoId: [{ value: '', disabled: true }, Validators.required],
    rol: [''],
    esDecisor: [false],
  });

  readonly productoForm = this.fb.group({
    productoId: ['', Validators.required],
    cantidad: [1, [Validators.required, Validators.min(1)]],
  });

  readonly colaboradorForm = this.fb.group({
    usuarioId: ['', Validators.required],
  });

  readonly contactosDisponibles = computed(() => {
    const vinculados = new Set(this.contactos().map((c) => c.contactoId));
    return this.contactosCliente().filter((c) => !vinculados.has(c.id));
  });

  readonly productosDisponibles = computed(() => {
    const vinculados = new Set(this.productos().map((p) => p.productoId));
    return this.catalogoProductos().filter((p) => !vinculados.has(p.id));
  });

  readonly usuariosDisponibles = computed(() => {
    const vinculados = new Set(this.colaboradores().map((c) => c.usuarioId));
    return this.usuarios().filter((u) => !vinculados.has(u.id));
  });

  readonly totalProductos = computed(() =>
    this.productos().reduce((acc, p) => acc + Number(p.precioBase || 0) * Number(p.cantidad || 0), 0)
  );

  ngOnInit() {
    this.cargarCatalogos();
    if (this.esEdicion()) {
      this.cargarOportunidad();
      this.cargarRelaciones();
    } else {
      this.restaurarBorrador();
    }
    if (this.dialogRef) {
      this.dialogRef.backdropClick().subscribe(() => {
        if (this.form.dirty) this.draft.save(this.draftKey(), this.form.getRawValue());
      });
    }
    this.form.valueChanges
      .pipe(debounceTime(600), takeUntilDestroyed(this.destroyRef))
      .subscribe((v) => {
        if (this.form.dirty) this.draft.save(this.draftKey(), v);
      });
  }

  private restaurarBorrador() {
    const borrador = this.draft.load(this.draftKey());
    if (borrador) {
      const restaurar = confirm('Se encontró un borrador sin guardar. ¿Deseas restaurarlo?');
      if (restaurar) {
        this.form.patchValue(borrador);
        this.form.markAsDirty();
      } else {
        this.draft.clear(this.draftKey());
      }
    }
  }

  private cargarCatalogos() {
    this.clientesService.listar().subscribe({
      next: (data) => this.clientes.set(data || []),
      error: () => this.clientes.set([]),
    });
    this.usuariosService.listar().subscribe({
      next: (data) => this.usuarios.set(data || []),
      error: () => this.usuarios.set([]),
    });
    this.productosService.listar().subscribe({
      next: (data) => this.catalogoProductos.set(data || []),
      error: () => this.catalogoProductos.set([]),
    });
  }

  private cargarOportunidad() {
    const id = this.id();
    if (!id) return;

    this.cargando.set(true);
    this.error.set('');
    this.oportunidadesService.obtener(id).subscribe({
      next: (op) => {
        this.oportunidad.set(op);
        this.form.patchValue({
          nombre: op.nombre || '',
          clienteNombre: op.clienteNombre || '',
          valor: Number(op.valor || 0),
          probabilidad: Number(op.probabilidad || 0),
          etapa: op.etapa || EtapaOportunidad.PROSPECCION,
          responsable: op.responsable || '',
          fechaCierre: op.fechaCierre || '',
          descripcion: op.descripcion || '',
        });
        this.cargando.set(false);
        const borrador = this.draft.load(this.draftKey());
        if (borrador) {
          const restaurar = confirm('Se encontró un borrador sin guardar. ¿Deseas restaurarlo?');
          if (restaurar) {
            this.form.patchValue(borrador);
            this.form.markAsDirty();
          } else {
            this.draft.clear(this.draftKey());
          }
        }
      },
      error: (err) => {
        this.error.set(err?.friendlyMessage || 'No se pudo cargar la oportunidad.');
        this.cargando.set(false);
      },
    });
  }

  private cargarRelaciones() {
    const id = this.id();
    if (!id) return;

    this.cargandoRelaciones.set(true);
    this.errorRelaciones.set('');
    forkJoin({
      contactos: this.oportunidadesService.listarContactos(id),
      productos: this.oportunidadesService.listarProductos(id),
      colaboradores: this.oportunidadesService.listarColaboradores(id),
    }).subscribe({
      next: ({ contactos, productos, colaboradores }) => {
        this.contactos.set(contactos || []);
        this.productos.set(productos || []);
        this.colaboradores.set(colaboradores || []);
        this.cargandoRelaciones.set(false);
      },
      error: (err) => {
        this.errorRelaciones.set(err?.friendlyMessage || 'No se pudieron cargar las relaciones.');
        this.cargandoRelaciones.set(false);
      },
    });
  }

  guardar() {
    if (this.guardando()) return;

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.tabActiva.set('general');
      this.error.set('Completa los campos obligatorios para guardar.');
      return;
    }

    const v = this.form.getRawValue();
    const request: OportunidadRequest = {
      nombre: (v.nombre || '').trim(),
      clienteNombre: (v.clienteNombre || '').trim() || undefined,
      valor: Number(v.valor) || 0,
      probabilidad: Number(v.probabilidad) || 0,
      etapa: v.etapa || EtapaOportunidad.PROSPECCION,
      responsable: (v.responsable || '').trim() || undefined,
      fechaCierre: v.fechaCierre || undefined,
      descripcion: (v.descripcion || '').trim() || undefined,
    };

    this.guardando.set(true);
    this.error.set('');

    const req$ = this.esEdicion()
      ? this.oportunidadesService.actualizar(this.id()!, request)
      : this.oportunidadesService.crear(request);

    req$.subscribe({
      next: (op) => {
        this.guardando.set(false);
        this.draft.clear(this.draftKey());
        this.dialogRef.close({ saved: true, id: op.id });
      },
      error: (err) => {
        this.guardando.set(false);
        this.error.set(err?.friendlyMessage || 'No se pudo guardar la oportunidad.');
      },
    });
  }

  cerrar() {
    if (this.guardando()) return;
    this.dialogRef.close({ saved: false });
  }

  abrirCalendario(input: HTMLInputElement) {
    if (typeof input.showPicker === 'function') {
      input.showPicker();
    } else {
      input.focus();
    }
  }

  limpiarFecha() {
    this.form.controls.fechaCierre.setValue('');
  }

  /* ----- Contactos ----- */

  onClienteChange() {
    const clienteId = Number(this.contactoForm.controls.clienteId.value) || null;
    const contactoCtrl = this.contactoForm.controls.contactoId;

    contactoCtrl.reset('');
    this.contactosCliente.set([]);

    if (!clienteId) {
      contactoCtrl.disable();
      return;
    }

    contactoCtrl.enable();
    this.cargandoContactosCliente.set(true);

    this.clientesService.listarContactos(clienteId).subscribe({
      next: (data) => {
        this.contactosCliente.set(data || []);
        this.cargandoContactosCliente.set(false);
      },
      error: (err) => {
        this.cargandoContactosCliente.set(false);
        this.errorRelaciones.set(err?.friendlyMessage || 'No se pudieron cargar los contactos del cliente.');
      },
    });
  }

  agregarContacto() {
    const id = this.id();
    const v = this.contactoForm.getRawValue();
    if (!id || !v.contactoId) {
      this.contactoForm.markAllAsTouched();
      return;
    }

    this.guardandoContacto.set(true);
    this.errorRelaciones.set('');

    this.oportunidadesService
      .agregarContacto(id, {
        contactoId: Number(v.contactoId),
        rol: v.rol?.trim() || null,
        esDecisor: !!v.esDecisor,
      })
      .subscribe({
        next: (nuevo) => {
          this.contactos.update((list) => [...list, nuevo]);
          this.contactoForm.reset({
            clienteId: v.clienteId,
            contactoId: '',
            rol: '',
            esDecisor: false,
          });
          this.contactoForm.controls.contactoId.disable();
          this.guardandoContacto.set(false);
        },
        error: (err) => {
          this.errorRelaciones.set(err?.friendlyMessage || 'No se pudo vincular el contacto.');
          this.guardandoContacto.set(false);
        },
      });
  }

  quitarContacto(contacto: OportunidadContactoResponse) {
    const id = this.id();
    if (!id) return;

    this.oportunidadesService.quitarContacto(id, contacto.contactoId).subscribe({
      next: () => this.contactos.update((list) => list.filter((c) => c.contactoId !== contacto.contactoId)),
      error: (err) => this.errorRelaciones.set(err?.friendlyMessage || 'No se pudo quitar el contacto.'),
    });
  }

  /* ----- Productos ----- */

  agregarProducto() {
    const id = this.id();
    if (!id || this.productoForm.invalid) {
      this.productoForm.markAllAsTouched();
      return;
    }

    const v = this.productoForm.getRawValue();
    this.guardandoProducto.set(true);
    this.errorRelaciones.set('');

    this.oportunidadesService
      .agregarProducto(id, {
        productoId: Number(v.productoId),
        cantidad: Number(v.cantidad) || 1,
      })
      .subscribe({
        next: (nuevo) => {
          this.productos.update((list) => [...list, nuevo]);
          this.productoForm.reset({ productoId: '', cantidad: 1 });
          this.guardandoProducto.set(false);
        },
        error: (err) => {
          this.errorRelaciones.set(err?.friendlyMessage || 'No se pudo vincular el producto.');
          this.guardandoProducto.set(false);
        },
      });
  }

  quitarProducto(producto: OportunidadProductoResponse) {
    const id = this.id();
    if (!id) return;

    this.oportunidadesService.quitarProducto(id, producto.productoId).subscribe({
      next: () => this.productos.update((list) => list.filter((p) => p.productoId !== producto.productoId)),
      error: (err) => this.errorRelaciones.set(err?.friendlyMessage || 'No se pudo quitar el producto.'),
    });
  }

  /* ----- Colaboradores ----- */

  agregarColaborador() {
    const id = this.id();
    if (!id || this.colaboradorForm.invalid) {
      this.colaboradorForm.markAllAsTouched();
      return;
    }

    const v = this.colaboradorForm.getRawValue();
    this.guardandoColaborador.set(true);
    this.errorRelaciones.set('');

    this.oportunidadesService
      .agregarColaborador(id, { usuarioId: Number(v.usuarioId) })
      .subscribe({
        next: (nuevo) => {
          this.colaboradores.update((list) => [...list, nuevo]);
          this.colaboradorForm.reset({ usuarioId: '' });
          this.guardandoColaborador.set(false);
        },
        error: (err) => {
          this.errorRelaciones.set(err?.friendlyMessage || 'No se pudo vincular el colaborador.');
          this.guardandoColaborador.set(false);
        },
      });
  }

  quitarColaborador(colaborador: OportunidadColaboradorResponse) {
    const id = this.id();
    if (!id) return;

    this.oportunidadesService.quitarColaborador(id, colaborador.usuarioId).subscribe({
      next: () =>
        this.colaboradores.update((list) => list.filter((c) => c.usuarioId !== colaborador.usuarioId)),
      error: (err) => this.errorRelaciones.set(err?.friendlyMessage || 'No se pudo quitar el colaborador.'),
    });
  }

  /* ----- Helpers ----- */

  contador(tab: TabId): number {
    switch (tab) {
      case 'contactos':
        return this.contactos().length;
      case 'productos':
        return this.productos().length;
      case 'colaboradores':
        return this.colaboradores().length;
      default:
        return 0;
    }
  }

  puedeAgregarContacto(): boolean {
    return !this.guardandoContacto() && !!this.contactoForm.getRawValue().contactoId;
  }

  usuarioSeleccionado(): UsuarioResponse | undefined {
    const nombre = this.form.controls.responsable.value;
    return this.usuarios().find((u) => this.nombreCompleto(u) === nombre);
  }

  nombreCompleto(usuario: UsuarioResponse): string {
    return `${usuario.nombre || ''} ${usuario.apellido || ''}`.trim();
  }

  iniciales(nombre: string | null | undefined): string {
    return (nombre || '')
      .split(' ')
      .filter(Boolean)
      .slice(0, 2)
      .map((p) => p[0]?.toUpperCase())
      .join('');
  }

  etiquetaEtapa(etapa: EtapaOportunidad | string): string {
    const labels: Record<string, string> = {
      PROSPECCION: 'Prospección',
      CONTACTO: 'Contacto',
      CALIFICACION: 'Calificación',
      PROPUESTA: 'Propuesta',
      NEGOCIACION: 'Negociación',
      GANADA: 'Ganada',
      PERDIDA: 'Perdida',
    };
    return labels[etapa] || etapa;
  }

  mostrarError(campo: string): boolean {
    const control = this.form.get(campo);
    return !!control && control.invalid && (control.touched || control.dirty);
  }

  mensajeError(campo: string): string {
    const control = this.form.get(campo);
    if (!control) return '';
    if (control.hasError('required')) return 'Este campo es obligatorio.';
    if (control.hasError('maxlength')) {
      return `Máximo ${control.getError('maxlength').requiredLength} caracteres.`;
    }
    if (control.hasError('min') || control.hasError('max')) return 'Valor fuera del rango permitido.';
    return 'Valor inválido.';
  }

  formatearMoneda(valor: number | null | undefined): string {
    return new Intl.NumberFormat('es-PE', {
      style: 'currency',
      currency: 'PEN',
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(Number(valor || 0));
  }
}
