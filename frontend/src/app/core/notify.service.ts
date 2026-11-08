import { Injectable, inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { HttpErrorResponse } from '@angular/common/http';

/** Retours utilisateur homogènes (succès / erreur API). */
@Injectable({ providedIn: 'root' })
export class NotifyService {
  private readonly snack = inject(MatSnackBar);

  success(message: string) { this.snack.open(message, 'OK', { duration: 3000 }); }

  error(err: unknown, fallback = 'Une erreur est survenue') {
    let msg = fallback;
    if (err instanceof HttpErrorResponse) {
      const body = err.error;
      if (body?.fieldErrors) msg = Object.entries(body.fieldErrors).map(([k, v]) => `${k} : ${v}`).join(' ; ');
      else if (body?.message) msg = body.message;
      else if (err.status === 0) msg = 'Serveur injoignable';
    }
    this.snack.open(msg, 'Fermer', { duration: 6000, panelClass: 'snack-error' });
  }
}
