import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { LeadsService } from '../leads.service';
import { LeadResponse, EstadoLead } from '../leads.model';

@Component({
  selector: 'app-lead-list',
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
  templateUrl: './lead-list.html',
  styleUrl: './lead-list.css'
})
export class LeadList implements OnInit {
  private leadsService = inject(LeadsService);
  private router = inject(Router);

  leads = signal<LeadResponse[]>([]);
  filtroTexto = signal('');
  filtroEstado = signal<string>('TODOS');
  loading = signal(false);
  error = signal('');

  displayedColumns: string[] = ['id', 'nombre', 'empresa', 'estado', 'fuente', 'acciones'];

  leadsFiltrados = computed(() => {
    const q = this.filtroTexto().toLowerCase().trim();
    const est = this.filtroEstado();
    return this.leads().filter(lead => {
      const matchText = !q || 
        lead.nombre?.toLowerCase().includes(q) || 
        lead.empresa?.toLowerCase().includes(q) || 
        lead.correo?.toLowerCase().includes(q);
      const matchEstado = est === 'TODOS' || lead.estado === est;
      return matchText && matchEstado;
    });
  });

  hasLeads = computed(() => this.leadsFiltrados().length > 0 && !this.loading());
  isEmpty = computed(() => !this.loading() && this.leadsFiltrados().length === 0 && !this.error());

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
        this.error.set('Error al cargar leads. Verifique que el backend esté disponible.');
        this.loading.set(false);
      }
    });
  }

  getEstadoClass(estado: EstadoLead | string): string {
    switch (estado) {
      case 'NUEVO': return 'blue';
      case 'CONTACTADO': return 'purple';
      case 'CALIFICADO': return 'success';
      case 'DESCARTADO': return 'danger';
      case 'CONVERTIDO': return 'emerald';
      default: return 'neutral';
    }
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
