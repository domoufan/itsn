package sn.senetech.intervit.ticket;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.senetech.intervit.auth.CurrentUserService;
import sn.senetech.intervit.common.AuditService;
import sn.senetech.intervit.common.BusinessException;
import sn.senetech.intervit.common.NotFoundException;
import sn.senetech.intervit.domain.*;
import sn.senetech.intervit.notification.NotificationService;
import sn.senetech.intervit.repository.*;
import sn.senetech.intervit.ticket.TicketDtos.*;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Règles métier du cycle de vie des tickets : création avec priorisation et SLA, affectation,
 * transitions d'état contrôlées, commentaires, historique et contrôle d'accès par rôle.
 */
@Service
@Transactional
public class TicketService {

    private final TicketRepository tickets;
    private final TicketCommentRepository comments;
    private final TicketHistoryRepository history;
    private final UserRepository users;
    private final CategoryRepository categories;
    private final SiteRepository sites;
    private final EquipmentRepository equipment;
    private final SlaService slaService;
    private final TicketReferenceService referenceService;
    private final CurrentUserService currentUser;
    private final NotificationService notifications;
    private final AuditService audit;

    public TicketService(TicketRepository tickets, TicketCommentRepository comments, TicketHistoryRepository history,
                         UserRepository users, CategoryRepository categories, SiteRepository sites, EquipmentRepository equipment,
                         SlaService slaService, TicketReferenceService referenceService, CurrentUserService currentUser,
                         NotificationService notifications, AuditService audit) {
        this.tickets = tickets;
        this.comments = comments;
        this.history = history;
        this.users = users;
        this.categories = categories;
        this.sites = sites;
        this.equipment = equipment;
        this.slaService = slaService;
        this.referenceService = referenceService;
        this.currentUser = currentUser;
        this.notifications = notifications;
        this.audit = audit;
    }

    // ------------------------------------------------------------------ lecture

    @Transactional(readOnly = true)
    public Page<TicketSummary> search(TicketFilter filter, Pageable pageable) {
        TicketFilter effective = filter;
        User me = currentUser.currentUser();
        if (me.getRole() == Role.REQUESTER) {
            // Un demandeur ne voit que ses propres tickets, quel que soit le filtre demandé
            effective = new TicketFilter(filter.status(), filter.priority(), filter.type(), null, me.getId(),
                filter.siteId(), filter.categoryId(), filter.openOnly(), filter.slaBreached(), filter.search());
        }
        return tickets.findAll(TicketSpecifications.from(effective), pageable).map(TicketSummary::from);
    }

