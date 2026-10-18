import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ClientesService } from '../clientes.service';
import { ContactoResponse, EstadoCliente } from '../clientes.model';

@Component({
  selector: 'app-cliente-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatCardModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatButtonModule, MatIconModule, MatTableModule, MatCheckboxModule, MatProgressSpinnerModule, RouterLink],
  templateUrl: './cliente-form.html',
  styleUrl: './cliente-form.css'
})
export class ClienteForm implements OnInit {
  form: FormGroup;
  isEdit = false;
  id!: number;
  loading = false;
  estados = Object.values(EstadoCliente);

  contactos: ContactoResponse[] = [];
  contactoForm: FormGroup;
  contactosLoading = false;
  contactosError = '';
  editandoContactoId: number | null = null;
  contactosColumns: string[] = ['nombre', 'cargo', 'correo', 'telefono', 'principal', 'acciones'];

  constructor(
    private fb: FormBuilder,
    private clientesService: ClientesService,
    private route: ActivatedRoute,
    private router: Router
  ) {
    this.form = this.fb.group({
      nombre: ['', [Validators.required, Validators.maxLength(150)]],
      empresa: ['', Validators.maxLength(150)],
      correo: ['', [Validators.required, Validators.email, Validators.maxLength(150)]],
      telefono: ['', Validators.maxLength(30)],
      estado: [EstadoCliente.ACTIVO, Validators.required],
      responsable: ['', Validators.maxLength(100)]
    });

    this.contactoForm = this.fb.group({
      nombre: ['', [Validators.required, Validators.maxLength(100)]],
      apellido: ['', Validators.maxLength(100)],
      cargo: ['', Validators.maxLength(100)],
      correo: ['', [Validators.email, Validators.maxLength(150)]],
      telefono: ['', Validators.maxLength(30)],
      principal: [false]
    });
  }

  ngOnInit() {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.isEdit = true;
      this.id = +idParam;
      this.cargarCliente();
      this.cargarContactos();
    }
  }

  cargarCliente() {
    this.loading = true;
    this.clientesService.obtener(this.id).subscribe({
      next: (cliente) => {
        this.form.patchValue(cliente);
        this.loading = false;
      },
      error: (err) => {
        alert(err?.friendlyMessage || 'Error al cargar el cliente');
        this.router.navigate(['/clientes']);
      }
    });
  }

  guardar() {
    if (this.form.invalid) return;

    this.loading = true;
    const request = this.form.value;

    const req$ = this.isEdit
      ? this.clientesService.actualizar(this.id, request)
      : this.clientesService.crear(request);

    req$.subscribe({
      next: (cliente) => {
        if (this.isEdit) {
          this.router.navigate(['/clientes']);
        } else {
          // Permitir agregar contactos inmediatamente después de crear
          this.router.navigate(['/clientes', cliente.id, 'editar']);
        }
      },
      error: (err) => {
        alert(err?.friendlyMessage || 'Error al guardar el cliente');
        this.loading = false;
      }
    });
  }

  cargarContactos() {
    this.contactosLoading = true;
    this.contactosError = '';
    this.clientesService.listarContactos(this.id).subscribe({
      next: (data) => {
        this.contactos = data;
        this.contactosLoading = false;
      },
      error: () => {
        this.contactosError = 'Error al cargar contactos.';
        this.contactosLoading = false;
      }
    });
  }

  guardarContacto() {
    if (this.contactoForm.invalid) return;

    this.contactosLoading = true;
    const request = this.contactoForm.value;

    const req$ = this.editandoContactoId
      ? this.clientesService.actualizarContacto(this.id, this.editandoContactoId, request)
      : this.clientesService.agregarContacto(this.id, request);

    req$.subscribe({
      next: () => {
        this.cancelarEdicionContacto();
        this.cargarContactos();
      },
      error: (err) => {
        alert(err?.friendlyMessage || 'Error al guardar el contacto');
        this.contactosLoading = false;
      }
    });
  }

  editarContacto(contacto: ContactoResponse) {
    this.editandoContactoId = contacto.id;
    this.contactoForm.patchValue(contacto);
  }

  cancelarEdicionContacto() {
    this.editandoContactoId = null;
    this.contactoForm.reset({ principal: false });
  }

  eliminarContacto(contactoId: number) {
    if (confirm('¿Está seguro de eliminar este contacto?')) {
      this.clientesService.eliminarContacto(this.id, contactoId).subscribe({
        next: () => this.cargarContactos(),
        error: (err) => alert(err?.friendlyMessage || 'Error al eliminar el contacto')
      });
    }
  }
}
