import { ChangeDetectorRef, Component, DestroyRef, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { catchError, finalize, forkJoin, of } from 'rxjs';
import { debounceTime } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActividadesService } from '../actividades.service';
import { ActividadRequest, TipoActividad, EstadoActividad } from '../actividades.model';
import { ClientesService } from '../../clientes/clientes.service';
import { LeadsService } from '../../leads/leads.service';
import { OportunidadesService } from '../../oportunidades/oportunidades.service';
import { ClienteResponse } from '../../clientes/clientes.model';
import { LeadResponse } from '../../leads/leads.model';
import { OportunidadResponse } from '../../oportunidades/oportunidades.model';
import { EtiquetaResponse } from '../../etiquetas/etiquetas.model';
import { EtiquetasService } from '../../etiquetas/etiquetas.service';
import { EtiquetaSelectorComponent } from '../../etiquetas/etiqueta-selector/etiqueta-selector';
import { DraftService } from '../../../core/services/draft.service';

export interface ActividadDialogData {
  id?: number | null;
}

export interface ActividadDialogResult {
  saved: boolean;
}

type TipoVinculo = 'NINGUNO' | 'CLIENTE' | 'LEAD' | 'OPORTUNIDAD';

@Component({
  selector: 'app-actividad-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatIconModule,
    EtiquetaSelectorComponent
  ],
  templateUrl: './actividad-form.html',
  styleUrl: './actividad-form.css'
})
export class ActividadForm implements OnInit {
  private readonly dialogRef = inject<MatDialogRef<ActividadForm, ActividadDialogResult> | null>(MatDialogRef, { optional: true });
  private readonly dialogData = inject<ActividadDialogData | null>(MAT_DIALOG_DATA, { optional: true });
  private readonly cdr = inject(ChangeDetectorRef);

  readonly esDialogo = this.dialogRef !== null;
  readonly maxEtiquetas = 8;

  form: FormGroup;
  isEdit = false;
  id!: number;
  loading = false;

  tipos = Object.values(TipoActividad);
  estados = Object.values(EstadoActividad);

  readonly vinculos: { value: TipoVinculo; label: string; icon: string }[] = [
    { value: 'NINGUNO', label: 'Sin vincular', icon: 'link_off' },
    { value: 'CLIENTE', label: 'Cliente', icon: 'business' },
    { value: 'LEAD', label: 'Lead', icon: 'person_search' },
    { value: 'OPORTUNIDAD', label: 'Oportunidad', icon: 'track_changes' },
  ];

  // Catálogos relacionados
  clientes: ClienteResponse[] = [];
  leads: LeadResponse[] = [];
  oportunidades: OportunidadResponse[] = [];
  cargandoRelaciones = false;

  // Etiquetas
  etiquetasCatalogo: EtiquetaResponse[] = [];
  etiquetasSeleccionadas: EtiquetaResponse[] = [];
  cargandoEtiquetas = false;
  errorEtiquetas = '';

  errorGuardado = '';

  constructor(
    private fb: FormBuilder,
    private actividadesService: ActividadesService,
    private clientesService: ClientesService,
    private leadsService: LeadsService,
    private oportunidadesService: OportunidadesService,
    private etiquetasService: EtiquetasService,
    private route: ActivatedRoute,
    private router: Router,
    private draft: DraftService,
    private destroyRef: DestroyRef
  ) {
    const fechaSugerida = new Date(Date.now() + 60 * 60 * 1000); // +1h

    this.form = this.fb.group({
      titulo: ['', [Validators.required]],
      descripcion: [''],
      tipo: [TipoActividad.TAREA, Validators.required],
      estado: [EstadoActividad.PENDIENTE, Validators.required],
      fechaHora: [this.formatDateTime(fechaSugerida), Validators.required],
      vinculacionTipo: ['NINGUNO'], // 'NINGUNO' | 'CLIENTE' | 'LEAD' | 'OPORTUNIDAD'
      idCliente: [null],
      idLead: [null],
      idOportunidad: [null]
    });
  }

  get draftKey(): string {
    return `actividad:${this.isEdit ? this.id : 'nuevo'}`;
  }

