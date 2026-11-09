import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroupDirective, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { OportunidadesService } from '../oportunidades.service';
import {
  EtapaOportunidad,
  OportunidadColaboradorResponse,
  OportunidadContactoResponse,
  OportunidadProductoResponse,
  OportunidadResponse,
} from '../oportunidades.model';
import { ClientesService } from '../../clientes/clientes.service';
import { ClienteResponse, ContactoResponse } from '../../clientes/clientes.model';
import { ProductosService } from '../../productos/productos.service';
import { ProductoResponse } from '../../productos/producto.model';
import { UsuariosService } from '../../usuarios/usuarios.service';
import { UsuarioResponse } from '../../usuarios/usuarios.model';
import {
  OportunidadDialog,
  OportunidadDialogResult,
} from '../oportunidad-dialog/oportunidad-dialog';

@Component({
  selector: 'app-oportunidad-detail',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatCheckboxModule,
    MatDialogModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './oportunidad-detail.html',
  styleUrl: './oportunidad-detail.css'
})
export class OportunidadDetail implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private fb = inject(FormBuilder);
  private oportunidadesService = inject(OportunidadesService);
  private clientesService = inject(ClientesService);
  private productosService = inject(ProductosService);
  private usuariosService = inject(UsuariosService);
  private dialog = inject(MatDialog);

  readonly id = Number(this.route.snapshot.paramMap.get('id'));

  oportunidad = signal<OportunidadResponse | null>(null);
  loading = signal(true);
  error = signal('');

  contactos = signal<OportunidadContactoResponse[]>([]);
  productos = signal<OportunidadProductoResponse[]>([]);
  colaboradores = signal<OportunidadColaboradorResponse[]>([]);
  loadingRelaciones = signal(true);
  errorRelaciones = signal('');

  clientes = signal<ClienteResponse[]>([]);
  contactosCliente = signal<ContactoResponse[]>([]);
  cargandoContactosCliente = signal(false);
  catalogoProductos = signal<ProductoResponse[]>([]);
  usuarios = signal<UsuarioResponse[]>([]);

  guardandoContacto = signal(false);
  guardandoProducto = signal(false);
  guardandoColaborador = signal(false);

  contactoEnEdicion = signal<number | null>(null);

  readonly displayedContactos = ['contacto', 'rol', 'decisor', 'acciones'];
  readonly displayedProductos = ['producto', 'precio', 'cantidad', 'subtotal', 'acciones'];
  readonly displayedColaboradores = ['colaborador', 'acciones'];

  contactoForm = this.fb.group({
    clienteId: [null as number | null, Validators.required],
    contactoId: [null as number | null, Validators.required],
    rol: [''],
    esDecisor: [false]
  });

  contactoEditForm = this.fb.group({
    rol: [''],
    esDecisor: [false]
  });

  productoForm = this.fb.group({
    productoId: [null as number | null, Validators.required],
    cantidad: [1, [Validators.required, Validators.min(1)]]
  });

  colaboradorForm = this.fb.group({
    usuarioId: [null as number | null, Validators.required]
  });

  contactosDisponibles = computed(() => {
    const vinculados = new Set(this.contactos().map((c) => c.contactoId));
    return this.contactosCliente().filter((c) => !vinculados.has(c.id));
  });

  productosDisponibles = computed(() => {
    const vinculados = new Set(this.productos().map((p) => p.productoId));
    return this.catalogoProductos().filter((p) => !vinculados.has(p.id));
  });

  usuariosDisponibles = computed(() => {
    const vinculados = new Set(this.colaboradores().map((c) => c.usuarioId));
    return this.usuarios().filter((u) => !vinculados.has(u.id));
  });

  totalProductos = computed(() =>
    this.productos().reduce((acc, p) => acc + Number(p.precioBase || 0) * Number(p.cantidad || 0), 0)
  );

  ngOnInit() {
    if (!this.id || Number.isNaN(this.id)) {
      this.error.set('Identificador de oportunidad inválido.');
      this.loading.set(false);
      return;
    }
    this.cargarOportunidad();
    this.cargarRelaciones();
    this.cargarCatalogos();
  }

  cargarOportunidad() {
    this.loading.set(true);
    this.error.set('');
    this.oportunidadesService.obtener(this.id).subscribe({
      next: (op) => {
        this.oportunidad.set(op);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err?.friendlyMessage || 'No se pudo cargar la oportunidad.');
        this.loading.set(false);
      }
    });
  }

  cargarRelaciones() {
    this.loadingRelaciones.set(true);
    this.errorRelaciones.set('');
    forkJoin({
      contactos: this.oportunidadesService.listarContactos(this.id),
      productos: this.oportunidadesService.listarProductos(this.id),
      colaboradores: this.oportunidadesService.listarColaboradores(this.id)
    }).subscribe({
      next: ({ contactos, productos, colaboradores }) => {
        this.contactos.set(contactos || []);
        this.productos.set(productos || []);
        this.colaboradores.set(colaboradores || []);
        this.loadingRelaciones.set(false);
      },
      error: (err) => {
        this.errorRelaciones.set(err?.friendlyMessage || 'No se pudieron cargar las relaciones de la oportunidad.');
        this.loadingRelaciones.set(false);
      }
    });
  }

  private cargarCatalogos() {
    this.clientesService.listar().subscribe({
      next: (data) => this.clientes.set(data || []),
      error: () => this.clientes.set([])
    });
    this.productosService.listar().subscribe({
      next: (data) => this.catalogoProductos.set(data || []),
      error: () => this.catalogoProductos.set([])
    });
    this.usuariosService.listar().subscribe({
      next: (data) => this.usuarios.set(data || []),
      error: () => this.usuarios.set([])
    });
  }

  onClienteChange() {
    const clienteId = this.contactoForm.get('clienteId')?.value;
    this.contactoForm.patchValue({ contactoId: null });
    this.contactosCliente.set([]);

    if (!clienteId) return;

    this.cargandoContactosCliente.set(true);
    this.clientesService.listarContactos(Number(clienteId)).subscribe({
      next: (data) => {
        this.contactosCliente.set(data || []);
        this.cargandoContactosCliente.set(false);
      },
      error: (err) => {
        this.cargandoContactosCliente.set(false);
        alert(err?.friendlyMessage || 'No se pudieron cargar los contactos del cliente.');
      }
    });
  }

  agregarContacto(formDir: FormGroupDirective) {
    if (this.contactoForm.invalid) {
      this.contactoForm.markAllAsTouched();
      return;
    }
    const v = this.contactoForm.getRawValue();
    this.guardandoContacto.set(true);

    this.oportunidadesService.agregarContacto(this.id, {
      contactoId: Number(v.contactoId),
      rol: v.rol?.trim() || null,
      esDecisor: !!v.esDecisor
    }).subscribe({
      next: (nuevo) => {
        this.contactos.update((list) => [...list, nuevo]);
        formDir.resetForm({ clienteId: v.clienteId, contactoId: null, rol: '', esDecisor: false });
        this.guardandoContacto.set(false);
      },
      error: (err) => {
        alert(err?.friendlyMessage || 'No se pudo vincular el contacto.');
        this.guardandoContacto.set(false);
      }
    });
  }

  editarContacto(contacto: OportunidadContactoResponse) {
    this.contactoEnEdicion.set(contacto.id);
    this.contactoEditForm.setValue({
      rol: contacto.rol || '',
      esDecisor: !!contacto.esDecisor
    });
  }

  cancelarEdicionContacto() {
    this.contactoEnEdicion.set(null);
  }

  guardarEdicionContacto(contacto: OportunidadContactoResponse) {
    const v = this.contactoEditForm.getRawValue();
    this.oportunidadesService
      .actualizarContacto(this.id, contacto.contactoId, {
        rol: v.rol?.trim() || null,
        esDecisor: !!v.esDecisor
      })
      .subscribe({
        next: (actualizado) => {
          this.contactos.update((list) =>
            list.map((c) => (c.contactoId === contacto.contactoId ? actualizado : c))
          );
          this.contactoEnEdicion.set(null);
        },
        error: (err) => alert(err?.friendlyMessage || 'No se pudo actualizar el contacto.')
      });
  }

  quitarContacto(contacto: OportunidadContactoResponse) {
    if (!confirm(`¿Quitar a ${contacto.nombreContacto || 'este contacto'} de la oportunidad?`)) return;

    this.oportunidadesService.quitarContacto(this.id, contacto.contactoId).subscribe({
      next: () => this.contactos.update((list) => list.filter((c) => c.contactoId !== contacto.contactoId)),
      error: (err) => alert(err?.friendlyMessage || 'No se pudo quitar el contacto.')
    });
  }

  agregarProducto(formDir: FormGroupDirective) {
    if (this.productoForm.invalid) {
      this.productoForm.markAllAsTouched();
      return;
    }
    const v = this.productoForm.getRawValue();
    this.guardandoProducto.set(true);

    this.oportunidadesService.agregarProducto(this.id, {
      productoId: Number(v.productoId),
      cantidad: Number(v.cantidad) || 1
    }).subscribe({
      next: (nuevo) => {
        this.productos.update((list) => [...list, nuevo]);
        formDir.resetForm({ productoId: null, cantidad: 1 });
        this.guardandoProducto.set(false);
      },
      error: (err) => {
        alert(err?.friendlyMessage || 'No se pudo vincular el producto.');
        this.guardandoProducto.set(false);
      }
    });
  }

  quitarProducto(producto: OportunidadProductoResponse) {
    if (!confirm(`¿Quitar ${producto.nombreProducto || 'este producto'} de la oportunidad?`)) return;

    this.oportunidadesService.quitarProducto(this.id, producto.productoId).subscribe({
      next: () => this.productos.update((list) => list.filter((p) => p.productoId !== producto.productoId)),
      error: (err) => alert(err?.friendlyMessage || 'No se pudo quitar el producto.')
    });
  }

  agregarColaborador(formDir: FormGroupDirective) {
    if (this.colaboradorForm.invalid) {
      this.colaboradorForm.markAllAsTouched();
      return;
    }
    const v = this.colaboradorForm.getRawValue();
    this.guardandoColaborador.set(true);

    this.oportunidadesService.agregarColaborador(this.id, {
      usuarioId: Number(v.usuarioId)
    }).subscribe({
      next: (nuevo) => {
        this.colaboradores.update((list) => [...list, nuevo]);
        formDir.resetForm({ usuarioId: null });
        this.guardandoColaborador.set(false);
      },
      error: (err) => {
        alert(err?.friendlyMessage || 'No se pudo vincular el colaborador.');
        this.guardandoColaborador.set(false);
      }
    });
  }

  quitarColaborador(colaborador: OportunidadColaboradorResponse) {
    if (!confirm(`¿Quitar a ${colaborador.nombreCompleto || 'este colaborador'} de la oportunidad?`)) return;

    this.oportunidadesService.quitarColaborador(this.id, colaborador.usuarioId).subscribe({
      next: () =>
        this.colaboradores.update((list) => list.filter((c) => c.usuarioId !== colaborador.usuarioId)),
      error: (err) => alert(err?.friendlyMessage || 'No se pudo quitar el colaborador.')
    });
  }

  verPropuestas() {
    this.router.navigate(['/propuestas'], { queryParams: { idOportunidad: this.id } });
  }

  editarOportunidad() {
    this.dialog
      .open(OportunidadDialog, {
        data: { id: this.id },
        panelClass: 'opp-dialog-panel',
        backdropClass: 'opp-dialog-backdrop',
        width: '780px',
        maxWidth: '95vw',
        disableClose: false,
        autoFocus: false,
      })
      .afterClosed()
      .subscribe((result: OportunidadDialogResult | undefined) => {
        if (result?.saved) {
          this.cargarOportunidad();
          this.cargarRelaciones();
        }
      });
  }

  getEtapaClass(etapa: EtapaOportunidad | string): string {
    switch (etapa) {
      case EtapaOportunidad.PROSPECCION: return 'blue';
      case EtapaOportunidad.CALIFICACION: return 'purple';
      case EtapaOportunidad.PROPUESTA: return 'orange';
      case EtapaOportunidad.NEGOCIACION: return 'emerald';
      case EtapaOportunidad.GANADA: return 'success';
      case EtapaOportunidad.PERDIDA: return 'danger';
      default: return 'neutral';
    }
  }

  formatearMoneda(valor: number, decimales = 0): string {
    return new Intl.NumberFormat('es-PE', {
      style: 'currency',
      currency: 'PEN',
      maximumFractionDigits: decimales,
      minimumFractionDigits: decimales
    }).format(valor || 0);
  }

  iniciales(nombre: string): string {
    return (nombre || '?')
      .split(' ')
      .filter(Boolean)
      .slice(0, 2)
      .map((p) => p[0]?.toUpperCase())
      .join('');
  }
}
