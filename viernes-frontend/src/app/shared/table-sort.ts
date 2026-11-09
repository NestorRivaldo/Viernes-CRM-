import { Sort } from '@angular/material/sort';

/**
 * Ordena una copia de las filas según el estado de MatSort.
 *
 * Compara números como números, booleanos como booleanos y el resto como
 * texto con orden alfabético/natural en español. Las fechas en formato ISO
 * (YYYY-MM-DD...) se ordenan correctamente como texto.
 */
export function sortRows<T>(rows: readonly T[], sort: Sort): T[] {
  const { active, direction } = sort;
  if (!active || !direction) {
    return [...rows];
  }

  const factor = direction === 'asc' ? 1 : -1;
  return [...rows].sort(
    (a, b) => factor * compareValues(valueOf(a, active), valueOf(b, active)),
  );
}

function valueOf(row: unknown, key: string): unknown {
  return (row as Record<string, unknown>)[key];
}

function compareValues(a: unknown, b: unknown): number {
  if (a === b) return 0;
  if (a === null || a === undefined || a === '') return 1;
  if (b === null || b === undefined || b === '') return -1;
  if (typeof a === 'number' && typeof b === 'number') return a - b;
  if (typeof a === 'boolean' && typeof b === 'boolean') return a === b ? 0 : a ? 1 : -1;

  return String(a).localeCompare(String(b), 'es', {
    numeric: true,
    sensitivity: 'base',
  });
}
