import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { LeadsService } from '../leads.service';
import { LeadResponse, EstadoLead } from '../leads.model';
import { LeadDialogResult, LeadForm } from '../lead-form/lead-form';
import { sortRows } from '../../../shared/table-sort';

@Component({
  selector: 'app-lead-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatTableModule,
    MatSortModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatDialogModule
  ],
  templateUrl: './lead-list.html',
  styleUrl: './lead-list.css'
})
export class LeadList implements OnInit {
  private leadsService = inject(LeadsService);
  private dialog = inject(MatDialog);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  leads = signal<LeadResponse[]>([]);
  filtroTexto = signal('');
  filtroEstado = signal<string>('TODOS');
  loading = signal(false);
  error = signal('');

  displayedColumns: string[] = ['id', 'nombre', 'empresa', 'estado', 'fuente', 'acciones'];

  sortState = signal<Sort>({ active: 'id', direction: 'asc' });

  leadsFiltrados = computed(() => {
    const q = this.filtroTexto().toLowerCase().trim();
    const est = this.filtroEstado();
    return this.leads().filter(lead => {
      const matchText = !q ||
        lead.nombre?.toLowerCase().includes(q) ||
        lead.empresa?.toLowerCase().includes(q) ||
        lead.correo?.toLowerCase().includes(q);
      const matchEstado = est === 'TODOS' || lead.estado === est;
      return matchText && matchEstado;
    });
  });

  leadsOrdenados = computed(() => sortRows(this.leadsFiltrados(), this.sortState()));


  onSortChange(sort: Sort) {

    this.sortState.set(sort);

  }


  hasLeads = computed(() => this.leadsFiltrados().length > 0 && !this.loading());
  isEmpty = computed(() => !this.loading() && this.leadsFiltrados().length === 0 && !this.error());

  ngOnInit() {
    this.cargarLeads();

    if (this.route.snapshot.data['abrirNuevoLead']) {
      this.abrirDialogo();
      return;
    }

    const idParam = this.route.snapshot.paramMap.get('id');
    if (this.route.snapshot.data['abrirEdicionLead'] && idParam) {
      const id = Number(idParam);
      if (Number.isFinite(id) && id > 0) {
        this.abrirDialogo(id);
      }
    }
  }

  cargarLeads() {
    this.loading.set(true);
    this.error.set('');
    this.leadsService.listar().subscribe({
      next: (data) => {
        this.leads.set(data || []);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Error al cargar leads. Verifique que el backend esté disponible.');
        this.loading.set(false);
      }
    });
  }

  getEstadoClass(estado: EstadoLead | string): string {
    switch (estado) {
      case 'NUEVO': return 'blue';
      case 'CONTACTADO': return 'purple';
      case 'CALIFICADO': return 'success';
      case 'DESCARTADO': return 'danger';
      case 'CONVERTIDO': return 'emerald';
      default: return 'neutral';
    }
  }

  eliminar(id: number) {
    if (confirm('¿Está seguro de eliminar este lead?')) {
      this.leadsService.eliminar(id).subscribe({
        next: () => this.cargarLeads(),
        error: (err) => alert(err?.friendlyMessage || 'Error al eliminar el lead')
      });
    }
  }

  nuevoLead() {
    this.abrirDialogo();
  }

  editar(id: number) {
    this.abrirDialogo(id);
  }

  private abrirDialogo(id?: number) {
    const enRutaDirecta = !!(
      this.route.snapshot.data['abrirNuevoLead'] || this.route.snapshot.data['abrirEdicionLead']
    );

    this.dialog
      .open(LeadForm, {
        data: { id: id ?? null },
        panelClass: 'entity-dialog-panel',
        backdropClass: 'entity-dialog-backdrop',
        width: '820px',
        maxWidth: '95vw',
        disableClose: false,
        autoFocus: false,
      })
      .afterClosed()
      .subscribe((result: LeadDialogResult | undefined) => {
        if (result?.saved) {
          this.cargarLeads();
        }
        if (enRutaDirecta) {
          this.router.navigate(['/leads'], { replaceUrl: true });
        }
      });
  }
}
