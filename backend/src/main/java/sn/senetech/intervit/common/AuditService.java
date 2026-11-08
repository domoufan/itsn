package sn.senetech.intervit.common;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import sn.senetech.intervit.domain.AuditLog;
import sn.senetech.intervit.repository.AuditLogRepository;

/** Journalisation des actions sensibles (connexion, changements de rôle, décisions sur l'IA...). */
@Service
public class AuditService {

    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Long actorId, String action, String targetType, Long targetId, String details) {
        repository.save(AuditLog.of(actorId, action, targetType, targetId, details, null));
    }
}
