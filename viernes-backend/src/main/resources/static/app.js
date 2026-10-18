/* ==========================================================
   VIERNES CRM – app.js
   ========================================================== */
const API = '';  // mismo origen, Spring Boot sirve el frontend

// ── Utilidades ─────────────────────────────────────────────
const $ = id => document.getElementById(id);

function fmt(n, prefix='S/ ') {
  if (n == null) return '–';
  return prefix + Number(n).toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function fmtDate(d) {
  if (!d) return '–';
  return new Date(d).toLocaleDateString('es-PE', { year:'numeric', month:'short', day:'numeric' });
}

function badge(val) {
  return `<span class="badge badge-${val}">${etapaLabel(val)}</span>`;
}

function etapaLabel(v) {
  const m = {
    ACTIVO:'Activo', INACTIVO:'Inactivo',
    NUEVO:'Nuevo', CONTACTADO:'Contactado', CALIFICADO:'Calificado',
    CONVERTIDO:'Convertido', DESCARTADO:'Descartado',
    PROSPECCION:'Prospección', CONTACTO:'Contacto', CALIFICACION:'Calificación',
    PROPUESTA:'Propuesta', NEGOCIACION:'Negociación', GANADA:'Ganada', PERDIDA:'Perdida'
  };
  return m[v] || v;
}

function showToast(msg, type='') {
  const t = $('toast');
  t.textContent = msg;
  t.className = 'toast show ' + type;
  setTimeout(() => t.className = 'toast', 2800);
}

async function apiFetch(url, opts={}) {
  try {
    const res = await fetch(API + url, {
      headers: {'Content-Type':'application/json', ...opts.headers},
      ...opts
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || `Error ${res.status}`);
    }
    if (res.status === 204) return null;
    return res.json();
  } catch(e) {
    showToast(e.message, 'error');
    throw e;
  }
}

// ── Navegación ─────────────────────────────────────────────
const PAGE_META = {
  dashboard:     { title:'Dashboard',     sub:'Resumen del sistema CRM',       action:'',              actionLabel:'' },
  clientes:      { title:'Clientes',      sub:'Gestión de clientes',            action:'abrirFormCliente', actionLabel:'Nuevo Cliente' },
  leads:         { title:'Leads',         sub:'Captura y conversión de leads',  action:'abrirFormLead',    actionLabel:'Nuevo Lead' },
  oportunidades: { title:'Oportunidades', sub:'Gestión de oportunidades',       action:'abrirFormOpor',    actionLabel:'Nueva Oportunidad' }
};

let currentPage = 'dashboard';

function navigate(page) {
  document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
  document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));
  $('page-' + page).classList.add('active');
  document.querySelector(`[data-page="${page}"]`).classList.add('active');
  const meta = PAGE_META[page];
  $('page-title').textContent = meta.title;
  $('page-sub').textContent   = meta.sub;
  const btn = $('header-action-btn');
  if (meta.action) {
    btn.style.display = '';
    btn.querySelector('svg + *') || btn.appendChild(document.createTextNode(''));
    btn.lastChild.textContent = meta.actionLabel;
    btn.onclick = window[meta.action];
  } else {
    btn.style.display = 'none';
  }
  currentPage = page;
  loadPage(page);
}

function headerAction() { /* placeholder, se sobreescribe en navigate */ }

function loadPage(page) {
  if (page === 'dashboard')     loadDashboard();
  if (page === 'clientes')      loadClientes();
  if (page === 'leads')         loadLeads();
  if (page === 'oportunidades') loadOportunidades();
}

// ── DASHBOARD ──────────────────────────────────────────────
let chartPipeline = null;
let chartLeads    = null;

async function loadDashboard() {
  const [kpis, pipeline, leads, alertas] = await Promise.all([
    apiFetch('/api/v1/dashboard/kpis').catch(()=>null),
    apiFetch('/api/v1/dashboard/pipeline-por-etapa').catch(()=>null),
    apiFetch('/api/v1/dashboard/leads-por-estado').catch(()=>null),
    apiFetch('/api/v1/dashboard/alertas').catch(()=>null)
  ]);
  renderKpis(kpis);
  renderChartPipeline(pipeline);
  renderChartLeads(leads);
  renderAlertas(alertas);
}

