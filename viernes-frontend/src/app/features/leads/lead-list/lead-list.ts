import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { LeadsService } from '../leads.service';
import { LeadResponse } from '../leads.model';

@Component({
  selector: 'app-lead-list',
  standalone: true,
  imports: [CommonModule, MatTableModule, MatButtonModule, MatIconModule, RouterLink, MatProgressSpinnerModule],
  templateUrl: './lead-list.html',
  styleUrl: './lead-list.css'
})
export class LeadList implements OnInit {
  private leadsService = inject(LeadsService);
  private router = inject(Router);

  leads = signal<LeadResponse[]>([]);
  displayedColumns: string[] = ['id', 'nombre', 'empresa', 'estado', 'fuente', 'acciones'];
  loading = signal(false);
  error = signal('');

  hasLeads = computed(() => this.leads().length > 0 && !this.loading());
  isEmpty = computed(() => !this.loading() && this.leads().length === 0 && !this.error());

  ngOnInit() {
    this.cargarLeads();
  }

  cargarLeads() {
    this.loading.set(true);
    this.error.set('');
    this.leadsService.listar().subscribe({
      next: (data) => {
        this.leads.set(data || []);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Error al cargar leads.');
        this.loading.set(false);
      }
    });
  }

  eliminar(id: number) {
    if (confirm('¿Está seguro de eliminar este lead?')) {
      this.leadsService.eliminar(id).subscribe({
        next: () => this.cargarLeads(),
        error: () => alert('Error al eliminar')
      });
    }
  }

  editar(id: number) {
    this.router.navigate(['/leads', id, 'editar']);
  }
}

