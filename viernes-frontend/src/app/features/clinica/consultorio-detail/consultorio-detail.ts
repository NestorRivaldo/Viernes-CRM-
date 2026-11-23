import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ConsultorioResponse } from '../clinica.model';
import { ClinicaService } from '../clinica.service';

@Component({
  selector: 'app-consultorio-detail',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './consultorio-detail.html',
  styleUrls: ['./consultorio-detail.css']
})
export class ConsultorioDetail implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private clinicaService = inject(ClinicaService);

  consultorio = signal<ConsultorioResponse | null>(null);
  loading = signal(true);
  error = signal('');

  ngOnInit() {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.cargarDetalle(Number(idParam));
    } else {
      this.error.set('ID de consultorio no proporcionado');
      this.loading.set(false);
    }
  }

  cargarDetalle(id: number) {
    this.loading.set(true);
    this.clinicaService.obtenerConsultorio(id).subscribe({
      next: (data) => {
        this.consultorio.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err?.error?.message || err?.message || 'Error al cargar el detalle del consultorio');
        this.loading.set(false);
      }
    });
  }

  volver() {
    this.router.navigate(['/consultorios']);
  }
}
