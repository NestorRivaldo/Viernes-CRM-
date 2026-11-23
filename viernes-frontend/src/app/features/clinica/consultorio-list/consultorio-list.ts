import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ConsultorioResponse } from '../clinica.model';
import { ClinicaService } from '../clinica.service';

@Component({
  selector: 'app-consultorio-list',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './consultorio-list.html',
  styleUrls: ['./consultorio-list.css']
})
export class ConsultorioList implements OnInit {
  private clinicaService = inject(ClinicaService);
  private router = inject(Router);

  consultorios = signal<ConsultorioResponse[]>([]);
  loading = signal(false);
  error = signal('');

  ngOnInit() {
    this.cargarConsultorios();
  }

  cargarConsultorios() {
    this.loading.set(true);
    this.error.set('');
    this.clinicaService.listarConsultorios().subscribe({
      next: (data) => {
        this.consultorios.set(data || []);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err?.error?.message || err?.message || 'Error al cargar consultorios');
        this.loading.set(false);
      }
    });
  }

  nuevoConsultorio() {
    this.router.navigate(['/consultorios/nuevo']);
  }

  verConsultorio(c: ConsultorioResponse) {
    this.router.navigate(['/consultorios', c.id]);
  }

  eliminarConsultorio(c: ConsultorioResponse) {
    const confirmacion = window.confirm(`¿Estás seguro de eliminar el consultorio "${c.codigo}"?`);
    if (!confirmacion) return;

    this.clinicaService.eliminarConsultorio(c.id).subscribe({
      next: () => {
        this.cargarConsultorios();
      },
      error: (err) => {
        alert(err?.error?.message || err?.message || 'Error al eliminar consultorio');
      }
    });
  }
}
