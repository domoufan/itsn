package sn.senetech.intervit.ticket;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import sn.senetech.intervit.domain.Ticket;
import sn.senetech.intervit.domain.TicketStatus;

import java.util.ArrayList;
import java.util.List;

/** Construction dynamique des filtres de la liste des tickets. */
public final class TicketSpecifications {
    private TicketSpecifications() {}

    public static Specification<Ticket> from(TicketDtos.TicketFilter f) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (f.status() != null) p.add(cb.equal(root.get("status"), f.status()));
            if (f.priority() != null) p.add(cb.equal(root.get("priority"), f.priority()));
            if (f.type() != null) p.add(cb.equal(root.get("type"), f.type()));
            if (f.assigneeId() != null) p.add(cb.equal(root.get("assignee").get("id"), f.assigneeId()));
            if (f.requesterId() != null) p.add(cb.equal(root.get("requester").get("id"), f.requesterId()));
            if (f.siteId() != null) p.add(cb.equal(root.get("site").get("id"), f.siteId()));
            if (f.categoryId() != null) p.add(cb.equal(root.get("category").get("id"), f.categoryId()));
            if (Boolean.TRUE.equals(f.openOnly())) {
                p.add(root.get("status").in(TicketStatus.NEW, TicketStatus.ASSIGNED, TicketStatus.IN_PROGRESS, TicketStatus.ON_HOLD));
            }
            if (Boolean.TRUE.equals(f.slaBreached())) {
                p.add(cb.or(cb.isTrue(root.get("slaResponseBreached")), cb.isTrue(root.get("slaResolutionBreached"))));
            }
            if (f.search() != null && !f.search().isBlank()) {
                String like = "%" + f.search().trim().toLowerCase() + "%";
                p.add(cb.or(
                    cb.like(cb.lower(root.get("reference")), like),
                    cb.like(cb.lower(root.get("title")), like),
                    cb.like(cb.lower(root.get("description")), like)));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
    }
}