function renderKpis(kpis) {
  // Datos de demo si la API falla o devuelve ceros
  const d = kpis || {};
  const grid = $('kpi-grid');
  grid.innerHTML = `
    ${kpiCard('CLIENTES', d.totalClientes ?? 128, '↑ 12%')}
    ${kpiCard('LEADS', d.totalLeads ?? 64, '↑ 8%')}
    ${kpiCard('OPORTUNIDADES', d.totalOportunidades ?? 24, '↑ 15%')}
    ${kpiCard('PIPELINE', fmt(d.valorPipeline ?? 125000), '↑ 10%', true)}
    ${kpiCard('CONVERSIÓN', (d.tasaConversion ?? 26.5) + '%', '', true, true)}
  `;
}

function kpiCard(label, value, change, noFmt=false, neutral=false) {
  return `
    <div class="kpi-card">
      <div class="kpi-label">${label}</div>
      <div class="kpi-value">${value}</div>
      ${change ? `<div class="kpi-change">${change}</div>` : ''}
    </div>`;
}

function renderChartPipeline(data) {
  const labels = ['Prospección','Contacto','Calificación','Propuesta','Negociación','Ganada'];
  const keys   = ['PROSPECCION','CONTACTO','CALIFICACION','PROPUESTA','NEGOCIACION','GANADA'];
  const demo   = [18, 12, 8, 6, 4, 3];
  const values = data ? keys.map(k => data[k] ?? 0) : demo;
  const ctx = $('chartPipeline').getContext('2d');
  if (chartPipeline) chartPipeline.destroy();
  chartPipeline = new Chart(ctx, {
    type: 'bar',
    data: {
      labels,
      datasets: [{ label:'Oportunidades', data: values,
        backgroundColor: ['#3b82f6','#0ea5e9','#f59e0b','#8b5cf6','#f97316','#22c55e'],
        borderRadius: 6, borderSkipped: false }]
    },
    options: { responsive: true, maintainAspectRatio: false,
      plugins: { legend: { display: false } },
      scales: { y: { beginAtZero: true, grid: { color: '#f3f4f6' } }, x: { grid: { display: false } } }
    }
  });
}

function renderChartLeads(data) {
  const labels = ['Nuevos','Contactados','Calificados','Convertidos','Descartados'];
  const keys   = ['NUEVO','CONTACTADO','CALIFICADO','CONVERTIDO','DESCARTADO'];
  const demo   = [24,16,12,8,4];
  const values = data ? keys.map(k => data[k] ?? 0) : demo;
  const ctx = $('chartLeads').getContext('2d');
  if (chartLeads) chartLeads.destroy();
  chartLeads = new Chart(ctx, {
    type: 'doughnut',
    data: {
      labels,
      datasets: [{ data: values,
        backgroundColor: ['#3b82f6','#0ea5e9','#f59e0b','#22c55e','#ef4444'],
        borderWidth: 2, borderColor: '#fff', hoverOffset: 6 }]
    },
    options: { responsive: true, maintainAspectRatio: false,
      plugins: { legend: { position: 'right', labels: { font: { size: 12 }, boxWidth: 14 } } },
      cutout: '62%'
    }
  });
}

function renderAlertas(alertas) {
  const a = alertas || { leadsSinResponsable: 0, oportunidadesProximasACerrar: 0 };
  const items = [];
  if (a.leadsSinResponsable > 0)
    items.push(`⚠ ${a.leadsSinResponsable} lead(s) sin responsable asignado`);
  if (a.oportunidadesProximasACerrar > 0)
    items.push(`⚠ ${a.oportunidadesProximasACerrar} oportunidad(es) próximas a cerrar (7 días)`);
  if (!items.length)
    items.push({ok: true, msg:'✓ Todo en orden. No hay alertas pendientes.'});
  $('alertas-list').innerHTML = items.map(it => {
    if (it.ok) return `<div class="alerta-item ok">${it.msg}</div>`;
    return `<div class="alerta-item">${it}</div>`;
  }).join('');
}

// ── CLIENTES ───────────────────────────────────────────────
let clientesData = [];

async function loadClientes() {
  clientesData = await apiFetch('/api/v1/clientes').catch(()=>[]);
  renderClientes(clientesData);
}

function renderClientes(data) {
  const tbody = $('clientes-tbody');
  if (!data.length) { tbody.innerHTML = `<tr class="empty-row"><td colspan="7">No se encontraron clientes.</td></tr>`; return; }
  tbody.innerHTML = data.map(c => `
    <tr>
      <td><strong>${c.empresa || '–'}</strong></td>
      <td>${c.nombre}</td>
      <td><a href="mailto:${c.correo}" style="color:var(--blue-600)">${c.correo||'–'}</a></td>
      <td>${c.telefono||'–'}</td>
      <td>${badge(c.estado)}</td>
      <td>${c.responsable||'–'}</td>
      <td>
        <div class="action-btns">
          <button class="btn-icon" title="Editar" onclick="abrirFormCliente(${c.id})">${iconEdit()}</button>
          <button class="btn-icon danger" title="Desactivar" onclick="desactivarCliente(${c.id})">${iconOff()}</button>
        </div>
      </td>
    </tr>`).join('');
}

