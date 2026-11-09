import { Injectable } from '@angular/core';

/**
 * Persistencia de borradores de formularios en `localStorage`.
 *
 * Se usa para que, si el usuario cierra un diálogo por accidente (clic fuera del
 * cuadro, ESC, etc.), no pierda la información que ya estaba escribiendo.
 * El borrador se guarda con un debounce desde el componente y se borra cuando
 * el registro se guarda con éxito.
 */
@Injectable({ providedIn: 'root' })
export class DraftService {
  private prefix = 'viernes:draft:';

  has(key: string): boolean {
    try {
      return localStorage.getItem(this.prefix + key) !== null;
    } catch {
      return false;
    }
  }

  load<T = any>(key: string): T | null {
    try {
      const raw = localStorage.getItem(this.prefix + key);
      if (!raw) return null;
      return JSON.parse(raw) as T;
    } catch {
      return null;
    }
  }

  save(key: string, value: unknown): void {
    try {
      localStorage.setItem(this.prefix + key, JSON.stringify(value));
    } catch {
      // Ignorar (cuota excedida, modo privado, etc.)
    }
  }

  clear(key: string): void {
    try {
      localStorage.removeItem(this.prefix + key);
    } catch {
      // Ignorar
    }
  }
}
