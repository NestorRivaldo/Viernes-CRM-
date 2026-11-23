import { Component, EventEmitter, Input, OnInit, Output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { Clinica } from '../clinica.model';
import { ClinicaService } from '../clinica.service';

@Component({
  selector: 'app-clinica-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatIconModule],
  templateUrl: './clinica-form.html',
  styleUrls: ['./clinica-form.css']
})
export class ClinicaForm implements OnInit {
  private fb = inject(FormBuilder);
  private clinicaService = inject(ClinicaService);

  @Input() clinicaEditar: Clinica | null = null;
  @Output() guardado = new EventEmitter<void>();
  @Output() cancelado = new EventEmitter<void>();

  form!: FormGroup;
  guardando = false;
  serverError = '';
  submitted = false;

  ngOnInit() {
    this.form = this.fb.group({
      ruc: [
        this.clinicaEditar?.ruc || '',
        [Validators.required, Validators.pattern(/^\d{11}$/)]
      ],
      razonSocial: [
        this.clinicaEditar?.razonSocial || '',
        [Validators.required]
      ],
      telefono: [this.clinicaEditar?.telefono || ''],
      activo: [this.clinicaEditar?.activo ?? true]
    });
  }

  get isEdit(): boolean {
    return !!this.clinicaEditar?.id;
  }

  get titulo(): string {
    return this.isEdit ? 'Editar clínica' : 'Nueva clínica';
  }

  // Helpers de validación visibles
  tieneErrorRuc(): boolean {
    const c = this.form.get('ruc');
    return !!(c && (c.touched || this.submitted) && c.invalid);
  }

  tieneErrorRazonSocial(): boolean {
    const c = this.form.get('razonSocial');
    return !!(c && (c.touched || this.submitted) && c.invalid);
  }

  guardar() {
    this.submitted = true;
    this.serverError = '';

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.guardando = true;
    const val = this.form.value;

    const payload: Clinica = {
      ruc: val.ruc.trim(),
      razonSocial: val.razonSocial.trim(),
      telefono: val.telefono ? val.telefono.trim() : '',
      activo: !!val.activo
    };

    const request$ = this.isEdit && this.clinicaEditar?.id
      ? this.clinicaService.actualizarClinica(this.clinicaEditar.id, payload)
      : this.clinicaService.crearClinica(payload);

    request$.subscribe({
      next: () => {
        this.guardando = false;
        this.guardado.emit();
      },
      error: (err) => {
        this.guardando = false;
        this.serverError = err?.error?.message || err?.message || 'Error al guardar la clínica';
      }
    });
  }

  cancelar() {
    this.cancelado.emit();
  }
}