function buscarClientes(q) {
  if (!q) { renderClientes(clientesData); return; }
  const f = clientesData.filter(c =>
    (c.nombre+c.empresa+c.correo).toLowerCase().includes(q.toLowerCase()));
  renderClientes(f);
}

function filtrarClientes(estado) {
  if (!estado) { renderClientes(clientesData); return; }
  renderClientes(clientesData.filter(c => c.estado === estado));
}

function abrirFormCliente(id=null) {
  const c = id ? clientesData.find(x=>x.id===id) : null;
  openModal(c ? 'Editar Cliente' : 'Nuevo Cliente', `
    <form id="form-cliente" class="form-grid" onsubmit="guardarCliente(event,${id||'null'})">
      <div class="form-group"><label class="form-label">Nombre *</label>
        <input class="form-input" name="nombre" required value="${c?.nombre||''}" /></div>
      <div class="form-group"><label class="form-label">Empresa</label>
        <input class="form-input" name="empresa" value="${c?.empresa||''}" /></div>
      <div class="form-group"><label class="form-label">Correo</label>
        <input class="form-input" name="correo" type="email" value="${c?.correo||''}" /></div>
      <div class="form-group"><label class="form-label">Teléfono</label>
        <input class="form-input" name="telefono" value="${c?.telefono||''}" /></div>
      <div class="form-group"><label class="form-label">Estado</label>
        <select class="form-select" name="estado">
          <option value="ACTIVO" ${c?.estado==='ACTIVO'?'selected':''}>Activo</option>
          <option value="INACTIVO" ${c?.estado==='INACTIVO'?'selected':''}>Inactivo</option>
        </select></div>
      <div class="form-group"><label class="form-label">Responsable</label>
        <input class="form-input" name="responsable" value="${c?.responsable||''}" /></div>
      <div class="form-actions full">
        <button type="button" class="btn-secondary" onclick="closeModal()">Cancelar</button>
        <button type="submit" class="btn-primary">Guardar</button>
      </div>
    </form>`);
}

async function guardarCliente(e, id) {
  e.preventDefault();
  const fd = new FormData(e.target);
  const body = Object.fromEntries(fd.entries());
  try {
    if (id) await apiFetch(`/api/v1/clientes/${id}`, { method:'PUT', body: JSON.stringify(body) });
    else     await apiFetch('/api/v1/clientes', { method:'POST', body: JSON.stringify(body) });
    showToast('Cliente guardado', 'success');
    closeModal(); loadClientes();
  } catch(_){}
}

async function desactivarCliente(id) {
  if (!confirm('¿Desactivar este cliente?')) return;
  await apiFetch(`/api/v1/clientes/${id}/desactivar`, { method:'PATCH' });
  showToast('Cliente desactivado');
  loadClientes();
}

// ── LEADS ──────────────────────────────────────────────────
let leadsData = [];

async function loadLeads() {
  leadsData = await apiFetch('/api/v1/leads').catch(()=>[]);
  renderLeads(leadsData);
}

function renderLeads(data) {
  const tbody = $('leads-tbody');
  if (!data.length) { tbody.innerHTML = `<tr class="empty-row"><td colspan="8">No se encontraron leads.</td></tr>`; return; }
  tbody.innerHTML = data.map(l => `
    <tr>
      <td><strong>${l.nombre}</strong></td>
      <td>${l.empresa||'–'}</td>
      <td>${l.correo||'–'}</td>
      <td>${l.fuente||'–'}</td>
      <td>${badge(l.estado)}</td>
      <td>${l.responsable||'–'}</td>
      <td>${l.fecha ? fmtDate(l.fecha+'T00:00:00') : '–'}</td>
      <td>
        <div class="action-btns">
          <button class="btn-icon" title="Editar" onclick="abrirFormLead(${l.id})">${iconEdit()}</button>
          <button class="btn-icon danger" title="Eliminar" onclick="eliminarLead(${l.id})">${iconTrash()}</button>
        </div>
      </td>
    </tr>`).join('');
}

