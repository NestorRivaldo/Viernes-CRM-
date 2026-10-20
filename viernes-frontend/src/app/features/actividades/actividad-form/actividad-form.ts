import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { ActividadesService } from '../actividades.service';
import { TipoActividad, EstadoActividad } from '../actividades.model';
import { ClientesService } from '../../clientes/clientes.service';
import { LeadsService } from '../../leads/leads.service';
import { OportunidadesService } from '../../oportunidades/oportunidades.service';
import { ClienteResponse } from '../../clientes/clientes.model';
import { LeadResponse } from '../../leads/leads.model';
import { OportunidadResponse } from '../../oportunidades/oportunidades.model';

@Component({
  selector: 'app-actividad-form',
  standalone: true,
  imports: [
    CommonModule, 
    ReactiveFormsModule, 
    MatIconModule,
    RouterLink
  ],
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

  // Catálogos relacionados
  clientes: ClienteResponse[] = [];
  leads: LeadResponse[] = [];
  oportunidades: OportunidadResponse[] = [];
  cargandoRelaciones = false;

  constructor(
    private fb: FormBuilder,
    private actividadesService: ActividadesService,
    private clientesService: ClientesService,
    private leadsService: LeadsService,
    private oportunidadesService: OportunidadesService,
    private route: ActivatedRoute,
    private router: Router
  ) {
    const today = new Date();
    const defaultDate = this.formatDate(today);
    const defaultTime = this.formatTime(new Date(today.getTime() + 60 * 60 * 1000)); // +1h

    this.form = this.fb.group({
      titulo: ['', [Validators.required]],
      descripcion: [''],
      tipo: [TipoActividad.TAREA, Validators.required],
      estado: [EstadoActividad.PENDIENTE, Validators.required],
      fechaDia: [defaultDate, Validators.required],
      fechaHora: [defaultTime, Validators.required],
      vinculacionTipo: ['NINGUNO'], // 'NINGUNO' | 'CLIENTE' | 'LEAD' | 'OPORTUNIDAD'
      idCliente: [null],
      idLead: [null],
      idOportunidad: [null]
    });
  }

  ngOnInit() {
    this.cargarCatalogos();

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.isEdit = true;
      this.id = +idParam;
      this.cargarActividad();
    }
  }

  private formatDate(d: Date): string {
    const pad = (n: number) => n.toString().padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
  }

  private formatTime(d: Date): string {
    const pad = (n: number) => n.toString().padStart(2, '0');
    return `${pad(d.getHours())}:${pad(d.getMinutes())}`;
  }

  cargarCatalogos() {
    this.cargandoRelaciones = true;
    
    this.clientesService.listar().subscribe({
      next: (data) => this.clientes = data,
      error: () => console.warn('No se pudieron cargar clientes')
    });

    this.leadsService.listar().subscribe({
      next: (data) => this.leads = data,
      error: () => console.warn('No se pudieron cargar leads')
    });

    this.oportunidadesService.listar().subscribe({
      next: (data) => {
        this.oportunidades = data;
        this.cargandoRelaciones = false;
      },
      error: () => {
        this.cargandoRelaciones = false;
        console.warn('No se pudieron cargar oportunidades');
      }
    });
  }

  setTipo(tipo: TipoActividad) {
    this.form.patchValue({ tipo });
  }

  setEstado(estado: EstadoActividad) {
    this.form.patchValue({ estado });
  }

  setVinculacionTipo(tipo: 'NINGUNO' | 'CLIENTE' | 'LEAD' | 'OPORTUNIDAD') {
    this.form.patchValue({ vinculacionTipo: tipo });
    if (tipo !== 'CLIENTE') this.form.patchValue({ idCliente: null });
    if (tipo !== 'LEAD') this.form.patchValue({ idLead: null });
    if (tipo !== 'OPORTUNIDAD') this.form.patchValue({ idOportunidad: null });
  }

  setDatePreset(preset: 'hoy' | 'manana' | 'en_2dias' | 'proxima_semana') {
    const d = new Date();
    if (preset === 'hoy') {
      // hoy
    } else if (preset === 'manana') {
      d.setDate(d.getDate() + 1);
    } else if (preset === 'en_2dias') {
      d.setDate(d.getDate() + 2);
    } else if (preset === 'proxima_semana') {
      const dayDiff = ((1 + 7 - d.getDay()) % 7) || 7;
      d.setDate(d.getDate() + dayDiff);
    }
    this.form.patchValue({ fechaDia: this.formatDate(d) });
    this.form.get('fechaDia')?.markAsDirty();
  }

  setTimePreset(timeStr: string) {
    this.form.patchValue({ fechaHora: timeStr });
    this.form.get('fechaHora')?.markAsDirty();
  }

  cargarActividad() {
    this.loading = true;
    this.actividadesService.obtener(this.id).subscribe({
      next: (act) => {
        let dia = '';
        let hora = '';
        if (act.fecha) {
          const parts = act.fecha.split('T');
          dia = parts[0];
          if (parts[1]) {
            hora = parts[1].slice(0, 5);
          }
        }

        let tipoVinc: 'NINGUNO' | 'CLIENTE' | 'LEAD' | 'OPORTUNIDAD' = 'NINGUNO';
        if (act.idCliente) tipoVinc = 'CLIENTE';
        else if (act.idLead) tipoVinc = 'LEAD';
        else if (act.idOportunidad) tipoVinc = 'OPORTUNIDAD';

        this.form.patchValue({
          titulo: act.titulo,
          descripcion: act.descripcion,
          tipo: act.tipo,
          estado: act.estado,
          fechaDia: dia || this.formatDate(new Date()),
          fechaHora: hora || '10:00',
          vinculacionTipo: tipoVinc,
          idCliente: act.idCliente || null,
          idLead: act.idLead || null,
          idOportunidad: act.idOportunidad || null
        });
        this.loading = false;
      },
      error: () => {
        alert('Error al cargar la actividad seleccionada.');
        this.router.navigate(['/actividades']);
      }
    });
  }

  guardar() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading = true;
    const val = this.form.value;

    // Combinar fechaDia y fechaHora en ISO compatible con Spring Boot LocalDateTime
    const fechaISO = `${val.fechaDia}T${val.fechaHora}:00`;

    const request: any = {
      titulo: val.titulo,
      descripcion: val.descripcion,
      tipo: val.tipo,
      estado: val.estado,
      fecha: fechaISO,
      idCliente: val.vinculacionTipo === 'CLIENTE' && val.idCliente ? Number(val.idCliente) : null,
      idLead: val.vinculacionTipo === 'LEAD' && val.idLead ? Number(val.idLead) : null,
      idOportunidad: val.vinculacionTipo === 'OPORTUNIDAD' && val.idOportunidad ? Number(val.idOportunidad) : null
    };

    const req$ = this.isEdit 
      ? this.actividadesService.actualizar(this.id, request)
      : this.actividadesService.crear(request);

    req$.subscribe({
      next: () => {
        this.router.navigate(['/actividades']);
      },
      error: (err) => {
        console.error('Error al guardar actividad:', err);
        alert('Error al guardar la actividad. Verifique que los campos obligatorios estén completos.');
        this.loading = false;
      }
    });
  }
}
