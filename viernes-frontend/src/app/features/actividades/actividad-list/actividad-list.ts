import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ActividadesService } from '../actividades.service';
import { ActividadResponse, EstadoActividad } from '../actividades.model';

@Component({
  selector: 'app-actividad-list',
  standalone: true,
  imports: [CommonModule, MatTableModule, MatButtonModule, MatIconModule, RouterLink, MatProgressSpinnerModule],
  templateUrl: './actividad-list.html',
  styleUrl: './actividad-list.css'
})
export class ActividadList implements OnInit {
  private actividadesService = inject(ActividadesService);
  private router = inject(Router);

  actividades = signal<ActividadResponse[]>([]);
  displayedColumns: string[] = ['id', 'titulo', 'tipo', 'estado', 'fecha', 'vencida', 'acciones'];
  loading = signal(false);
  error = signal('');

  hasActividades = computed(() => this.actividades().length > 0 && !this.loading());
  isEmpty = computed(() => !this.loading() && this.actividades().length === 0 && !this.error());

  ngOnInit() {
    this.cargarActividades();
  }

  cargarActividades() {
    this.loading.set(true);
    this.error.set('');
    this.actividadesService.listar().subscribe({
      next: (data) => {
        this.actividades.set(data || []);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Error al cargar actividades.');
        this.loading.set(false);
      }
    });
  }

  completar(id: number) {
    this.actividadesService.cambiarEstado(id, EstadoActividad.COMPLETADA).subscribe({
      next: () => this.cargarActividades(),
      error: () => alert('Error al completar')
    });
  }

  eliminar(id: number) {
    if (confirm('¿Está seguro de eliminar esta actividad?')) {
      this.actividadesService.eliminar(id).subscribe({
        next: () => this.cargarActividades(),
        error: () => alert('Error al eliminar')
      });
    }
  }

  editar(id: number) {
    this.router.navigate(['/actividades', id, 'editar']);
  }
}

