package sn.senetech.itsn.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import sn.senetech.itsn.config.AppProperties;
import sn.senetech.itsn.domain.Ticket;
import sn.senetech.itsn.domain.User;

/**
 * Notifications e-mail (création, affectation, résolution, dépassement SLA).
 * Désactivées par défaut ; en développement, Mailpit capture les messages (docker-compose).
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final AppProperties properties;

    public NotificationService(ObjectProvider<JavaMailSender> mailSender, AppProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    public void ticketCreated(Ticket t) {
        send(t.getRequester(), "[" + t.getReference() + "] Votre demande a été enregistrée",
            "Bonjour %s,\n\nVotre ticket %s « %s » a été enregistré avec la priorité %s.\nÉchéance de résolution cible : %s.\n\nL'équipe support SénéTech"
                .formatted(t.getRequester().getFullName(), t.getReference(), t.getTitle(), t.getPriority(), t.getResolutionDueAt()));
    }

    public void ticketAssigned(Ticket t) {
        if (t.getAssignee() == null) return;
        send(t.getAssignee(), "[" + t.getReference() + "] Ticket affecté",
            "Bonjour %s,\n\nLe ticket %s « %s » (priorité %s) vous a été affecté.\n\nITSN"
                .formatted(t.getAssignee().getFullName(), t.getReference(), t.getTitle(), t.getPriority()));
    }

    public void ticketResolved(Ticket t) {
        send(t.getRequester(), "[" + t.getReference() + "] Ticket résolu",
            "Bonjour %s,\n\nLe ticket %s a été résolu : %s\n\nMerci de confirmer la clôture ou de rouvrir le ticket si le problème persiste.\n\nL'équipe support SénéTech"
                .formatted(t.getRequester().getFullName(), t.getReference(), t.getResolutionSummary()));
    }

    public void slaBreached(Ticket t, String kind) {
        User target = t.getAssignee() != null ? t.getAssignee() : null;
        if (target == null) return;
        send(target, "[" + t.getReference() + "] Dépassement SLA (" + kind + ")",
            "Le ticket %s « %s » a dépassé son délai de %s (priorité %s).".formatted(t.getReference(), t.getTitle(), kind, t.getPriority()));
    }


    void send(User to, String subject, String body) {
        if (!properties.notifications().emailEnabled()) {
            log.info("[notification désactivée] à={} sujet={}", to.getEmail(), subject);
            return;
        }
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            log.warn("Aucun expéditeur mail configuré ; notification ignorée ({})", subject);
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(properties.notifications().from());
            msg.setTo(to.getEmail());
            msg.setSubject(subject);
            msg.setText(body);
            sender.send(msg);
        } catch (Exception e) {
            // Une panne de messagerie ne doit jamais bloquer le traitement d'un ticket
            log.error("Échec d'envoi de la notification « {} » : {}", subject, e.getMessage());
        }
    }
}
