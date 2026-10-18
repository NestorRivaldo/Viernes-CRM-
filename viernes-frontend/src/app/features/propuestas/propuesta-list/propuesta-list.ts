import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { PropuestasService } from '../propuestas.service';
import { PropuestaResponse, EstadoPropuesta } from '../propuestas.model';

@Component({
  selector: 'app-propuesta-list',
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
  templateUrl: './propuesta-list.html',
  styleUrl: './propuesta-list.css'
})
export class PropuestaList implements OnInit {
  private propuestasService = inject(PropuestasService);
  private router = inject(Router);

  propuestas = signal<PropuestaResponse[]>([]);
  filtroTexto = signal('');
  loading = signal(false);
  error = signal('');

  displayedColumns: string[] = ['numeroPropuesta', 'cliente', 'estado', 'total', 'fechaEmision', 'acciones'];

  propuestasFiltradas = computed(() => {
    const q = this.filtroTexto().toLowerCase().trim();
    return this.propuestas().filter(p => {
      return !q ||
        p.numeroPropuesta?.toLowerCase().includes(q) ||
        p.clienteNombre?.toLowerCase().includes(q) ||
        p.responsable?.toLowerCase().includes(q) ||
        p.estado?.toLowerCase().includes(q);
    });
  });

  totalPropuestasValor = computed(() => {
    return this.propuestasFiltradas().reduce((acc, p) => acc + (p.total || 0), 0);
  });

  hasPropuestas = computed(() => this.propuestasFiltradas().length > 0 && !this.loading());
  isEmpty = computed(() => !this.loading() && this.propuestasFiltradas().length === 0 && !this.error());

  ngOnInit() {
    this.cargarPropuestas();
  }

  cargarPropuestas() {
    this.loading.set(true);
    this.error.set('');
    this.propuestasService.listar().subscribe({
      next: (data) => {
        this.propuestas.set(data || []);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Error al cargar propuestas.');
        this.loading.set(false);
      }
    });
  }

  getEstadoClass(estado: EstadoPropuesta | string): string {
    switch (estado) {
      case EstadoPropuesta.BORRADOR: return 'warning';
      case EstadoPropuesta.ENVIADA: return 'blue';
      case EstadoPropuesta.APROBADA: return 'success';
      case EstadoPropuesta.RECHAZADA: return 'danger';
      case EstadoPropuesta.CANCELADA: return 'neutral';
      default: return 'neutral';
    }
  }

  formatearSoles(valor: number): string {
    return new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN', maximumFractionDigits: 0 }).format(valor || 0);
  }

  eliminar(id: number) {
    if (confirm('¿Está seguro de eliminar esta propuesta? (Solo si está en BORRADOR)')) {
      this.propuestasService.eliminar(id).subscribe({
        next: () => this.cargarPropuestas(),
        error: (err) => alert(err?.friendlyMessage || 'Error al eliminar, es posible que no esté en estado BORRADOR')
      });
    }
  }

  editar(id: number) {
    this.router.navigate(['/propuestas', id, 'editar']);
  }
}
