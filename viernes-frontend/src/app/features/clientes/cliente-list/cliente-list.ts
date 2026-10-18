import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { ClientesService } from '../clientes.service';
import { ClienteResponse } from '../clientes.model';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

@Component({
  selector: 'app-cliente-list',
  standalone: true,
  imports: [CommonModule, MatTableModule, MatButtonModule, MatIconModule, RouterLink, MatProgressSpinnerModule],
  templateUrl: './cliente-list.html',
  styleUrl: './cliente-list.css'
})
export class ClienteList implements OnInit {
  private clientesService = inject(ClientesService);
  private router = inject(Router);

  clientes = signal<ClienteResponse[]>([]);
  displayedColumns: string[] = ['id', 'nombre', 'empresa', 'correo', 'estado', 'acciones'];
  loading = signal(false);
  error = signal('');

  hasClientes = computed(() => this.clientes().length > 0 && !this.loading());
  isEmpty = computed(() => !this.loading() && this.clientes().length === 0 && !this.error());

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
        this.error.set('Error al cargar clientes.');
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
        error: (err) => alert(err?.friendlyMessage || 'Error al desactivar el cliente')
      });
    }
  }

  editar(id: number) {
    this.router.navigate(['/clientes', id, 'editar']);
  }
}

