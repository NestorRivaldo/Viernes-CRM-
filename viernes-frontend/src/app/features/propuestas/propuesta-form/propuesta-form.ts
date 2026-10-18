import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormArray, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { PropuestasService } from '../propuestas.service';
import { EstadoPropuesta } from '../propuestas.model';

@Component({
  selector: 'app-propuesta-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatCardModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule, RouterLink],
  templateUrl: './propuesta-form.html',
  styleUrl: './propuesta-form.css'
})
export class PropuestaForm implements OnInit {
  form: FormGroup;
  isEdit = false;
  id!: number;
  loading = false;
  estadoActual: EstadoPropuesta = EstadoPropuesta.BORRADOR;

  constructor(
    private fb: FormBuilder,
    private propuestasService: PropuestasService,
    private route: ActivatedRoute,
    private router: Router
  ) {
    this.form = this.fb.group({
      idOportunidad: [null, Validators.required],
      idCliente: [null, Validators.required],
      clienteNombre: [''],
      responsable: [''],
      fechaEmision: ['', Validators.required],
      fechaVencimiento: [''],
      descuento: [0, Validators.min(0)],
      observaciones: [''],
      detalles: this.fb.array([])
    });
    // Agregamos un detalle por defecto
    this.agregarDetalle();
  }

  get detalles() {
    return this.form.controls['detalles'] as FormArray;
  }

  get totalEstimado(): number {
    const subtotal = this.detalles.controls.reduce((acc, d) => {
      const cantidad = Number(d.get('cantidad')?.value) || 0;
      const precio = Number(d.get('precioUnitario')?.value) || 0;
      return acc + cantidad * precio;
    }, 0);
    const descuento = Number(this.form.get('descuento')?.value) || 0;
    return subtotal - descuento;
  }

  agregarDetalle() {
    const detalleForm = this.fb.group({
      descripcion: ['', Validators.required],
      cantidad: [1, [Validators.required, Validators.min(1)]],
      precioUnitario: [0, [Validators.required, Validators.min(0)]]
    });
    this.detalles.push(detalleForm);
  }

  eliminarDetalle(index: number) {
    if (this.detalles.length > 1) {
      this.detalles.removeAt(index);
    } else {
      alert('Debe haber al menos un detalle');
    }
  }

  ngOnInit() {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.isEdit = true;
      this.id = +idParam;
      this.cargarPropuesta();
    }
  }

  cargarPropuesta() {
    this.loading = true;
    this.propuestasService.obtener(this.id).subscribe({
      next: (p) => {
        this.estadoActual = p.estado;
        
        // Limpiamos detalles por defecto
        while (this.detalles.length !== 0) {
          this.detalles.removeAt(0);
        }
        
        // Cargamos los detalles reales
        if (p.detalles && p.detalles.length > 0) {
          p.detalles.forEach(d => {
            this.detalles.push(this.fb.group({
              descripcion: [d.descripcion, Validators.required],
              cantidad: [d.cantidad, [Validators.required, Validators.min(1)]],
              precioUnitario: [d.precioUnitario, [Validators.required, Validators.min(0)]]
            }));
          });
        }
        
        this.form.patchValue(p);
        
        // Deshabilitar formulario si no es borrador
        if (this.estadoActual !== EstadoPropuesta.BORRADOR) {
          this.form.disable();
        }
        
        this.loading = false;
      },
      error: (err) => {
        alert(err?.friendlyMessage || 'Error al cargar propuesta');
        this.router.navigate(['/propuestas']);
      }
    });
  }

  guardar() {
    if (this.form.invalid) {
      alert('Revisa los campos obligatorios');
      return;
    }
    
    this.loading = true;
    const request = this.form.getRawValue();

    const req$ = this.isEdit 
      ? this.propuestasService.actualizar(this.id, request)
      : this.propuestasService.crear(request);

    req$.subscribe({
      next: () => {
        this.router.navigate(['/propuestas']);
      },
      error: (err) => {
        alert(err?.friendlyMessage || 'Error al guardar propuesta');
        this.loading = false;
      }
    });
  }
  
  cambiarEstado(accion: 'enviar' | 'aprobar' | 'rechazar' | 'cancelar') {
    if (!this.id) return;
    
    this.loading = true;
    let req$;
    switch(accion) {
      case 'enviar': req$ = this.propuestasService.enviar(this.id); break;
      case 'aprobar': req$ = this.propuestasService.aprobar(this.id); break;
      case 'rechazar': req$ = this.propuestasService.rechazar(this.id); break;
      case 'cancelar': req$ = this.propuestasService.cancelar(this.id); break;
    }
    
    req$.subscribe({
      next: () => this.cargarPropuesta(),
      error: (err) => {
        alert(err?.friendlyMessage || 'Error al cambiar de estado');
        this.loading = false;
      }
    });
  }
}