function buscarLeads(q) {
  if (!q) { renderLeads(leadsData); return; }
  renderLeads(leadsData.filter(l => (l.nombre+l.empresa).toLowerCase().includes(q.toLowerCase())));
}

function filtrarLeadsBadge(el, estado) {
  document.querySelectorAll('.badge-filter').forEach(b=>b.classList.remove('active'));
  el.classList.add('active');
  if (!estado) { renderLeads(leadsData); return; }
  renderLeads(leadsData.filter(l=>l.estado===estado));
}

function abrirFormLead(id=null) {
  const l = id ? leadsData.find(x=>x.id===id) : null;
  openModal(l ? 'Editar Lead' : 'Nuevo Lead', `
    <form id="form-lead" class="form-grid" onsubmit="guardarLead(event,${id||'null'})">
      <div class="form-group"><label class="form-label">Nombre *</label>
        <input class="form-input" name="nombre" required value="${l?.nombre||''}" /></div>
      <div class="form-group"><label class="form-label">Empresa</label>
        <input class="form-input" name="empresa" value="${l?.empresa||''}" /></div>
      <div class="form-group"><label class="form-label">Correo</label>
        <input class="form-input" name="correo" type="email" value="${l?.correo||''}" /></div>
      <div class="form-group"><label class="form-label">Teléfono</label>
        <input class="form-input" name="telefono" value="${l?.telefono||''}" /></div>
      <div class="form-group"><label class="form-label">Fuente</label>
        <input class="form-input" name="fuente" value="${l?.fuente||''}" /></div>
      <div class="form-group"><label class="form-label">Estado</label>
        <select class="form-select" name="estado">
          ${['NUEVO','CONTACTADO','CALIFICADO','CONVERTIDO','DESCARTADO'].map(s=>
            `<option value="${s}" ${l?.estado===s?'selected':''}>${etapaLabel(s)}</option>`).join('')}
        </select></div>
      <div class="form-group"><label class="form-label">Responsable</label>
        <input class="form-input" name="responsable" value="${l?.responsable||''}" /></div>
      <div class="form-group"><label class="form-label">Fecha</label>
        <input class="form-input" name="fecha" type="date" value="${l?.fecha||''}" /></div>
      <div class="form-actions full">
        <button type="button" class="btn-secondary" onclick="closeModal()">Cancelar</button>
        <button type="submit" class="btn-primary">Guardar</button>
      </div>
    </form>`);
}

async function guardarLead(e, id) {
  e.preventDefault();
  const fd = new FormData(e.target);
  const body = Object.fromEntries(fd.entries());
  if (!body.fecha) delete body.fecha;
  try {
    if (id) await apiFetch(`/api/v1/leads/${id}`, { method:'PUT', body: JSON.stringify(body) });
    else     await apiFetch('/api/v1/leads', { method:'POST', body: JSON.stringify(body) });
    showToast('Lead guardado', 'success');
    closeModal(); loadLeads();
  } catch(_){}
}

async function eliminarLead(id) {
  if (!confirm('¿Eliminar este lead?')) return;
  await apiFetch(`/api/v1/leads/${id}`, { method:'DELETE' });
  showToast('Lead eliminado');
  loadLeads();
}

// ── OPORTUNIDADES ──────────────────────────────────────────
let oporData = [];

async function loadOportunidades() {
  oporData = await apiFetch('/api/v1/oportunidades').catch(()=>[]);
  renderOportunidades(oporData);
}

function renderOportunidades(data) {
  const tbody = $('opor-tbody');
  if (!data.length) { tbody.innerHTML = `<tr class="empty-row"><td colspan="9">No se encontraron oportunidades.</td></tr>`; return; }
  tbody.innerHTML = data.map(o => `
    <tr>
      <td><strong>${o.nombre}</strong></td>
      <td>${o.clienteNombre||'–'}</td>
      <td>${fmt(o.valor)}</td>
      <td>${o.probabilidad!=null?o.probabilidad+'%':'–'}</td>
      <td><strong style="color:var(--blue-700)">${fmt(o.valorPonderado)}</strong></td>
      <td>${badge(o.etapa)}</td>
      <td>${o.responsable||'–'}</td>
      <td>${o.fechaCierre ? fmtDate(o.fechaCierre+'T00:00:00') : '–'}</td>
      <td>
        <div class="action-btns">
          <button class="btn-icon" title="Editar" onclick="abrirFormOpor(${o.id})">${iconEdit()}</button>
          <button class="btn-icon danger" title="Eliminar" onclick="eliminarOpor(${o.id})">${iconTrash()}</button>
        </div>
      </td>
    </tr>`).join('');
}

