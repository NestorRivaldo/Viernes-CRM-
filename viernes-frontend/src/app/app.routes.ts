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
        path: 'leads',
        loadComponent: () => import('./features/leads/lead-list/lead-list').then((m) => m.LeadList),
      },
      {
        path: 'leads/nuevo',
        loadComponent: () => import('./features/leads/lead-form/lead-form').then((m) => m.LeadForm),
      },
      {
        path: 'leads/:id/editar',
        loadComponent: () => import('./features/leads/lead-form/lead-form').then((m) => m.LeadForm),
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
        path: 'actividades',
        loadComponent: () => import('./features/actividades/actividad-list/actividad-list').then((m) => m.ActividadList),
      },
      {
        path: 'actividades/nuevo',
        loadComponent: () => import('./features/actividades/actividad-form/actividad-form').then((m) => m.ActividadForm),
      },
      {
        path: 'actividades/:id/editar',
        loadComponent: () => import('./features/actividades/actividad-form/actividad-form').then((m) => m.ActividadForm),
      },
      {
        path: 'propuestas',
        loadComponent: () => import('./features/propuestas/propuesta-list/propuesta-list').then((m) => m.PropuestaList),
      },
      {
        path: 'propuestas/nuevo',
        loadComponent: () => import('./features/propuestas/propuesta-form/propuesta-form').then((m) => m.PropuestaForm),
      },
      {
        path: 'propuestas/:id/editar',
        loadComponent: () => import('./features/propuestas/propuesta-form/propuesta-form').then((m) => m.PropuestaForm),
      },
      {
        path: 'usuarios',
        loadComponent: () => import('./features/usuarios/usuario-list/usuario-list').then((m) => m.UsuarioList),
        canActivate: [roleGuard],
        data: { roles: ['ADMIN'] },
      },
      {
        path: 'usuarios/nuevo',
        loadComponent: () => import('./features/usuarios/usuario-form/usuario-form').then((m) => m.UsuarioForm),
        canActivate: [roleGuard],
        data: { roles: ['ADMIN'] },
      },
      {
        path: 'usuarios/:id/editar',
        loadComponent: () => import('./features/usuarios/usuario-form/usuario-form').then((m) => m.UsuarioForm),
        canActivate: [roleGuard],
        data: { roles: ['ADMIN'] },
      }
    ],
  },
  { path: '**', redirectTo: '' }
];