  ngOnInit() {
    this.cargarCatalogos();

    const idDialogo = this.dialogData?.id ?? null;
    const idParam = this.route.snapshot.paramMap.get('id');
    const id = idDialogo ?? (idParam ? +idParam : null);

    if (id) {
      this.isEdit = true;
      this.id = id;
      this.cargarActividad();
    } else {
      this.restaurarBorrador();
    }

    if (this.dialogRef) {
      this.dialogRef.backdropClick().subscribe(() => {
        if (this.form.dirty) {
          this.draft.save(this.draftKey, { form: this.form.getRawValue(), etiquetas: this.etiquetasSeleccionadas });
        }
      });
    }

    this.form.valueChanges
      .pipe(debounceTime(600), takeUntilDestroyed(this.destroyRef))
      .subscribe((v) => {
        if (this.form.dirty) {
          this.draft.save(this.draftKey, { form: v, etiquetas: this.etiquetasSeleccionadas });
        }
      });
  }

  private restaurarBorrador() {
    const borrador = this.draft.load(this.draftKey);
    if (borrador) {
      const restaurar = confirm('Se encontró un borrador sin guardar. ¿Deseas restaurarlo?');
      if (restaurar) {
        if (borrador.form) this.form.patchValue(borrador.form);
        if (borrador.etiquetas) this.etiquetasSeleccionadas = borrador.etiquetas;
        this.form.markAsDirty();
      } else {
        this.draft.clear(this.draftKey);
      }
    }
  }

  private formatDateTime(d: Date): string {
    const pad = (n: number) => n.toString().padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
  }

  cargarCatalogos() {
    this.cargandoRelaciones = true;
    this.cargandoEtiquetas = true;
    this.errorEtiquetas = '';

    forkJoin({
      clientes: this.clientesService.listar().pipe(catchError(() => of([]))),
      leads: this.leadsService.listar().pipe(catchError(() => of([]))),
      oportunidades: this.oportunidadesService.listar().pipe(catchError(() => of([]))),
      etiquetas: this.etiquetasService.listar(true).pipe(
        catchError(() => {
          this.errorEtiquetas = 'No se pudieron cargar las etiquetas.';
          return of([]);
        })
      )
    }).pipe(
      finalize(() => {
        this.cargandoRelaciones = false;
        this.cargandoEtiquetas = false;
        this.cdr.markForCheck();
      })
    ).subscribe({
      next: ({ clientes, leads, oportunidades, etiquetas }) => {
        this.clientes = clientes;
        this.leads = leads;
        this.oportunidades = oportunidades;
        this.etiquetasCatalogo = etiquetas;
        this.cdr.markForCheck();
      }
    });
  }

  getTipoLabel(tipo: TipoActividad | string): string {
    switch (tipo) {
      case TipoActividad.LLAMADA: return 'Llamada';
      case TipoActividad.REUNION: return 'Reunión';
      case TipoActividad.TAREA: return 'Tarea';
      case TipoActividad.SEGUIMIENTO: return 'Seguimiento';
      default: return tipo;
    }
  }

  getTipoIcon(tipo: TipoActividad | string): string {
    switch (tipo) {
      case TipoActividad.LLAMADA: return 'call';
      case TipoActividad.REUNION: return 'groups';
      case TipoActividad.TAREA: return 'checklist';
      case TipoActividad.SEGUIMIENTO: return 'sync';
      default: return 'event';
    }
  }

  getTipoHint(tipo: TipoActividad | string): string {
    switch (tipo) {
      case TipoActividad.LLAMADA: return 'Contacto telefónico';
      case TipoActividad.REUNION: return 'Encuentro con cliente';
      case TipoActividad.TAREA: return 'Pendiente operativo';
      case TipoActividad.SEGUIMIENTO: return 'Revisión periódica';
      default: return 'Actividad de agenda';
    }
  }

  seleccionarTipo(tipo: TipoActividad) {
    this.form.patchValue({ tipo });
  }

  seleccionarVinculo(tipo: TipoVinculo) {
    this.form.patchValue({ vinculacionTipo: tipo });
    this.onVinculacionChange(tipo);
  }

  getEstadoLabel(estado: EstadoActividad | string): string {
    switch (estado) {
      case EstadoActividad.PENDIENTE: return 'Pendiente';
      case EstadoActividad.EN_PROCESO: return 'En proceso';
      case EstadoActividad.COMPLETADA: return 'Completada';
      case EstadoActividad.CANCELADA: return 'Cancelada';
      default: return estado;
    }
  }

  mostrarError(campo: string): boolean {
    const control = this.form.get(campo);
    return !!control && control.invalid && (control.touched || control.dirty);
  }

  mensajeError(campo: string): string {
    const control = this.form.get(campo);
    if (!control) return '';
    if (control.hasError('required')) return 'Este campo es obligatorio.';
    if (control.hasError('maxlength')) {
      return `Máximo ${control.getError('maxlength').requiredLength} caracteres.`;
    }
    return 'Valor inválido.';
  }

