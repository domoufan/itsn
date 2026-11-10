package sn.senetech.itsn.ticket;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import sn.senetech.itsn.domain.TicketSequence;
import sn.senetech.itsn.domain.TicketType;
import sn.senetech.itsn.repository.TicketSequenceRepository;

import java.time.Year;
import java.time.ZoneOffset;

/** Génère des références lisibles et uniques : INC-2026-00042, REQ-2026-00007. */
@Service
public class TicketReferenceService {

    private final TicketSequenceRepository repository;

    public TicketReferenceService(TicketSequenceRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public String next(TicketType type) {
        int year = Year.now(ZoneOffset.UTC).getValue();
        TicketSequence seq = repository.lockByYear(year).orElseGet(() -> repository.saveAndFlush(new TicketSequence(year)));
        int value = seq.next();
        repository.save(seq);
        return format(type, year, value);
    }

    static String format(TicketType type, int year, int value) {
        String prefix = type == TicketType.INCIDENT ? "INC" : "REQ";
        return "%s-%d-%05d".formatted(prefix, year, value);
    }
}
