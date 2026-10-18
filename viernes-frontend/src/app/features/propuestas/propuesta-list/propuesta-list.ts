import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { PropuestasService } from '../propuestas.service';
import { PropuestaResponse } from '../propuestas.model';

@Component({
  selector: 'app-propuesta-list',
  standalone: true,
  imports: [CommonModule, MatTableModule, MatButtonModule, MatIconModule, RouterLink, MatProgressSpinnerModule],
  templateUrl: './propuesta-list.html',
  styleUrl: './propuesta-list.css'
})
export class PropuestaList implements OnInit {
  private propuestasService = inject(PropuestasService);
  private router = inject(Router);

  propuestas = signal<PropuestaResponse[]>([]);
  displayedColumns: string[] = ['numeroPropuesta', 'cliente', 'estado', 'total', 'fechaEmision', 'acciones'];
  loading = signal(false);
  error = signal('');

  hasPropuestas = computed(() => this.propuestas().length > 0 && !this.loading());
  isEmpty = computed(() => !this.loading() && this.propuestas().length === 0 && !this.error());

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

