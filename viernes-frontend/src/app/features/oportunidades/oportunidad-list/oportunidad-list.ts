import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { OportunidadesService } from '../oportunidades.service';
import { OportunidadResponse } from '../oportunidades.model';

@Component({
  selector: 'app-oportunidad-list',
  standalone: true,
  imports: [CommonModule, MatTableModule, MatButtonModule, MatIconModule, RouterLink, MatProgressSpinnerModule],
  templateUrl: './oportunidad-list.html',
  styleUrl: './oportunidad-list.css'
})
export class OportunidadList implements OnInit {
  private oportunidadesService = inject(OportunidadesService);
  private router = inject(Router);

  oportunidades = signal<OportunidadResponse[]>([]);
  displayedColumns: string[] = ['id', 'nombre', 'clienteNombre', 'valor', 'probabilidad', 'etapa', 'acciones'];
  loading = signal(false);
  error = signal('');

  hasOportunidades = computed(() => this.oportunidades().length > 0 && !this.loading());
  isEmpty = computed(() => !this.loading() && this.oportunidades().length === 0 && !this.error());

  ngOnInit() {
    this.cargarOportunidades();
  }

  cargarOportunidades() {
    this.loading.set(true);
    this.error.set('');
    this.oportunidadesService.listar().subscribe({
      next: (data) => {
        this.oportunidades.set(data || []);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Error al cargar oportunidades.');
        this.loading.set(false);
      }
    });
  }

  eliminar(id: number) {
    if (confirm('¿Está seguro de eliminar esta oportunidad?')) {
      this.oportunidadesService.eliminar(id).subscribe({
        next: () => this.cargarOportunidades(),
        error: () => alert('Error al eliminar')
      });
    }
  }

  editar(id: number) {
    this.router.navigate(['/oportunidades', id, 'editar']);
  }
}

