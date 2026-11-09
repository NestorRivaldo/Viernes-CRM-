import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { debounceTime } from 'rxjs';
import { DraftService } from '../../../core/services/draft.service';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ClientesService } from '../clientes.service';
import {
  ClienteRequest,
  ClienteResponse,
  ContactoRequest,
  ContactoResponse,
  DocumentoConsultaResponse,
  EstadoCliente,
  TipoDocumento,
} from '../clientes.model';

export interface ClienteDialogData {
  id?: number | null;
}

export interface ClienteDialogResult {
  saved: boolean;
  id?: number;
}

type TabId = 'general' | 'contactos';

@Component({
  selector: 'app-cliente-dialog',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './cliente-dialog.html',
  styleUrl: './cliente-dialog.css',
})
export class ClienteDialog implements OnInit {
  private fb = inject(FormBuilder);
  private dialogRef = inject(MatDialogRef<ClienteDialog, ClienteDialogResult>);
  private clientesService = inject(ClientesService);
  private draft = inject(DraftService);
  private destroyRef = inject(DestroyRef);

  readonly data = inject<ClienteDialogData>(MAT_DIALOG_DATA);

  readonly id = signal<number | null>(this.data?.id ?? null);
  readonly esEdicion = computed(() => this.id() !== null);
  readonly draftKey = computed(() => `cliente:${this.id() ?? 'nuevo'}`);
  readonly tabActiva = signal<TabId>('general');

  readonly estados = Object.values(EstadoCliente);
  readonly tiposDocumento = Object.values(TipoDocumento);

  readonly cargando = signal(false);
  readonly guardando = signal(false);
  readonly error = signal('');
  readonly consultandoDocumento = signal(false);
  readonly errorDocumento = signal('');
  readonly documentoConsultado = signal<DocumentoConsultaResponse | null>(null);

  readonly cliente = signal<ClienteResponse | null>(null);

  readonly contactos = signal<ContactoResponse[]>([]);
  readonly cargandoContactos = signal(false);
  readonly guardandoContacto = signal(false);
  readonly errorContactos = signal('');
  readonly editandoContactoId = signal<number | null>(null);

  readonly form = this.fb.group({
    nombre: ['', [Validators.required, Validators.maxLength(150)]],
    empresa: ['', Validators.maxLength(150)],
    tipoDocumento: [null as TipoDocumento | null],
    numeroDocumento: ['', [Validators.maxLength(20), this.documentoValidator]],
    correo: ['', [Validators.required, Validators.email, Validators.maxLength(150)]],
    telefono: ['', Validators.maxLength(30)],
    estado: [EstadoCliente.ACTIVO, Validators.required],
    responsable: ['', Validators.maxLength(100)],
  });

  readonly contactoForm = this.fb.group({
    nombre: ['', [Validators.required, Validators.maxLength(100)]],
    apellido: ['', Validators.maxLength(100)],
    cargo: ['', Validators.maxLength(100)],
    correo: ['', [Validators.email, Validators.maxLength(150)]],
    telefono: ['', Validators.maxLength(30)],
    principal: [false],
  });

