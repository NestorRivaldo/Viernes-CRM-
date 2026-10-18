import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { UsuariosService } from '../usuarios.service';
import { RolUsuario, UsuarioRequest } from '../usuarios.model';

@Component({
  selector: 'app-usuario-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatCardModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatButtonModule, RouterLink],
  templateUrl: './usuario-form.html',
  styleUrl: './usuario-form.css'
})
export class UsuarioForm implements OnInit {
  form: FormGroup;
  isEdit = false;
  id!: number;
  loading = false;
  roles = Object.values(RolUsuario);

  constructor(
    private fb: FormBuilder,
    private usuariosService: UsuariosService,
    private route: ActivatedRoute,
    private router: Router
  ) {
    this.form = this.fb.group({
      nombre: ['', [Validators.required, Validators.maxLength(100)]],
      apellido: ['', [Validators.required, Validators.maxLength(100)]],
      correo: ['', [Validators.required, Validators.email, Validators.maxLength(150)]],
      password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(100)]],
      rol: [RolUsuario.VENDEDOR, Validators.required]
    });
  }

  ngOnInit() {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.isEdit = true;
      this.id = +idParam;
      // En edición la contraseña es opcional: vacía = mantener la actual
      this.form.get('password')?.setValidators([Validators.minLength(8), Validators.maxLength(100)]);
      this.form.get('password')?.updateValueAndValidity();
      this.cargarUsuario();
    }
  }

  cargarUsuario() {
    this.loading = true;
    this.usuariosService.obtener(this.id).subscribe({
      next: (usuario) => {
        this.form.patchValue(usuario);
        this.loading = false;
      },
      error: (err) => {
        alert(err?.friendlyMessage || 'Error al cargar el usuario');
        this.router.navigate(['/usuarios']);
      }
    });
  }

  guardar() {
    if (this.form.invalid) return;

    this.loading = true;
    const request: UsuarioRequest = { ...this.form.value };
    if (this.isEdit && !request.password) {
      delete request.password;
    }

    const req$ = this.isEdit
      ? this.usuariosService.actualizar(this.id, request)
      : this.usuariosService.crear(request);

    req$.subscribe({
      next: () => {
        this.router.navigate(['/usuarios']);
      },
      error: (err) => {
        alert(err?.friendlyMessage || 'Error al guardar el usuario');
        this.loading = false;
      }
    });
  }
}
