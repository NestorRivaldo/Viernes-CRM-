import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { ActividadesService } from '../actividades.service';
import { TipoActividad, EstadoActividad } from '../actividades.model';

@Component({
  selector: 'app-actividad-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatCardModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatButtonModule, RouterLink],
  templateUrl: './actividad-form.html',
  styleUrl: './actividad-form.css'
})
export class ActividadForm implements OnInit {
  form: FormGroup;
  isEdit = false;
  id!: number;
  loading = false;
  tipos = Object.values(TipoActividad);
  estados = Object.values(EstadoActividad);

  constructor(
    private fb: FormBuilder,
    private actividadesService: ActividadesService,
    private route: ActivatedRoute,
    private router: Router
  ) {
    this.form = this.fb.group({
      titulo: ['', [Validators.required]],
      descripcion: [''],
      tipo: [TipoActividad.TAREA, Validators.required],
      estado: [EstadoActividad.PENDIENTE],
      fecha: ['', Validators.required],
      idCliente: [null],
      idLead: [null],
      idOportunidad: [null]
    });
  }

  ngOnInit() {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.isEdit = true;
      this.id = +idParam;
      this.cargarActividad();
    }
  }

  cargarActividad() {
    this.loading = true;
    this.actividadesService.obtener(this.id).subscribe({
      next: (act) => {
        // Simple mapping for HTML datetime-local (yyyy-MM-ddThh:mm)
        const fechaFormat = act.fecha ? new Date(act.fecha).toISOString().slice(0, 16) : '';
        this.form.patchValue({ ...act, fecha: fechaFormat });
        this.loading = false;
      },
      error: () => {
        alert('Error al cargar actividad');
        this.router.navigate(['/actividades']);
      }
    });
  }

  guardar() {
    if (this.form.invalid) return;

    this.loading = true;
    const request = this.form.value;

    const req$ = this.isEdit 
      ? this.actividadesService.actualizar(this.id, request)
      : this.actividadesService.crear(request);

    req$.subscribe({
      next: () => {
        this.router.navigate(['/actividades']);
      },
      error: () => {
        alert('Error al guardar actividad');
        this.loading = false;
      }
    });
  }
}