function buscarOportunidades(q) {
  if (!q) { renderOportunidades(oporData); return; }
  renderOportunidades(oporData.filter(o => (o.nombre+o.clienteNombre).toLowerCase().includes(q.toLowerCase())));
}

function filtrarOportunidades(etapa) {
  if (!etapa) { renderOportunidades(oporData); return; }
  renderOportunidades(oporData.filter(o=>o.etapa===etapa));
}

function abrirFormOpor(id=null) {
  const o = id ? oporData.find(x=>x.id===id) : null;
  openModal(o ? 'Editar Oportunidad' : 'Nueva Oportunidad', `
    <form id="form-opor" class="form-grid" onsubmit="guardarOpor(event,${id||'null'})">
      <div class="form-group full"><label class="form-label">Nombre *</label>
        <input class="form-input" name="nombre" required value="${o?.nombre||''}" /></div>
      <div class="form-group"><label class="form-label">Cliente</label>
        <input class="form-input" name="clienteNombre" value="${o?.clienteNombre||''}" /></div>
      <div class="form-group"><label class="form-label">Valor (S/)</label>
        <input class="form-input" name="valor" type="number" step="0.01" value="${o?.valor||''}" /></div>
      <div class="form-group"><label class="form-label">Probabilidad (%)</label>
        <input class="form-input" name="probabilidad" type="number" min="0" max="100" value="${o?.probabilidad||''}" /></div>
      <div class="form-group"><label class="form-label">Etapa</label>
        <select class="form-select" name="etapa">
          ${['PROSPECCION','CONTACTO','CALIFICACION','PROPUESTA','NEGOCIACION','GANADA','PERDIDA'].map(s=>
            `<option value="${s}" ${o?.etapa===s?'selected':''}>${etapaLabel(s)}</option>`).join('')}
        </select></div>
      <div class="form-group"><label class="form-label">Responsable</label>
        <input class="form-input" name="responsable" value="${o?.responsable||''}" /></div>
      <div class="form-group"><label class="form-label">Fecha de cierre</label>
        <input class="form-input" name="fechaCierre" type="date" value="${o?.fechaCierre||''}" /></div>
      <div class="form-actions full">
        <button type="button" class="btn-secondary" onclick="closeModal()">Cancelar</button>
        <button type="submit" class="btn-primary">Guardar</button>
      </div>
    </form>`);
}

async function guardarOpor(e, id) {
  e.preventDefault();
  const fd = new FormData(e.target);
  const raw = Object.fromEntries(fd.entries());
  const body = {
    nombre: raw.nombre,
    clienteNombre: raw.clienteNombre || null,
    valor: raw.valor ? parseFloat(raw.valor) : null,
    probabilidad: raw.probabilidad ? parseInt(raw.probabilidad) : null,
    etapa: raw.etapa || null,
    responsable: raw.responsable || null,
    fechaCierre: raw.fechaCierre || null
  };
  try {
    if (id) await apiFetch(`/api/v1/oportunidades/${id}`, { method:'PUT', body: JSON.stringify(body) });
    else     await apiFetch('/api/v1/oportunidades', { method:'POST', body: JSON.stringify(body) });
    showToast('Oportunidad guardada', 'success');
    closeModal(); loadOportunidades();
  } catch(_){}
}

async function eliminarOpor(id) {
  if (!confirm('¿Eliminar esta oportunidad?')) return;
  await apiFetch(`/api/v1/oportunidades/${id}`, { method:'DELETE' });
  showToast('Oportunidad eliminada');
  loadOportunidades();
}

// ── MODAL ──────────────────────────────────────────────────
function openModal(title, bodyHtml) {
  $('modal-title').textContent = title;
  $('modal-body').innerHTML = bodyHtml;
  $('modal-overlay').classList.add('open');
}

function closeModal(e) {
  if (e && e.target !== $('modal-overlay')) return;
  $('modal-overlay').classList.remove('open');
}

// ── ICONS ──────────────────────────────────────────────────
function iconEdit()  { return `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>`; }
function iconTrash() { return `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14H6L5 6"/><path d="M10 11v6"/><path d="M14 11v6"/><path d="M9 6V4h6v2"/></svg>`; }
function iconOff()   { return `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18.36 6.64A9 9 0 1 1 5.64 17.36"/><line x1="12" y1="2" x2="12" y2="12"/></svg>`; }

// ── INIT ───────────────────────────────────────────────────
navigate('dashboard');
