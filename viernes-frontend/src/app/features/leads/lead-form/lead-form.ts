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
import { LeadsService } from '../leads.service';
import { EstadoLead, LeadRequest } from '../leads.model';

export interface LeadDialogData {
  id?: number | null;
}

export interface LeadDialogResult {
  saved: boolean;
}

@Component({
  selector: 'app-lead-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './lead-form.html',
  styleUrls: ['./lead-form.css', '../../../shared/styles/entity-dialog.css']
})
export class LeadForm implements OnInit {
  private fb = inject(FormBuilder);
  private leadsService = inject(LeadsService);
  private cdr = inject(ChangeDetectorRef);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private dialogRef = inject<MatDialogRef<LeadForm, LeadDialogResult> | null>(MatDialogRef, {
    optional: true,
  });
  private dialogData = inject<LeadDialogData | null>(MAT_DIALOG_DATA, { optional: true });
  private draft = inject(DraftService);
  private destroyRef = inject(DestroyRef);

  readonly esDialogo = this.dialogRef !== null;
  readonly estados = Object.values(EstadoLead);

  form: FormGroup;
  isEdit = false;
  id!: number;
  loading = false;
  cargandoLead = false;
  serverError = '';

  private submitted = false;

  constructor() {
    this.form = this.fb.group({
      nombre: ['', [Validators.required, Validators.maxLength(150)]],
      empresa: ['', Validators.maxLength(150)],
      correo: ['', [Validators.required, Validators.email, Validators.maxLength(150)]],
      telefono: ['', Validators.maxLength(30)],
      fuente: ['', Validators.maxLength(80)],
      estado: [EstadoLead.NUEVO, Validators.required],
      responsable: ['', Validators.maxLength(100)],
      fecha: [''],
    });
  }

  get draftKey(): string {
    return `lead:${this.isEdit ? this.id : 'nuevo'}`;
  }

  ngOnInit() {
    const id = this.dialogData?.id ?? this.idDeRuta();

    if (id) {
      this.isEdit = true;
      this.id = id;
      this.cargarLead();
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

  cargarLead() {
    this.cargandoLead = true;
    this.leadsService.obtener(this.id).subscribe({
      next: (lead) => {
        this.form.patchValue(lead);
        this.cargandoLead = false;
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
        this.cargandoLead = false;
        this.cdr.markForCheck();
        alert(err?.friendlyMessage || 'Error al cargar el lead');
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
    const request = this.form.getRawValue() as LeadRequest;

    const req$ = this.isEdit
      ? this.leadsService.actualizar(this.id, request)
      : this.leadsService.crear(request);

    req$.subscribe({
      next: () => {
        this.draft.clear(this.draftKey);
        this.cerrar(true);
      },
      error: (err) => {
        this.serverError = err?.friendlyMessage || 'Error al guardar el lead.';
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
    this.router.navigate(['/leads']);
  }
}
