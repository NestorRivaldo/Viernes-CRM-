import { ChangeDetectorRef, Component, DestroyRef, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { debounceTime } from 'rxjs';
import { DraftService } from '../../../core/services/draft.service';
import { ActivatedRoute, Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { EquiposService } from '../equipos.service';
import { MiembroResponse } from '../equipo.model';
import { UsuariosService } from '../../usuarios/usuarios.service';
import { UsuarioResponse } from '../../usuarios/usuarios.model';

export interface EquipoDialogData {
  id?: number | null;
}

export interface EquipoDialogResult {
  saved: boolean;
}

@Component({
  selector: 'app-equipo-form',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './equipo-form.html',
  styleUrls: ['./equipo-form.css', '../../../shared/styles/entity-dialog.css']
})
export class EquipoForm implements OnInit {
  private fb = inject(FormBuilder);
  private equiposService = inject(EquiposService);
  private usuariosService = inject(UsuariosService);
  private cdr = inject(ChangeDetectorRef);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private dialogRef = inject<MatDialogRef<EquipoForm, EquipoDialogResult> | null>(MatDialogRef, {
    optional: true,
  });
  private dialogData = inject<EquipoDialogData | null>(MAT_DIALOG_DATA, { optional: true });

  readonly esDialogo = this.dialogRef !== null;

  form: FormGroup;
  isEdit = false;
  id!: number;
  loading = false;
  cargandoEquipo = false;
  recienCreado = false;
  guardado = false;
  cambiosMiembros = false;
  serverError = '';

  private submitted = false;

  usuarios: UsuarioResponse[] = [];
  usuariosLoading = false;
  usuariosError = '';
  usuarioSeleccionadoId: number | null = null;

  miembros: MiembroResponse[] = [];
  miembrosLoading = false;
  miembrosError = '';

  constructor(private draft: DraftService, private destroyRef: DestroyRef) {
    this.form = this.fb.group({
      nombre: ['', [Validators.required, Validators.maxLength(120)]],
      descripcion: ['', Validators.maxLength(500)],
    });
  }

  get titulo(): string {
    if (this.recienCreado) return 'Equipo creado';
    return this.isEdit ? 'Editar equipo' : 'Nuevo equipo';
  }

  get subtitulo(): string {
    if (this.recienCreado) return 'Agrega los miembros que formarán parte del equipo.';
    return this.isEdit
      ? 'Actualiza la información y administra los miembros del equipo.'
      : 'Crea un equipo para organizar a los usuarios del CRM.';
  }

  get draftKey(): string {
    return `equipo:${this.isEdit ? this.id : 'nuevo'}`;
  }

  ngOnInit() {
    const id = this.dialogData?.id ?? this.idDeRuta();

    if (id) {
      this.isEdit = true;
      this.id = id;
      this.cargarEquipo();
      this.cargarMiembros();
      this.cargarUsuarios();
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

  cargarEquipo() {
    this.cargandoEquipo = true;
    this.equiposService.obtener(this.id).subscribe({
      next: (equipo) => {
        this.form.patchValue(equipo);
        this.cargandoEquipo = false;
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
        this.cargandoEquipo = false;
        this.cdr.markForCheck();
        alert(err?.friendlyMessage || 'Error al cargar el equipo');
        this.cerrar(false);
      },
    });
  }

  guardar() {
    this.submitted = true;
    this.serverError = '';

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading = true;
    const request = this.form.value;

    const req$ = this.isEdit
      ? this.equiposService.actualizar(this.id, request)
      : this.equiposService.crear(request);

    req$.subscribe({
      next: (equipo) => {
        this.loading = false;
        this.guardado = true;

        if (!this.isEdit) {
          // Mantener el modal abierto para agregar miembros de inmediato.
          this.isEdit = true;
          this.id = equipo.id;
          this.recienCreado = true;
          this.draft.clear(this.draftKey);
          this.form.patchValue({
            nombre: equipo.nombre,
            descripcion: equipo.descripcion || '',
          });
          this.form.markAsPristine();
          this.cdr.markForCheck();
          this.cargarUsuarios();
          this.cargarMiembros();
        } else {
          this.draft.clear(this.draftKey);
          this.cerrar(true);
        }
      },
      error: (err) => {
        this.serverError = err?.friendlyMessage || 'Error al guardar el equipo.';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  cargarUsuarios() {
    this.usuariosLoading = true;
    this.usuariosError = '';
    this.usuariosService.listar().subscribe({
      next: (data) => {
        this.usuarios = data || [];
        this.usuariosLoading = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.usuariosError = err?.friendlyMessage || 'No se pudieron cargar los usuarios.';
        this.usuariosLoading = false;
        this.cdr.markForCheck();
      },
    });
  }

  cargarMiembros() {
    this.miembrosLoading = true;
    this.miembrosError = '';
    this.equiposService.listarMiembros(this.id).subscribe({
      next: (data) => {
        this.miembros = data || [];
        this.miembrosLoading = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.miembrosError = err?.friendlyMessage || 'Error al cargar los miembros del equipo.';
        this.miembrosLoading = false;
        this.cdr.markForCheck();
      },
    });
  }

  esMiembro(usuarioId: number): boolean {
    return this.miembros.some((m) => m.usuarioId === usuarioId);
  }

  agregarMiembro() {
    if (!this.usuarioSeleccionadoId) return;

    this.miembrosLoading = true;
    this.miembrosError = '';
    this.equiposService.agregarMiembro(this.id, this.usuarioSeleccionadoId).subscribe({
      next: () => {
        this.usuarioSeleccionadoId = null;
        this.cambiosMiembros = true;
        this.cdr.markForCheck();
        this.cargarMiembros();
      },
      error: (err) => {
        this.miembrosError = err?.friendlyMessage || 'Error al agregar el miembro.';
        this.miembrosLoading = false;
        this.cdr.markForCheck();
      },
    });
  }

  quitarMiembro(miembro: MiembroResponse) {
    if (!confirm(`¿Está seguro de quitar a ${miembro.nombreCompleto} del equipo?`)) {
      return;
    }

    this.miembrosLoading = true;
    this.miembrosError = '';
    this.equiposService.quitarMiembro(this.id, miembro.usuarioId).subscribe({
      next: () => {
        this.cambiosMiembros = true;
        this.cdr.markForCheck();
        this.cargarMiembros();
      },
      error: (err) => {
        this.miembrosError = err?.friendlyMessage || 'Error al quitar el miembro.';
        this.miembrosLoading = false;
        this.cdr.markForCheck();
      },
    });
  }

  inicial(nombre: string): string {
    return (nombre || '?').trim().charAt(0).toUpperCase() || '?';
  }

  mostrarError(campo: string): boolean {
    const control = this.form.get(campo);
    return !!control && control.invalid && (control.touched || this.submitted);
  }

  mensajeError(campo: string): string {
    const errors = this.form.get(campo)?.errors;
    if (!errors) return '';

    if (errors['required']) return 'Este campo es obligatorio.';
    if (errors['minlength']) return `Mínimo ${errors['minlength'].requiredLength} caracteres.`;
    if (errors['maxlength']) return `Máximo ${errors['maxlength'].requiredLength} caracteres.`;
    return 'Revisa este campo.';
  }

  cerrar(saved = false) {
    const resultado = saved || this.guardado || this.cambiosMiembros;

    if (this.dialogRef) {
      this.dialogRef.close({ saved: resultado });
      return;
    }
    this.router.navigate(['/equipos']);
  }
}
