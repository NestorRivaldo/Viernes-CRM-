import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { DashboardService } from './dashboard.service';
import { DashboardResumenResponse } from './dashboard.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatProgressSpinnerModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css'
})
export class Dashboard implements OnInit {
  private dashboardService = inject(DashboardService);

  resumen = signal<DashboardResumenResponse | null>(null);
  loading = signal(false);
  error = signal('');

  hasResumen = computed(() => this.resumen() !== null && !this.loading());

  ngOnInit() {
    this.loading.set(true);
    this.error.set('');
    this.dashboardService.getResumen().subscribe({
      next: (res) => {
        this.resumen.set(res);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Error al cargar resumen del dashboard. Verifique que el backend esté disponible.');
        this.loading.set(false);
      }
    });
  }
}
