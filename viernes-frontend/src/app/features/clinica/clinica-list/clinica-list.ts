import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Clinica } from '../clinica.model';
import { ClinicaService } from '../clinica.service';
import { ClinicaForm } from '../clinica-form/clinica-form';

@Component({
  selector: 'app-clinica-list',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatProgressSpinnerModule, ClinicaForm],
  templateUrl: './clinica-list.html',
  styleUrls: ['./clinica-list.css']
})
export class ClinicaList implements OnInit {
  private clinicaService = inject(ClinicaService);

  clinicas = signal<Clinica[]>([]);
  loading = signal(false);
  error = signal('');

  // Control del modal de formulario
  mostrarModal = signal(false);
  clinicaSeleccionada = signal<Clinica | null>(null);

  ngOnInit() {
    this.cargarClinicas();
  }

  cargarClinicas() {
    this.loading.set(true);
    this.error.set('');
    this.clinicaService.listarClinicas().subscribe({
      next: (data) => {
        this.clinicas.set(data || []);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err?.error?.message || err?.message || 'Error al cargar las clínicas');
        this.loading.set(false);
      }
    });
  }

  nuevaClinica() {
    this.clinicaSeleccionada.set(null);
    this.mostrarModal.set(true);
  }

  editarClinica(c: Clinica) {
    this.clinicaSeleccionada.set(c);
    this.mostrarModal.set(true);
  }

  eliminarClinica(c: Clinica) {
    if (!c.id) return;
    const confirmacion = window.confirm(`¿Estás seguro de eliminar la clínica "${c.razonSocial}"?`);
    if (!confirmacion) return;

    this.clinicaService.eliminarClinica(c.id).subscribe({
      next: () => {
        this.cargarClinicas();
      },
      error: (err) => {
        alert(err?.error?.message || err?.message || 'Error al eliminar la clínica');
      }
    });
  }

  onFormGuardado() {
    this.mostrarModal.set(false);
    this.cargarClinicas();
  }

  onFormCancelado() {
    this.mostrarModal.set(false);
  }
}
