import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ProductosService } from '../productos.service';
import { ProductoResponse, EstadoProducto } from '../producto.model';

@Component({
  selector: 'app-producto-list',
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
  templateUrl: './producto-list.html',
  styleUrl: './producto-list.css'
})
export class ProductoList implements OnInit {
  private productosService = inject(ProductosService);
  private router = inject(Router);

  productos = signal<ProductoResponse[]>([]);
  filtroTexto = signal('');
  filtroEstado = signal<string>('TODOS');
  loading = signal(false);
  error = signal('');

  estados = Object.values(EstadoProducto);
  displayedColumns: string[] = ['id', 'codigo', 'nombre', 'categoria', 'precioBase', 'estado', 'acciones'];

  productosFiltrados = computed(() => {
    const q = this.filtroTexto().toLowerCase().trim();
    const est = this.filtroEstado();
    return this.productos().filter(producto => {
      const matchText = !q ||
        producto.nombre?.toLowerCase().includes(q) ||
        producto.codigo?.toLowerCase().includes(q) ||
        producto.categoria?.toLowerCase().includes(q);
      const matchEstado = est === 'TODOS' || producto.estado === est;
      return matchText && matchEstado;
    });
  });

  hasProductos = computed(() => this.productosFiltrados().length > 0 && !this.loading());
  isEmpty = computed(() => !this.loading() && this.productosFiltrados().length === 0 && !this.error());

  ngOnInit() {
    this.cargarProductos();
  }

  cargarProductos() {
    this.loading.set(true);
    this.error.set('');
    this.productosService.listar().subscribe({
      next: (data) => {
        this.productos.set(data || []);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Error al cargar productos. Verifique que el backend esté disponible.');
        this.loading.set(false);
      }
    });
  }

  getEstadoClass(estado: EstadoProducto | string): string {
    switch (estado) {
      case 'ACTIVO': return 'success';
      case 'INACTIVO': return 'neutral';
      case 'DESCONTINUADO': return 'danger';
      default: return 'neutral';
    }
  }

  formatearPrecio(valor: number): string {
    return new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' }).format(valor || 0);
  }

  eliminar(id: number) {
    if (confirm('¿Está seguro de eliminar este producto?')) {
      this.productosService.eliminar(id).subscribe({
        next: () => this.cargarProductos(),
        error: (err) => alert(err?.friendlyMessage || 'Error al eliminar')
      });
    }
  }

  editar(id: number) {
    this.router.navigate(['/productos', id, 'editar']);
  }
}
