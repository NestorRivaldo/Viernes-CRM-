import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Clinica, Especialidad, EstadoConsultorio } from '../clinica.model';
import { ClinicaService } from '../clinica.service';

@Component({
  selector: 'app-consultorio-form',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './consultorio-form.html',
  styleUrls: ['./consultorio-form.css']
})
export class ConsultorioForm implements OnInit {
  private fb = inject(FormBuilder);
  private clinicaService = inject(ClinicaService);
  private router = inject(Router);

  form!: FormGroup;
  guardando = false;
  cargandoCatalogos = false;
  serverError = '';
  submitted = false;

  // Catálogos
  clinicas: Clinica[] = [];
  especialidadesCatalogo: Especialidad[] = [];
  especialidadSeleccionadaId: number | null = null;

  // Tabla dinámica de detalle en memoria (Figura 5)
  especialidadesAsignadas: Especialidad[] = [];

  readonly estados: EstadoConsultorio[] = ['DISPONIBLE', 'OCUPADO', 'MANTENIMIENTO'];

  ngOnInit() {
    this.form = this.fb.group({
      idClinica: ['', [Validators.required]],
      codigo: ['', [Validators.required, Validators.maxLength(10)]],
      tarifaHora: ['', [Validators.required, Validators.min(0.01)]],
      estado: ['DISPONIBLE', [Validators.required]]
    });

    this.cargarCatalogos();
  }

  cargarCatalogos() {
    this.cargandoCatalogos = true;
    this.clinicaService.listarClinicas().subscribe({
      next: (clinicasData) => {
        this.clinicas = clinicasData || [];
        if (this.clinicas.length > 0 && !this.form.get('idClinica')?.value) {
          this.form.patchValue({ idClinica: this.clinicas[0].id });
        }
      }
    });

    this.clinicaService.listarEspecialidades().subscribe({
      next: (espData) => {
        this.especialidadesCatalogo = espData || [];
        if (this.especialidadesCatalogo.length > 0) {
          this.especialidadSeleccionadaId = this.especialidadesCatalogo[0].id;
        }
        this.cargandoCatalogos = false;
      },
      error: () => {
        this.cargandoCatalogos = false;
      }
    });
  }

  // Agregar especialidad a la subtabla
  agregarEspecialidad() {
    if (!this.especialidadSeleccionadaId) return;
    const esp = this.especialidadesCatalogo.find(e => e.id === Number(this.especialidadSeleccionadaId));
    if (!esp) return;

    // Evitar duplicados
    const yaAsignada = this.especialidadesAsignadas.some(e => e.id === esp.id);
    if (!yaAsignada) {
      this.especialidadesAsignadas.push(esp);
    }
  }

  // Quitar especialidad de la subtabla
  quitarEspecialidad(id: number) {
    this.especialidadesAsignadas = this.especialidadesAsignadas.filter(e => e.id !== id);
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

    const payload = {
      idClinica: Number(val.idClinica),
      codigo: val.codigo.trim(),
      tarifaHora: Number(val.tarifaHora),
      estado: val.estado as EstadoConsultorio,
      especialidadIds: this.especialidadesAsignadas.map(e => e.id)
    };

    this.clinicaService.crearConsultorio(payload).subscribe({
      next: () => {
        this.guardando = false;
        this.router.navigate(['/consultorios']);
      },
      error: (err) => {
        this.guardando = false;
        this.serverError = err?.error?.message || err?.message || 'Error al guardar el consultorio';
      }
    });
  }

  cancelar() {
    this.router.navigate(['/consultorios']);
  }
}
