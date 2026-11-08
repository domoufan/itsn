// Modèles d'échange alignés sur les DTO de l'API (backend/src/main/java/.../*Dtos.java)

export type Role = 'ADMIN' | 'MANAGER' | 'TECHNICIAN' | 'REQUESTER';
export type Priority = 'P1' | 'P2' | 'P3' | 'P4';
export type TicketType = 'INCIDENT' | 'REQUEST';
export type TicketStatus = 'NEW' | 'ASSIGNED' | 'IN_PROGRESS' | 'ON_HOLD' | 'RESOLVED' | 'CLOSED' | 'CANCELLED';
export type SuggestionStatus = 'PENDING' | 'ACCEPTED' | 'PARTIAL' | 'REJECTED' | 'FAILED';
export type InterventionMode = 'ONSITE' | 'REMOTE';
export type InterventionStatus = 'PLANNED' | 'IN_PROGRESS' | 'DONE' | 'CANCELLED';

export interface UserProfile { id: number; fullName: string; email: string; role: Role; siteId?: number; siteName?: string; }
export interface LoginResponse { accessToken: string; tokenType: string; expiresInSeconds: number; user: UserProfile; }

export interface Ref { id: number; fullName?: string; email?: string; }
export interface CategoryRef { id: number; code: string; name: string; }
export interface SiteRef { id: number; name: string; clientName: string; }
export interface EquipmentRef { id: number; assetTag: string; type: string; brand?: string; model?: string; }

export interface TicketSummary {
  id: number; reference: string; type: TicketType; title: string; category?: CategoryRef; priority: Priority; status: TicketStatus;
  requester: Ref; assignee?: Ref; site?: SiteRef; createdAt: string; resolutionDueAt: string; slaResponseBreached: boolean; slaResolutionBreached: boolean;
}

export interface Ticket extends TicketSummary {
  description: string; impact: number; urgency: number; equipment?: EquipmentRef; updatedAt: string; responseDueAt: string;
  firstResponseAt?: string; resolvedAt?: string; closedAt?: string; resolutionSummary?: string; satisfactionScore?: number; allowedTransitions: TicketStatus[];
}

export interface Comment { id: number; author: Ref; content: string; internal: boolean; createdAt: string; }
export interface HistoryEntry { id: number; changedBy?: Ref; field: string; oldValue?: string; newValue?: string; changedAt: string; }

export interface Page<T> { content: T[]; totalElements: number; totalPages: number; number: number; size: number; }

export interface Category { id: number; code: string; name: string; description?: string; active: boolean; }
export interface Site { id: number; name: string; clientName: string; address?: string; city?: string; contactPhone?: string; active: boolean; }
export interface Equipment { id: number; assetTag: string; type: string; brand?: string; model?: string; serialNumber?: string; siteId?: number; siteName?: string; status: string; purchaseDate?: string; warrantyEnd?: string; notes?: string; }
export interface UserAdmin { id: number; fullName: string; email: string; role: Role; phone?: string; siteId?: number; siteName?: string; skills?: string; active: boolean; createdAt: string; lastLoginAt?: string; openTickets?: number; }
export interface SlaPolicy { id: number; priority: Priority; label: string; responseMinutes: number; resolutionMinutes: number; }

export interface Intervention {
  id: number; ticketId: number; ticketReference: string; ticketTitle: string; ticketPriority: Priority; technicianId: number; technicianName: string;
  mode: InterventionMode; status: InterventionStatus; scheduledStart: string; scheduledEnd: string; actualStart?: string; actualEnd?: string; report?: string; partsUsed?: string; siteName?: string;
}

export interface Article { id: number; title: string; content: string; categoryId?: number; categoryName?: string; tags?: string; authorName?: string; published: boolean; views: number; createdAt: string; updatedAt: string; }

export interface Kpis {
  openTickets: number; createdLast30Days: number; resolvedLast30Days: number; overdueOpen: number; slaResolutionComplianceRate: number; slaResponseComplianceRate: number;
  meanTimeToResolveHours?: number; averageSatisfaction?: number; openByPriority: Record<string, number>; byCategory: Record<string, number>;
  workload: { technicianId: number; technicianName: string; activeTickets: number }[]; pendingAiSuggestions: number; computedAt: string;
}

export interface Suggestion {
  id: number; ticketId: number; ticketReference: string; ticketTitle: string; status: SuggestionStatus;
  suggestedCategoryId?: number; suggestedCategoryCode?: string; suggestedCategoryName?: string; suggestedImpact?: number; suggestedUrgency?: number; suggestedPriority?: Priority;
  suggestedAssigneeId?: number; suggestedAssigneeName?: string; summary?: string; rationale?: string; relatedArticleIds?: string;
  model: string; toolCalls: number; inputTokens: number; outputTokens: number; latencyMs: number; estimatedCostUsd: number; errorMessage?: string; createdAt: string;
  reviewedByName?: string; reviewedAt?: string; reviewComment?: string; currentCategoryCode?: string; currentPriority: Priority; currentAssigneeName?: string;
}

export interface AiStatus { enabled: boolean; provider: string; model: string; dailyBudgetUsd: number; spentTodayUsd: number; pendingSuggestions: number; fallbackActive: boolean; }

export const STATUS_LABELS: Record<TicketStatus, string> = {
  NEW: 'Nouveau', ASSIGNED: 'Affecté', IN_PROGRESS: 'En cours', ON_HOLD: 'En attente', RESOLVED: 'Résolu', CLOSED: 'Clôturé', CANCELLED: 'Annulé',
};
export const PRIORITY_LABELS: Record<Priority, string> = { P1: 'P1 – Critique', P2: 'P2 – Élevée', P3: 'P3 – Moyenne', P4: 'P4 – Faible' };
export const ROLE_LABELS: Record<Role, string> = { ADMIN: 'Administrateur', MANAGER: 'Responsable support', TECHNICIAN: 'Technicien', REQUESTER: 'Demandeur' };
export const SCALE_LABELS: Record<number, string> = { 1: '1 – Élevé(e)', 2: '2 – Moyen(ne)', 3: '3 – Faible' };
