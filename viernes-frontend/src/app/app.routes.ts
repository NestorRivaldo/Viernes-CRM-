import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { roleGuard } from './core/auth/role.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login').then(m => m.Login)
  },
  {
    path: '',
    loadComponent: () => import('./core/layout/layout').then((m) => m.Layout),
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      {
        path: 'dashboard',
        loadComponent: () => import('./features/dashboard/dashboard').then((m) => m.Dashboard),
      },
      {
        path: 'clientes',
        loadComponent: () => import('./features/clientes/cliente-list/cliente-list').then((m) => m.ClienteList),
      },
      {
        path: 'clientes/nuevo',
        loadComponent: () => import('./features/clientes/cliente-form/cliente-form').then((m) => m.ClienteForm),
      },
      {
        path: 'clientes/:id/editar',
        loadComponent: () => import('./features/clientes/cliente-form/cliente-form').then((m) => m.ClienteForm),
      },
      {
        path: 'relaciones',
        loadComponent: () => import('./features/relationship-map/relationship-map').then((m) => m.RelationshipMap),
      },
      {
        path: 'relaciones/clientes/:clienteId',
        loadComponent: () => import('./features/relationship-map/relationship-map').then((m) => m.RelationshipMap),
      },
      {
        path: 'relaciones/oportunidades/:oportunidadId',
        loadComponent: () => import('./features/relationship-map/relationship-map').then((m) => m.RelationshipMap),
      },
      {
        path: 'leads',
        loadComponent: () => import('./features/leads/lead-list/lead-list').then((m) => m.LeadList),
      },
      {
        path: 'leads/nuevo',
        loadComponent: () => import('./features/leads/lead-list/lead-list').then((m) => m.LeadList),
        data: { abrirNuevoLead: true },
      },
      {
        path: 'leads/:id/editar',
        loadComponent: () => import('./features/leads/lead-list/lead-list').then((m) => m.LeadList),
        data: { abrirEdicionLead: true },
      },
      {
        path: 'oportunidades',
        loadComponent: () => import('./features/oportunidades/oportunidad-list/oportunidad-list').then((m) => m.OportunidadList),
      },
      {
        path: 'oportunidades/nuevo',
        loadComponent: () => import('./features/oportunidades/oportunidad-form/oportunidad-form').then((m) => m.OportunidadForm),
      },
      {
        path: 'oportunidades/:id/editar',
        loadComponent: () => import('./features/oportunidades/oportunidad-form/oportunidad-form').then((m) => m.OportunidadForm),
      },
      {
        path: 'oportunidades/:id',
        loadComponent: () => import('./features/oportunidades/oportunidad-detail/oportunidad-detail').then((m) => m.OportunidadDetail),
      },
      {
        path: 'productos',
        loadComponent: () => import('./features/productos/producto-list/producto-list').then((m) => m.ProductoList),
      },
      {
        path: 'productos/nuevo',
        loadComponent: () => import('./features/productos/producto-list/producto-list').then((m) => m.ProductoList),
        canActivate: [roleGuard],
        data: { roles: ['ADMIN', 'JEFE'], abrirNuevoProducto: true },
      },
      {
        path: 'productos/:id/editar',
        loadComponent: () => import('./features/productos/producto-list/producto-list').then((m) => m.ProductoList),
        canActivate: [roleGuard],
        data: { roles: ['ADMIN', 'JEFE'], abrirEdicionProducto: true },
      },
      {
        path: 'actividades',
        loadComponent: () => import('./features/actividades/actividad-list/actividad-list').then((m) => m.ActividadList),
      },
      {
        path: 'actividades/nuevo',
        loadComponent: () => import('./features/actividades/actividad-list/actividad-list').then((m) => m.ActividadList),
        data: { abrirNuevaActividad: true },
      },
      {
        path: 'actividades/:id/editar',
        loadComponent: () => import('./features/actividades/actividad-list/actividad-list').then((m) => m.ActividadList),
        data: { abrirEdicion: true },
      },
      {
        path: 'propuestas',
        loadComponent: () => import('./features/propuestas/propuesta-list/propuesta-list').then((m) => m.PropuestaList),
      },
      {
        path: 'propuestas/nuevo',
        loadComponent: () => import('./features/propuestas/propuesta-list/propuesta-list').then((m) => m.PropuestaList),
        data: { abrirNuevaPropuesta: true },
      },
      {
        path: 'propuestas/:id/editar',
        loadComponent: () => import('./features/propuestas/propuesta-list/propuesta-list').then((m) => m.PropuestaList),
        data: { abrirEdicionPropuesta: true },
      },
      {
        path: 'usuarios',
        loadComponent: () => import('./features/usuarios/usuario-list/usuario-list').then((m) => m.UsuarioList),
        canActivate: [roleGuard],
        data: { roles: ['ADMIN'] },
      },
      {
        path: 'usuarios/nuevo',
        loadComponent: () => import('./features/usuarios/usuario-list/usuario-list').then((m) => m.UsuarioList),
        canActivate: [roleGuard],
        data: { roles: ['ADMIN'], abrirNuevoUsuario: true },
      },
      {
        path: 'usuarios/:id/editar',
        loadComponent: () => import('./features/usuarios/usuario-list/usuario-list').then((m) => m.UsuarioList),
        canActivate: [roleGuard],
        data: { roles: ['ADMIN'], abrirEdicionUsuario: true },
      },
      {
        path: 'equipos',
        loadComponent: () => import('./features/equipos/equipo-list/equipo-list').then((m) => m.EquipoList),
        canActivate: [roleGuard],
        data: { roles: ['ADMIN', 'JEFE'] },
      },
      {
        path: 'equipos/nuevo',
        loadComponent: () => import('./features/equipos/equipo-list/equipo-list').then((m) => m.EquipoList),
        canActivate: [roleGuard],
        data: { roles: ['ADMIN', 'JEFE'], abrirNuevoEquipo: true },
      },
      {
        path: 'equipos/:id/editar',
        loadComponent: () => import('./features/equipos/equipo-list/equipo-list').then((m) => m.EquipoList),
        canActivate: [roleGuard],
        data: { roles: ['ADMIN', 'JEFE'], abrirEdicionEquipo: true },
      },
      { path: 'contactos', redirectTo: 'clientes', pathMatch: 'full' },
      { path: 'pipeline', redirectTo: 'oportunidades', pathMatch: 'full' },
      { path: 'indicadores', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'campanas', redirectTo: 'dashboard', pathMatch: 'full' },
      {
        path: 'configuracion',
        loadComponent: () => import('./features/configuracion/configuracion').then(m => m.Configuracion),
        canActivate: [roleGuard],
        data: { roles: ['ADMIN'] },
      }
    ],
  },
  { path: '**', redirectTo: '' }
];
