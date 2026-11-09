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
import { UsuariosService } from '../usuarios.service';
import { RolUsuario, UsuarioRequest } from '../usuarios.model';

export interface UsuarioDialogData {
  id?: number | null;
}

export interface UsuarioDialogResult {
  saved: boolean;
}

@Component({
  selector: 'app-usuario-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './usuario-form.html',
  styleUrls: ['./usuario-form.css', '../../../shared/styles/entity-dialog.css']
})
export class UsuarioForm implements OnInit {
  private fb = inject(FormBuilder);
  private usuariosService = inject(UsuariosService);
  private cdr = inject(ChangeDetectorRef);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private dialogRef = inject<MatDialogRef<UsuarioForm, UsuarioDialogResult> | null>(MatDialogRef, {
    optional: true,
  });
  private dialogData = inject<UsuarioDialogData | null>(MAT_DIALOG_DATA, { optional: true });
  private draft = inject(DraftService);
  private destroyRef = inject(DestroyRef);

  readonly esDialogo = this.dialogRef !== null;

  form: FormGroup;
  isEdit = false;
  id!: number;
  loading = false;
  cargandoUsuario = false;
  mostrarPassword = false;
  serverError = '';

  private submitted = false;

  readonly rolesInfo: { value: RolUsuario; label: string; hint: string; icon: string; tone: string }[] = [
    {
      value: RolUsuario.ADMIN,
      label: 'Administrador',
      hint: 'Acceso total al CRM',
      icon: 'shield',
      tone: 'violet',
    },
    {
      value: RolUsuario.JEFE,
      label: 'Jefe de equipo',
      hint: 'Supervisión y reportes',
      icon: 'supervisor_account',
      tone: 'blue',
    },
    {
      value: RolUsuario.VENDEDOR,
      label: 'Vendedor',
      hint: 'Gestión comercial',
      icon: 'point_of_sale',
      tone: 'emerald',
    },
  ];

  constructor() {
    this.form = this.fb.group({
      nombre: ['', [Validators.required, Validators.maxLength(100)]],
      apellido: ['', [Validators.required, Validators.maxLength(100)]],
      correo: ['', [Validators.required, Validators.email, Validators.maxLength(150)]],
      password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(100)]],
      rol: [RolUsuario.VENDEDOR, Validators.required],
    });
  }

  get draftKey(): string {
    return `usuario:${this.isEdit ? this.id : 'nuevo'}`;
  }

  ngOnInit() {
    const id = this.dialogData?.id ?? this.idDeRuta();

    if (id) {
      this.isEdit = true;
      this.id = id;
      // En edición la contraseña es opcional: vacía = mantener la actual.
      this.form.get('password')?.setValidators([Validators.minLength(8), Validators.maxLength(100)]);
      this.form.get('password')?.updateValueAndValidity();
      this.cargarUsuario();
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

  cargarUsuario() {
    this.cargandoUsuario = true;
    this.usuariosService.obtener(this.id).subscribe({
      next: (usuario) => {
        this.form.patchValue(usuario);
        this.cargandoUsuario = false;
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
        this.cargandoUsuario = false;
        this.cdr.markForCheck();
        alert(err?.friendlyMessage || 'Error al cargar el usuario');
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
    const request: UsuarioRequest = { ...this.form.value };
    if (this.isEdit && !request.password) {
      delete request.password;
    }

    const req$ = this.isEdit
      ? this.usuariosService.actualizar(this.id, request)
      : this.usuariosService.crear(request);

    req$.subscribe({
      next: () => {
        this.draft.clear(this.draftKey);
        this.cerrar(true);
      },
      error: (err) => {
        this.serverError = err?.friendlyMessage || 'Error al guardar el usuario.';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  seleccionarRol(rol: RolUsuario) {
    this.form.patchValue({ rol });
  }

  mostrarError(campo: string): boolean {
    const control = this.form.get(campo);
    return !!control && control.invalid && (control.touched || this.submitted);
  }

  mensajeError(campo: string): string {
    const errors = this.form.get(campo)?.errors;
    if (!errors) return '';

    if (errors['required']) return 'Este campo es obligatorio.';
    if (errors['email']) return 'Ingresa un correo electrónico válido.';
    if (errors['minlength']) return `Mínimo ${errors['minlength'].requiredLength} caracteres.`;
    if (errors['maxlength']) return `Máximo ${errors['maxlength'].requiredLength} caracteres.`;
    return 'Revisa este campo.';
  }

  cerrar(saved = false) {
    if (this.dialogRef) {
      this.dialogRef.close({ saved });
      return;
    }
    this.router.navigate(['/usuarios']);
  }
}
