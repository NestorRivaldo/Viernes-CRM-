import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { ClientesService } from '../clientes.service';
import { ClienteResponse } from '../clientes.model';
import { ClienteDialog, ClienteDialogResult } from '../cliente-dialog/cliente-dialog';
import { sortRows } from '../../../shared/table-sort';

@Component({
  selector: 'app-cliente-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatTableModule,
    MatSortModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatDialogModule
  ],
  templateUrl: './cliente-list.html',
  styleUrl: './cliente-list.css'
})
export class ClienteList implements OnInit {
  private clientesService = inject(ClientesService);
  private dialog = inject(MatDialog);
  private router = inject(Router);

  clientes = signal<ClienteResponse[]>([]);
  filtroTexto = signal('');
  filtroEstado = signal<string>('TODOS');
  loading = signal(false);
  error = signal('');

  displayedColumns: string[] = ['id', 'nombre', 'empresa', 'documento', 'correo', 'estado', 'acciones'];
  sortState = signal<Sort>({ active: 'id', direction: 'asc' });

  clientesFiltrados = computed(() => {
    const q = this.filtroTexto().toLowerCase().trim();
    const est = this.filtroEstado();
    return this.clientes().filter(c => {
      const matchText = !q || 
        c.nombre?.toLowerCase().includes(q) || 
        c.empresa?.toLowerCase().includes(q) || 
        c.correo?.toLowerCase().includes(q) ||
        c.numeroDocumento?.toLowerCase().includes(q) ||
        c.tipoDocumento?.toLowerCase().includes(q);
      const matchEstado = est === 'TODOS' || c.estado === est;
      return matchText && matchEstado;
    });
  });

  clientesOrdenados = computed(() => sortRows(this.clientesFiltrados(), this.sortState()));

  onSortChange(sort: Sort) {
    this.sortState.set(sort);
  }

  documentoCliente(cliente: ClienteResponse): string {
    if (!cliente.numeroDocumento) return '-';
    return `${cliente.tipoDocumento || 'DOC'} ${cliente.numeroDocumento}`;
  }

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
    this.abrirDialogo(id);
  }

  verMapa(id: number) {
    this.router.navigate(['/relaciones/clientes', id]);
  }

  nuevoCliente() {
    this.abrirDialogo();
  }

  private abrirDialogo(id?: number) {
    this.dialog
      .open(ClienteDialog, {
        data: { id: id ?? null },
        panelClass: 'opp-dialog-panel',
        backdropClass: 'opp-dialog-backdrop',
        width: '720px',
        maxWidth: '95vw',
        disableClose: false,
        autoFocus: false,
      })
      .afterClosed()
      .subscribe((result: ClienteDialogResult | undefined) => {
        if (result?.saved) {
          this.cargarClientes();
        }
      });
  }
}
