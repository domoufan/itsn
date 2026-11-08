import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../environments/environment';
import {
  AiStatus, Article, Category, Comment, Equipment, HistoryEntry, Intervention, Kpis, Page, Site, SlaPolicy, Suggestion, Ticket, TicketSummary, UserAdmin,
} from './models';

/** Façade unique des appels REST. Les chemins correspondent aux contrôleurs Spring. */
@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiUrl;

  private params(obj: Record<string, unknown>): HttpParams {
    let p = new HttpParams();
    Object.entries(obj).forEach(([k, v]) => { if (v !== undefined && v !== null && v !== '') p = p.set(k, String(v)); });
    return p;
  }

  // Tickets
  tickets(filter: Record<string, unknown>) { return this.http.get<Page<TicketSummary>>(`${this.base}/tickets`, { params: this.params(filter) }); }
  ticket(id: number) { return this.http.get<Ticket>(`${this.base}/tickets/${id}`); }
  createTicket(body: unknown) { return this.http.post<Ticket>(`${this.base}/tickets`, body); }
  updateTicket(id: number, body: unknown) { return this.http.patch<Ticket>(`${this.base}/tickets/${id}`, body); }
  assignTicket(id: number, assigneeId: number, comment?: string) { return this.http.post<Ticket>(`${this.base}/tickets/${id}/assign`, { assigneeId, comment }); }
  changeStatus(id: number, body: { status: string; comment?: string; resolutionSummary?: string }) { return this.http.post<Ticket>(`${this.base}/tickets/${id}/status`, body); }
  comments(id: number) { return this.http.get<Comment[]>(`${this.base}/tickets/${id}/comments`); }
  addComment(id: number, content: string, internal: boolean) { return this.http.post<Comment>(`${this.base}/tickets/${id}/comments`, { content, internal }); }
  history(id: number) { return this.http.get<HistoryEntry[]>(`${this.base}/tickets/${id}/history`); }
  rate(id: number, score: number) { return this.http.post<Ticket>(`${this.base}/tickets/${id}/satisfaction`, { score }); }

  // Référentiels
  categories() { return this.http.get<Category[]>(`${this.base}/categories`); }
  sites() { return this.http.get<Site[]>(`${this.base}/sites`); }
  saveSite(s: Partial<Site>) { return s.id ? this.http.put<Site>(`${this.base}/sites/${s.id}`, s) : this.http.post<Site>(`${this.base}/sites`, s); }
  equipment(siteId?: number) { return this.http.get<Equipment[]>(`${this.base}/equipment`, { params: this.params({ siteId }) }); }
  saveEquipment(e: Partial<Equipment>) { return e.id ? this.http.put<Equipment>(`${this.base}/equipment/${e.id}`, e) : this.http.post<Equipment>(`${this.base}/equipment`, e); }
  slaPolicies() { return this.http.get<SlaPolicy[]>(`${this.base}/sla-policies`); }
  updateSla(p: SlaPolicy) { return this.http.put<SlaPolicy>(`${this.base}/sla-policies/${p.priority}`, p); }

  // Utilisateurs
  users(role?: string) { return this.http.get<UserAdmin[]>(`${this.base}/users`, { params: this.params({ role }) }); }
  technicians() { return this.http.get<UserAdmin[]>(`${this.base}/users/technicians`); }
  saveUser(u: Partial<UserAdmin> & { password?: string }) { return u.id ? this.http.put<UserAdmin>(`${this.base}/users/${u.id}`, u) : this.http.post<UserAdmin>(`${this.base}/users`, u); }
  deactivateUser(id: number) { return this.http.delete<void>(`${this.base}/users/${id}`); }
  changePassword(currentPassword: string, newPassword: string) { return this.http.post<void>(`${this.base}/auth/change-password`, { currentPassword, newPassword }); }

  // Interventions
  interventions(from?: string, to?: string, technicianId?: number) { return this.http.get<Intervention[]>(`${this.base}/interventions`, { params: this.params({ from, to, technicianId }) }); }
  interventionsByTicket(ticketId: number) { return this.http.get<Intervention[]>(`${this.base}/interventions/ticket/${ticketId}`); }
  planIntervention(body: unknown) { return this.http.post<Intervention>(`${this.base}/interventions`, body); }
  reportIntervention(id: number, body: unknown) { return this.http.patch<Intervention>(`${this.base}/interventions/${id}`, body); }

  // Base de connaissances
  articles(q?: string, page = 0) { return this.http.get<Page<Article>>(`${this.base}/kb/articles`, { params: this.params({ q, page, size: 20 }) }); }
  article(id: number) { return this.http.get<Article>(`${this.base}/kb/articles/${id}`); }
  saveArticle(a: Partial<Article>) { return a.id ? this.http.put<Article>(`${this.base}/kb/articles/${a.id}`, a) : this.http.post<Article>(`${this.base}/kb/articles`, a); }
  deleteArticle(id: number) { return this.http.delete<void>(`${this.base}/kb/articles/${id}`); }

  // Tableau de bord
  kpis(days = 30) { return this.http.get<Kpis>(`${this.base}/dashboard/kpis`, { params: this.params({ days }) }); }

  // Pipeline agentique
  aiStatus() { return this.http.get<AiStatus>(`${this.base}/ai/status`); }
  triage(ticketId: number, fallback = false) { return this.http.post<Suggestion>(`${this.base}/ai/tickets/${ticketId}/triage`, {}, { params: this.params({ fallback }) }); }
  pendingSuggestions() { return this.http.get<Suggestion[]>(`${this.base}/ai/suggestions/pending`); }
  suggestionsForTicket(ticketId: number) { return this.http.get<Suggestion[]>(`${this.base}/ai/suggestions/ticket/${ticketId}`); }
  reviewSuggestion(id: number, body: { decision: string; applyCategory?: boolean; applyPriority?: boolean; applyAssignee?: boolean; comment?: string }) {
    return this.http.post<Suggestion>(`${this.base}/ai/suggestions/${id}/review`, body);
  }
}