    @Transactional(readOnly = true)
    public TicketResponse get(Long id) {
        Ticket t = load(id);
        assertCanView(t, currentUser.currentUser());
        return TicketResponse.from(t);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> comments(Long ticketId) {
        Ticket t = load(ticketId);
        User me = currentUser.currentUser();
        assertCanView(t, me);
        List<TicketComment> list = me.getRole() == Role.REQUESTER
            ? comments.findByTicketIdAndInternalFalseOrderByCreatedAtAsc(ticketId)
            : comments.findByTicketIdOrderByCreatedAtAsc(ticketId);
        return list.stream().map(CommentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<HistoryResponse> history(Long ticketId) {
        Ticket t = load(ticketId);
        assertCanView(t, currentUser.currentUser());
        return history.findByTicketIdOrderByChangedAtAsc(ticketId).stream().map(HistoryResponse::from).toList();
    }

    // ------------------------------------------------------------------ création / modification

    public TicketResponse create(CreateRequest req) {
        User me = currentUser.currentUser();
        User requester = me;
        if (req.requesterId() != null && !req.requesterId().equals(me.getId())) {
            if (!currentUser.isStaff()) throw new AccessDeniedException("Création pour un tiers réservée au personnel");
            requester = users.findById(req.requesterId()).orElseThrow(() -> new NotFoundException("Utilisateur", req.requesterId()));
        }

        Ticket t = new Ticket();
        t.setReference(referenceService.next(req.type()));
        t.setType(req.type());
        t.setTitle(req.title().trim());
        t.setDescription(req.description().trim());
        t.setCategory(req.categoryId() == null ? defaultCategory() : loadCategory(req.categoryId()));
        t.setImpact(req.impact().shortValue());
        t.setUrgency(req.urgency().shortValue());
        t.setPriority(PriorityMatrix.compute(req.impact(), req.urgency()));
        t.setRequester(requester);
        t.setSite(req.siteId() != null ? loadSite(req.siteId()) : requester.getSite());
        t.setEquipment(req.equipmentId() == null ? null : loadEquipment(req.equipmentId()));
        t.setStatus(TicketStatus.NEW);
        t.setCreatedAt(Instant.now());
        slaService.applyDeadlines(t);

        t = tickets.save(t);
        history.save(TicketHistory.of(t, me, "status", null, TicketStatus.NEW));
        notifications.ticketCreated(t);
        return TicketResponse.from(t);
    }

    public TicketResponse update(Long id, UpdateRequest req) {
        Ticket t = load(id);
        User me = currentUser.currentUser();
        if (t.getStatus().isTerminal()) throw new BusinessException("Un ticket clôturé ou annulé ne peut plus être modifié");
        boolean staff = currentUser.isStaff();
        if (!staff && !isRequester(t, me)) throw new AccessDeniedException("Modification non autorisée");
        if (!staff && t.getStatus() != TicketStatus.NEW) throw new BusinessException("Le demandeur ne peut modifier un ticket déjà pris en charge");

        if (req.title() != null && !req.title().equals(t.getTitle())) {
            history.save(TicketHistory.of(t, me, "title", t.getTitle(), req.title()));
            t.setTitle(req.title().trim());
        }
        if (req.description() != null) t.setDescription(req.description().trim());
        if (req.categoryId() != null) {
            Category c = loadCategory(req.categoryId());
            if (!Objects.equals(c.getId(), t.getCategory() == null ? null : t.getCategory().getId())) {
                history.save(TicketHistory.of(t, me, "category", t.getCategory() == null ? null : t.getCategory().getCode(), c.getCode()));
                t.setCategory(c);
            }
        }
        if (req.siteId() != null) t.setSite(loadSite(req.siteId()));
        if (req.equipmentId() != null) t.setEquipment(loadEquipment(req.equipmentId()));

        boolean reprioritize = false;
        if (req.impact() != null && req.impact() != t.getImpact()) {
            if (!staff) throw new AccessDeniedException("Seul le personnel peut modifier l'impact");
            history.save(TicketHistory.of(t, me, "impact", t.getImpact(), req.impact()));
            t.setImpact(req.impact().shortValue());
            reprioritize = true;
        }
        if (req.urgency() != null && req.urgency() != t.getUrgency()) {
            if (!staff) throw new AccessDeniedException("Seul le personnel peut modifier l'urgence");
            history.save(TicketHistory.of(t, me, "urgency", t.getUrgency(), req.urgency()));
            t.setUrgency(req.urgency().shortValue());
            reprioritize = true;
        }
        if (reprioritize) {
            Priority newPriority = PriorityMatrix.compute(t.getImpact(), t.getUrgency());
            if (newPriority != t.getPriority()) {
                history.save(TicketHistory.of(t, me, "priority", t.getPriority(), newPriority));
                t.setPriority(newPriority);
                slaService.applyDeadlines(t);
            }
        }
        return TicketResponse.from(tickets.save(t));
    }

    /** Affectation (ou réaffectation) à un technicien : réservée aux responsables et administrateurs. */
    public TicketResponse assign(Long id, AssignRequest req) {
        Ticket t = load(id);
        User me = currentUser.currentUser();
        if (!(currentUser.hasRole(Role.MANAGER) || currentUser.hasRole(Role.ADMIN))) {
            throw new AccessDeniedException("Affectation réservée au responsable support");
        }
        User tech = users.findById(req.assigneeId()).orElseThrow(() -> new NotFoundException("Utilisateur", req.assigneeId()));
        if (tech.getRole() != Role.TECHNICIAN || !tech.isActive()) throw new BusinessException("L'utilisateur choisi n'est pas un technicien actif");
        if (t.getStatus().isTerminal() || t.getStatus() == TicketStatus.RESOLVED) throw new BusinessException("Ticket non affectable dans l'état " + t.getStatus());

        history.save(TicketHistory.of(t, me, "assignee", t.getAssignee() == null ? null : t.getAssignee().getFullName(), tech.getFullName()));
        t.setAssignee(tech);
        if (t.getStatus() == TicketStatus.NEW) {
            transition(t, TicketStatus.ASSIGNED, me);
        }
        if (req.comment() != null && !req.comment().isBlank()) addComment(t, me, req.comment(), true);
        t = tickets.save(t);
        notifications.ticketAssigned(t);
        audit.record(me.getId(), "TICKET_ASSIGNED", "TICKET", t.getId(), "assignee=" + tech.getId());
        return TicketResponse.from(t);
    }

    public TicketResponse changeStatus(Long id, StatusChangeRequest req) {
        Ticket t = load(id);
        User me = currentUser.currentUser();
        TicketStatus target = req.status();
        assertCanChangeStatus(t, me, target);

        if (target == TicketStatus.RESOLVED) {
            if (req.resolutionSummary() == null || req.resolutionSummary().isBlank()) {
                throw new BusinessException("Un résumé de résolution est obligatoire pour résoudre un ticket");
            }
            t.setResolutionSummary(req.resolutionSummary().trim());
        }
        transition(t, target, me);
        if (req.comment() != null && !req.comment().isBlank()) {
            addComment(t, me, req.comment(), me.getRole() != Role.REQUESTER);
        }
        t = tickets.save(t);
        if (target == TicketStatus.RESOLVED) notifications.ticketResolved(t);
        return TicketResponse.from(t);
    }

    public CommentResponse comment(Long id, CommentRequest req) {
        Ticket t = load(id);
        User me = currentUser.currentUser();
        assertCanView(t, me);
        if (t.getStatus().isTerminal()) throw new BusinessException("Impossible de commenter un ticket clôturé ou annulé");
        boolean internal = req.internal() && currentUser.isStaff(); // un demandeur ne peut pas créer de note interne
        TicketComment c = addComment(t, me, req.content(), internal);
        tickets.save(t);
        return CommentResponse.from(c);
    }

    public TicketResponse rate(Long id, SatisfactionRequest req) {
        Ticket t = load(id);
        User me = currentUser.currentUser();
        if (!isRequester(t, me)) throw new AccessDeniedException("Seul le demandeur peut évaluer la résolution");
        if (t.getStatus() != TicketStatus.RESOLVED && t.getStatus() != TicketStatus.CLOSED) {
            throw new BusinessException("L'évaluation n'est possible qu'après résolution");
        }
        t.setSatisfactionScore(req.score().shortValue());
        return TicketResponse.from(tickets.save(t));
    }

    // ------------------------------------------------------------------ règles internes

    private void transition(Ticket t, TicketStatus target, User by) {
        TicketStatus from = t.getStatus();
        if (from != target && !from.canTransitionTo(target)) {
            throw new BusinessException("Transition interdite : " + from + " → " + target);
        }
        if (target == TicketStatus.IN_PROGRESS && t.getAssignee() == null) {
            throw new BusinessException("Un ticket doit être affecté avant d'être pris en charge");
        }
        Instant now = Instant.now();
        if (target == TicketStatus.IN_PROGRESS && t.getFirstResponseAt() == null) {
            t.setFirstResponseAt(now);
            t.setSlaResponseBreached(now.isAfter(t.getResponseDueAt()));
        }
        if (target == TicketStatus.RESOLVED) {
            t.setResolvedAt(now);
            t.setSlaResolutionBreached(now.isAfter(t.getResolutionDueAt()));
        }
        if (target == TicketStatus.IN_PROGRESS && from == TicketStatus.RESOLVED) {
            // réouverture : on annule la résolution précédente
            t.setResolvedAt(null);
            t.setResolutionSummary(null);
        }
        if (target == TicketStatus.CLOSED) t.setClosedAt(now);
        if (from != target) {
            history.save(TicketHistory.of(t, by, "status", from, target));
            t.setStatus(target);
        }
    }

    private TicketComment addComment(Ticket t, User author, String content, boolean internal) {
        TicketComment c = new TicketComment();
        c.setTicket(t);
        c.setAuthor(author);
        c.setContent(content.trim());
        c.setInternal(internal);
        if (!internal && author.getRole() != Role.REQUESTER && t.getFirstResponseAt() == null) {
            // Une première réponse publique du support vaut prise en compte au sens du SLA
            Instant now = Instant.now();
            t.setFirstResponseAt(now);
            t.setSlaResponseBreached(now.isAfter(t.getResponseDueAt()));
        }
        return comments.save(c);
    }

    private void assertCanView(Ticket t, User me) {
        if (me.getRole() == Role.REQUESTER && !isRequester(t, me)) {
            throw new AccessDeniedException("Ce ticket ne vous appartient pas");
        }
    }

    private void assertCanChangeStatus(Ticket t, User me, TicketStatus target) {
        switch (me.getRole()) {
            case ADMIN, MANAGER -> { /* toutes les transitions autorisées par la machine à états */ }
            case TECHNICIAN -> {
                if (t.getAssignee() == null || !t.getAssignee().getId().equals(me.getId())) {
                    throw new AccessDeniedException("Seul le technicien affecté peut faire évoluer ce ticket");
                }
                if (target == TicketStatus.CLOSED || target == TicketStatus.CANCELLED) {
                    throw new AccessDeniedException("La clôture et l'annulation relèvent du demandeur ou du responsable");
                }
            }
            case REQUESTER -> {
                if (!isRequester(t, me)) throw new AccessDeniedException("Ce ticket ne vous appartient pas");
                boolean allowed = (target == TicketStatus.CLOSED && t.getStatus() == TicketStatus.RESOLVED)
                    || (target == TicketStatus.IN_PROGRESS && t.getStatus() == TicketStatus.RESOLVED) // réouverture
                    || (target == TicketStatus.CANCELLED && t.getStatus() == TicketStatus.NEW);
                if (!allowed) throw new AccessDeniedException("Le demandeur peut seulement clôturer, rouvrir ou annuler avant prise en charge");
            }
        }
    }

    private boolean isRequester(Ticket t, User me) {
        return t.getRequester().getId().equals(me.getId());
    }

    private Ticket load(Long id) {
        return tickets.findById(id).orElseThrow(() -> new NotFoundException("Ticket", id));
    }

    private Category loadCategory(Long id) {
        return categories.findById(id).orElseThrow(() -> new NotFoundException("Catégorie", id));
    }

    private Category defaultCategory() {
        return categories.findByCode("OTHER").orElseThrow(() -> new NotFoundException("Catégorie", "OTHER"));
    }

    private Site loadSite(Long id) {
        return sites.findById(id).orElseThrow(() -> new NotFoundException("Site", id));
    }

    private Equipment loadEquipment(Long id) {
        return equipment.findById(id).orElseThrow(() -> new NotFoundException("Équipement", id));
    }
}