  ngOnInit() {
    if (this.esEdicion()) {
      this.cargarCliente();
      this.cargarContactos();
    } else {
      this.restaurarBorrador();
    }
    if (this.dialogRef) {
      this.dialogRef.backdropClick().subscribe(() => {
        if (this.form.dirty) this.draft.save(this.draftKey(), this.form.getRawValue());
      });
    }
    this.form.get('tipoDocumento')?.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.form.get('numeroDocumento')?.updateValueAndValidity());
    this.form.get('numeroDocumento')?.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        if (this.errorDocumento()) this.errorDocumento.set('');
        if (this.documentoConsultado()) this.documentoConsultado.set(null);
      });
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

  private cargarCliente() {
    const id = this.id();
    if (!id) return;

    this.cargando.set(true);
    this.error.set('');
    this.clientesService.obtener(id).subscribe({
      next: (cliente) => {
        this.cliente.set(cliente);
        this.form.patchValue({
          nombre: cliente.nombre || '',
          empresa: cliente.empresa || '',
          tipoDocumento: cliente.tipoDocumento || null,
          numeroDocumento: cliente.numeroDocumento || '',
          correo: cliente.correo || '',
          telefono: cliente.telefono || '',
          estado: cliente.estado || EstadoCliente.ACTIVO,
          responsable: cliente.responsable || '',
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
        this.error.set(this.mensajeHttp(err, 'No se pudo cargar el cliente.'));
        this.cargando.set(false);
      },
    });
  }

  cargarContactos() {
    const id = this.id();
    if (!id) return;

    this.cargandoContactos.set(true);
    this.errorContactos.set('');
    this.clientesService.listarContactos(id).subscribe({
      next: (data) => {
        this.contactos.set(data || []);
        this.cargandoContactos.set(false);
      },
      error: (err) => {
        this.errorContactos.set(this.mensajeHttp(err, 'No se pudieron cargar los contactos.'));
        this.cargandoContactos.set(false);
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
    const request: ClienteRequest = {
      nombre: (v.nombre || '').trim(),
      empresa: (v.empresa || '').trim() || undefined,
      tipoDocumento: v.tipoDocumento || undefined,
      numeroDocumento: (v.numeroDocumento || '').trim() || undefined,
      correo: (v.correo || '').trim(),
      telefono: (v.telefono || '').trim() || undefined,
      estado: v.estado || EstadoCliente.ACTIVO,
      responsable: (v.responsable || '').trim() || undefined,
    };

    this.guardando.set(true);
    this.error.set('');

    const req$ = this.esEdicion()
      ? this.clientesService.actualizar(this.id()!, request)
      : this.clientesService.crear(request);

    req$.subscribe({
      next: (cliente) => {
        this.guardando.set(false);
        this.draft.clear(this.draftKey());
        this.dialogRef.close({ saved: true, id: cliente.id });
      },
      error: (err) => {
        this.guardando.set(false);
        this.error.set(this.mensajeHttp(err, 'No se pudo guardar el cliente.'));
      },
    });
  }

  cerrar() {
    if (this.guardando()) return;
    this.dialogRef.close({ saved: false });
  }

  consultarDocumento() {
    if (this.consultandoDocumento()) return;

    const control = this.form.get('numeroDocumento');
    const tipo = this.form.get('tipoDocumento')?.value as TipoDocumento | null;
    const numero = String(control?.value || '').trim();

    this.errorDocumento.set('');

    if (!tipo) {
      control?.markAsTouched();
      this.errorDocumento.set('Selecciona el tipo de documento antes de consultar.');
      return;
    }

    control?.markAsTouched();
    if (!numero || control?.invalid) {
      // El error de formato se muestra debajo del campo mediante el validador.
      this.errorDocumento.set('');
      return;
    }

    this.consultandoDocumento.set(true);
    this.clientesService.consultarDocumento(tipo, numero).subscribe({
      next: (documento) => {
        this.documentoConsultado.set(documento);

        if (documento.tipoDocumento === TipoDocumento.DNI) {
          const nombreActual = String(this.form.get('nombre')?.value || '').trim();
          this.form.patchValue({ nombre: documento.nombreCompleto || nombreActual });
        } else {
          const razonSocial = documento.razonSocial || documento.nombreComercial || '';
          const nombreActual = String(this.form.get('nombre')?.value || '').trim();
          this.form.patchValue({
            empresa: razonSocial || this.form.get('empresa')?.value || '',
            nombre: nombreActual || razonSocial,
          });

          const telefono = documento.telefonos?.find((valor) => !!valor?.trim());
          if (telefono && !String(this.form.get('telefono')?.value || '').trim()) {
            this.form.patchValue({ telefono });
          }
        }

        this.form.markAsDirty();
        this.consultandoDocumento.set(false);
      },
      error: (err) => {
        this.consultandoDocumento.set(false);
        this.errorDocumento.set(this.mensajeHttp(err, 'No se pudo consultar el documento.'));
      },
    });
  }

  /* ----- Contactos ----- */

  guardarContacto() {
    const id = this.id();
    if (!id || this.guardandoContacto()) return;

    if (this.contactoForm.invalid) {
      this.contactoForm.markAllAsTouched();
      return;
    }

    const v = this.contactoForm.getRawValue();
    const request: ContactoRequest = {
      nombre: (v.nombre || '').trim(),
      apellido: (v.apellido || '').trim() || undefined,
      cargo: (v.cargo || '').trim() || undefined,
      correo: (v.correo || '').trim() || undefined,
      telefono: (v.telefono || '').trim() || undefined,
      principal: !!v.principal,
    };

    const contactoId = this.editandoContactoId();
    this.guardandoContacto.set(true);
    this.errorContactos.set('');

    const req$ = contactoId
      ? this.clientesService.actualizarContacto(id, contactoId, request)
      : this.clientesService.agregarContacto(id, request);

    req$.subscribe({
      next: () => {
        this.guardandoContacto.set(false);
        this.cancelarEdicionContacto();
        this.cargarContactos();
      },
      error: (err) => {
        this.guardandoContacto.set(false);
        this.errorContactos.set(this.mensajeHttp(err, 'No se pudo guardar el contacto.'));
      },
    });
  }

  editarContacto(contacto: ContactoResponse) {
    this.editandoContactoId.set(contacto.id);
    this.contactoForm.patchValue({
      nombre: contacto.nombre || '',
      apellido: contacto.apellido || '',
      cargo: contacto.cargo || '',
      correo: contacto.correo || '',
      telefono: contacto.telefono || '',
      principal: !!contacto.principal,
    });
    this.tabActiva.set('contactos');
  }

  cancelarEdicionContacto() {
    this.editandoContactoId.set(null);
    this.contactoForm.reset({
      nombre: '',
      apellido: '',
      cargo: '',
      correo: '',
      telefono: '',
      principal: false,
    });
  }

  eliminarContacto(contacto: ContactoResponse) {
    const id = this.id();
    if (!id) return;

    this.errorContactos.set('');
    this.clientesService.eliminarContacto(id, contacto.id).subscribe({
      next: () => this.cargarContactos(),
      error: (err) => this.errorContactos.set(this.mensajeHttp(err, 'No se pudo eliminar el contacto.')),
    });
  }

  /* ----- Helpers ----- */

  nombreCompleto(contacto: ContactoResponse): string {
    return `${contacto.nombre || ''} ${contacto.apellido || ''}`.trim();
  }

  iniciales(nombre: string | null | undefined): string {
    return (nombre || '')
      .split(' ')
      .filter(Boolean)
      .slice(0, 2)
      .map((p) => p[0]?.toUpperCase())
      .join('');
  }

  mostrarError(campo: string): boolean {
    return this.controlInvalido(this.form.get(campo));
  }

  mensajeError(campo: string): string {
    return this.mensajeControl(this.form.get(campo));
  }

  mostrarErrorContacto(campo: string): boolean {
    return this.controlInvalido(this.contactoForm.get(campo));
  }

  mensajeErrorContacto(campo: string): string {
    return this.mensajeControl(this.contactoForm.get(campo));
  }

  private controlInvalido(control: AbstractControl | null): boolean {
    return !!control && control.invalid && (control.touched || control.dirty);
  }

  private documentoValidator(control: AbstractControl): ValidationErrors | null {
    const tipo = control.parent?.get('tipoDocumento')?.value as TipoDocumento | null;
    const numero = (control.value || '').trim();

    if (!tipo && !numero) return null;
    if (!tipo) return { tipoDocumentoRequerido: true };
    if (!numero) return { required: true };

    const patron = tipo === TipoDocumento.DNI ? /^\d{8}$/ : /^\d{11}$/;
    return patron.test(numero) ? null : { documentoInvalido: true };
  }

  private mensajeControl(control: AbstractControl | null): string {
    if (!control) return '';
    if (control.hasError('required')) return 'Este campo es obligatorio.';
    if (control.hasError('tipoDocumentoRequerido')) return 'Selecciona el tipo de documento.';
    if (control.hasError('documentoInvalido')) {
      const tipo = control.parent?.get('tipoDocumento')?.value;
      return tipo === TipoDocumento.RUC
        ? 'El RUC debe tener exactamente 11 dígitos.'
        : 'El DNI debe tener exactamente 8 dígitos.';
    }
    if (control.hasError('email')) return 'Ingresa un correo válido.';
    if (control.hasError('maxlength')) {
      return `Máximo ${control.getError('maxlength').requiredLength} caracteres.`;
    }
    return 'Valor inválido.';
  }

  private mensajeHttp(err: any, fallback: string): string {
    const api = err?.error;
    return (
      err?.friendlyMessage ||
      (typeof api === 'string' ? api : api?.message || api?.detail) ||
      fallback
    );
  }
}
