import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ClientesService } from '../clientes.service';
import { ClienteResponse } from '../clientes.model';

@Component({
  selector: 'app-cliente-list',
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
  templateUrl: './cliente-list.html',
  styleUrl: './cliente-list.css'
})
export class ClienteList implements OnInit {
  private clientesService = inject(ClientesService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  clientes = signal<ClienteResponse[]>([]);
  filtroTexto = signal('');
  filtroEstado = signal<string>('TODOS');
  loading = signal(false);
  error = signal('');

  displayedColumns: string[] = ['id', 'nombre', 'empresa', 'correo', 'estado', 'acciones'];

  clientesFiltrados = computed(() => {
    const q = this.filtroTexto().toLowerCase().trim();
    const est = this.filtroEstado();
    return this.clientes().filter(c => {
      const matchText = !q || 
        c.nombre?.toLowerCase().includes(q) || 
        c.empresa?.toLowerCase().includes(q) || 
        c.correo?.toLowerCase().includes(q);
      const matchEstado = est === 'TODOS' || c.estado === est;
      return matchText && matchEstado;
    });
  });

  hasClientes = computed(() => this.clientesFiltrados().length > 0 && !this.loading());
  isEmpty = computed(() => !this.loading() && this.clientesFiltrados().length === 0 && !this.error());

  ngOnInit() {
    this.cargarClientes();
  }

  cargarClientes() {
    this.loading.set(true);
    this.error.set('');
    this.clientesService.listar().subscribe({
      next: (data) => {
        this.clientes.set(data || []);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Error al cargar clientes. Verifique que el backend esté disponible.');
        this.loading.set(false);
      }
    });
  }

  eliminar(id: number) {
    if (confirm('¿Está seguro de eliminar este cliente?')) {
      this.clientesService.eliminar(id).subscribe({
        next: () => this.cargarClientes(),
        error: (err) => alert(err?.friendlyMessage || 'Error al eliminar')
      });
    }
  }

  desactivar(cliente: ClienteResponse) {
    if (confirm(`¿Está seguro de desactivar al cliente ${cliente.nombre}?`)) {
      this.clientesService.desactivar(cliente.id).subscribe({
        next: () => this.cargarClientes(),
        error: (err) => alert(err?.friendlyMessage || 'Error al desactivar')
      });
    }
  }

  editar(id: number) {
    this.router.navigate(['/clientes', id, 'editar']);
  }
}
