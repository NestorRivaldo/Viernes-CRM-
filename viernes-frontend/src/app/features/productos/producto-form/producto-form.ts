import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { ProductosService } from '../productos.service';
import { EstadoProducto } from '../producto.model';

@Component({
  selector: 'app-producto-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatCardModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatButtonModule, RouterLink],
  templateUrl: './producto-form.html',
  styleUrl: './producto-form.css'
})
export class ProductoForm implements OnInit {
  form: FormGroup;
  isEdit = false;
  id!: number;
  loading = false;
  estados = Object.values(EstadoProducto);

  constructor(
    private fb: FormBuilder,
    private productosService: ProductosService,
    private route: ActivatedRoute,
    private router: Router
  ) {
    this.form = this.fb.group({
      codigo: ['', [Validators.required, Validators.maxLength(50)]],
      nombre: ['', [Validators.required, Validators.maxLength(200)]],
      descripcion: ['', Validators.maxLength(1000)],
      categoria: ['', Validators.maxLength(100)],
      precioBase: [null, [Validators.required, Validators.min(0)]],
      estado: [EstadoProducto.ACTIVO, Validators.required]
    });
  }

  ngOnInit() {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.isEdit = true;
      this.id = +idParam;
      this.cargarProducto();
    }
  }

  cargarProducto() {
    this.loading = true;
    this.productosService.obtener(this.id).subscribe({
      next: (producto) => {
        this.form.patchValue(producto);
        this.loading = false;
      },
      error: (err) => {
        alert(err?.friendlyMessage || 'Error al cargar el producto');
        this.router.navigate(['/productos']);
      }
    });
  }

  guardar() {
    if (this.form.invalid) return;

    this.loading = true;
    const request = this.form.value;

    const req$ = this.isEdit
      ? this.productosService.actualizar(this.id, request)
      : this.productosService.crear(request);

    req$.subscribe({
      next: () => {
        this.router.navigate(['/productos']);
      },
      error: (err) => {
        alert(err?.friendlyMessage || 'Error al guardar el producto');
        this.loading = false;
      }
    });
  }
}
