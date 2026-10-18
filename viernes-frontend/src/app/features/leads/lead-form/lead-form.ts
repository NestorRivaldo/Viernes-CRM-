import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { LeadsService } from '../leads.service';
import { EstadoLead } from '../leads.model';

@Component({
  selector: 'app-lead-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatCardModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatButtonModule, RouterLink],
  templateUrl: './lead-form.html',
  styleUrl: './lead-form.css'
})
export class LeadForm implements OnInit {
  form: FormGroup;
  isEdit = false;
  id!: number;
  loading = false;
  estados = Object.values(EstadoLead);

  constructor(
    private fb: FormBuilder,
    private leadsService: LeadsService,
    private route: ActivatedRoute,
    private router: Router
  ) {
    this.form = this.fb.group({
      nombre: ['', [Validators.required, Validators.maxLength(150)]],
      empresa: ['', Validators.maxLength(150)],
      correo: ['', [Validators.required, Validators.email, Validators.maxLength(150)]],
      telefono: ['', Validators.maxLength(30)],
      fuente: ['', Validators.maxLength(80)],
      estado: [EstadoLead.NUEVO, Validators.required],
      responsable: ['', Validators.maxLength(100)],
      fecha: ['']
    });
  }

  ngOnInit() {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.isEdit = true;
      this.id = +idParam;
      this.cargarLead();
    }
  }

  cargarLead() {
    this.loading = true;
    this.leadsService.obtener(this.id).subscribe({
      next: (lead) => {
        this.form.patchValue(lead);
        this.loading = false;
      },
      error: () => {
        alert('Error al cargar el lead');
        this.router.navigate(['/leads']);
      }
    });
  }

  guardar() {
    if (this.form.invalid) return;

    this.loading = true;
    const request = this.form.value;

    const req$ = this.isEdit 
      ? this.leadsService.actualizar(this.id, request)
      : this.leadsService.crear(request);

    req$.subscribe({
      next: () => {
        this.router.navigate(['/leads']);
      },
      error: () => {
        alert('Error al guardar el lead');
        this.loading = false;
      }
    });
  }
}