  abrirSelector(input: HTMLInputElement) {
    if (typeof input.showPicker === 'function') {
      input.showPicker();
    } else {
      input.focus();
    }
  }

  cancelar() {
    if (this.loading) return;
    this.cerrar(false);
  }

  private cerrar(saved: boolean) {
    if (this.dialogRef) {
      this.dialogRef.close({ saved });
      return;
    }
    this.router.navigate(['/actividades']);
  }

  onVinculacionChange(tipo: TipoVinculo) {
    if (tipo !== 'CLIENTE') this.form.patchValue({ idCliente: null });
    if (tipo !== 'LEAD') this.form.patchValue({ idLead: null });
    if (tipo !== 'OPORTUNIDAD') this.form.patchValue({ idOportunidad: null });
  }

  onEtiquetasChange(etiquetas: EtiquetaResponse[]) {
    this.etiquetasSeleccionadas = etiquetas;
    this.cdr.markForCheck();
  }

  onEtiquetaCreada(etiqueta: EtiquetaResponse) {
    if (!this.etiquetasCatalogo.some((item) => item.id === etiqueta.id)) {
      this.etiquetasCatalogo = [...this.etiquetasCatalogo, etiqueta]
        .sort((a, b) => a.nombre.localeCompare(b.nombre, 'es'));
    }
    this.cdr.markForCheck();
  }

  cargarActividad() {
    this.actividadesService.obtener(this.id).subscribe({
      next: (act) => {
        const fechaHora = act.fecha
          ? act.fecha.slice(0, 16)
          : this.formatDateTime(new Date());

        let tipoVinc: TipoVinculo = 'NINGUNO';
        if (act.idCliente) tipoVinc = 'CLIENTE';
        else if (act.idLead) tipoVinc = 'LEAD';
        else if (act.idOportunidad) tipoVinc = 'OPORTUNIDAD';

        this.form.patchValue({
          titulo: act.titulo,
          descripcion: act.descripcion,
          tipo: act.tipo,
          estado: act.estado,
          fechaHora,
          vinculacionTipo: tipoVinc,
          idCliente: act.idCliente || null,
          idLead: act.idLead || null,
          idOportunidad: act.idOportunidad || null
        });

        this.etiquetasSeleccionadas = act.etiquetas || [];
        this.cdr.markForCheck();
        const borrador = this.draft.load(this.draftKey);
        if (borrador) {
          const restaurar = confirm('Se encontró un borrador sin guardar. ¿Deseas restaurarlo?');
          if (restaurar) {
            if (borrador.form) this.form.patchValue(borrador.form);
            if (borrador.etiquetas) this.etiquetasSeleccionadas = borrador.etiquetas;
            this.form.markAsDirty();
          } else {
            this.draft.clear(this.draftKey);
          }
        }
      },
      error: (err) => {
        this.errorGuardado = this.mensajeErrorHttp(err, 'No se pudo cargar la actividad seleccionada.');
        this.cdr.markForCheck();
      }
    });
  }

  guardar() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.errorGuardado = '';
    const val = this.form.value;

    // datetime-local entrega "YYYY-MM-DDTHH:mm"; Spring espera LocalDateTime.
    const fechaISO = val.fechaHora.length === 16 ? `${val.fechaHora}:00` : val.fechaHora;

    const request: ActividadRequest = {
      titulo: val.titulo,
      descripcion: val.descripcion,
      tipo: val.tipo,
      estado: val.estado,
      fecha: fechaISO,
      idCliente: val.vinculacionTipo === 'CLIENTE' && val.idCliente ? Number(val.idCliente) : undefined,
      idLead: val.vinculacionTipo === 'LEAD' && val.idLead ? Number(val.idLead) : undefined,
      idOportunidad: val.vinculacionTipo === 'OPORTUNIDAD' && val.idOportunidad ? Number(val.idOportunidad) : undefined,
      etiquetaIds: this.etiquetasSeleccionadas.map((etiqueta) => etiqueta.id)
    };

    const req$ = this.isEdit
      ? this.actividadesService.actualizar(this.id, request)
      : this.actividadesService.crear(request);

    req$.subscribe({
      next: () => {
        this.draft.clear(this.draftKey);
        this.cerrar(true);
      },
      error: (err) => {
        this.errorGuardado = this.mensajeErrorHttp(
          err,
          'Error al guardar la actividad. Verifique los campos obligatorios.'
        );
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  private mensajeErrorHttp(err: any, fallback: string): string {
    return err?.error?.message || err?.error?.error || fallback;
  }
}
