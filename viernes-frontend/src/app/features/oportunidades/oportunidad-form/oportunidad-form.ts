import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { OportunidadesService } from '../oportunidades.service';
import { EtapaOportunidad } from '../oportunidades.model';

@Component({
  selector: 'app-oportunidad-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatCardModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatButtonModule, RouterLink],
  templateUrl: './oportunidad-form.html',
  styleUrl: './oportunidad-form.css'
})
export class OportunidadForm implements OnInit {
  form: FormGroup;
  isEdit = false;
  id!: number;
  loading = false;
  etapas = Object.values(EtapaOportunidad);

  constructor(
    private fb: FormBuilder,
    private oportunidadesService: OportunidadesService,
    private route: ActivatedRoute,
    private router: Router
  ) {
    this.form = this.fb.group({
      nombre: ['', [Validators.required, Validators.maxLength(200)]],
      clienteNombre: ['', Validators.maxLength(150)],
      valor: [0, Validators.min(0)],
      probabilidad: [0, [Validators.min(0), Validators.max(100)]],
      etapa: [EtapaOportunidad.PROSPECCION],
      responsable: ['', Validators.maxLength(100)],
      fechaCierre: [''],
      descripcion: ['', Validators.maxLength(500)]
    });
  }

  ngOnInit() {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.isEdit = true;
      this.id = +idParam;
      this.cargarOportunidad();
    }
  }

  cargarOportunidad() {
    this.loading = true;
    this.oportunidadesService.obtener(this.id).subscribe({
      next: (op) => {
        this.form.patchValue(op);
        this.loading = false;
      },
      error: () => {
        alert('Error al cargar oportunidad');
        this.router.navigate(['/oportunidades']);
      }
    });
  }

  guardar() {
    if (this.form.invalid) return;

    this.loading = true;
    const request = this.form.value;

    const req$ = this.isEdit 
      ? this.oportunidadesService.actualizar(this.id, request)
      : this.oportunidadesService.crear(request);

    req$.subscribe({
      next: () => {
        this.router.navigate(['/oportunidades']);
      },
      error: () => {
        alert('Error al guardar oportunidad');
        this.loading = false;
      }
    });
  }
}
