import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ActividadesService } from '../actividades.service';
import { ActividadResponse, EstadoActividad } from '../actividades.model';

@Component({
  selector: 'app-actividad-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    RouterLink,
    MatProgressSpinnerModule
  ],
  templateUrl: './actividad-list.html',
  styleUrl: './actividad-list.css'
})
export class ActividadList implements OnInit {
  private actividadesService = inject(ActividadesService);
  private router = inject(Router);

  actividades = signal<ActividadResponse[]>([]);
  filtroTexto = signal('');
  loading = signal(false);
  error = signal('');

  displayedColumns: string[] = ['id', 'titulo', 'tipo', 'estado', 'fecha', 'vencida', 'acciones'];

  actividadesFiltradas = computed(() => {
    const q = this.filtroTexto().toLowerCase().trim();
    return this.actividades().filter(a => {
      return !q ||
        a.titulo?.toLowerCase().includes(q) ||
        a.tipo?.toLowerCase().includes(q) ||
        a.estado?.toLowerCase().includes(q) ||
        a.descripcion?.toLowerCase().includes(q);
    });
  });

  hasActividades = computed(() => this.actividadesFiltradas().length > 0 && !this.loading());
  isEmpty = computed(() => !this.loading() && this.actividadesFiltradas().length === 0 && !this.error());

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
      error: () => alert('Error al completar actividad')
    });
  }

  getEstadoClass(estado: EstadoActividad | string): string {
    switch (estado) {
      case EstadoActividad.PENDIENTE: return 'warning';
      case EstadoActividad.EN_PROCESO: return 'blue';
      case EstadoActividad.COMPLETADA: return 'success';
      case EstadoActividad.CANCELADA: return 'neutral';
      default: return 'neutral';
    }
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
