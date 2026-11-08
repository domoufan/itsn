package sn.senetech.intervit.ai;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sn.senetech.intervit.ai.AiDtos.AiStatus;
import sn.senetech.intervit.ai.AiDtos.ReviewRequest;
import sn.senetech.intervit.ai.AiDtos.SuggestionResponse;

import java.util.List;

/**
 * Pipeline agentique de triage : déclenchement, file de revue et décision humaine.
 * Réservé au personnel ; la décision d'application est réservée aux responsables et administrateurs.
 */
@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiSuggestionService service;

    public AiController(AiSuggestionService service) {
        this.service = service;
    }

    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','TECHNICIAN')")
    public AiStatus status() {
        return service.status();
    }

    @PostMapping("/tickets/{ticketId}/triage")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','TECHNICIAN')")
    public SuggestionResponse triage(@PathVariable Long ticketId, @RequestParam(defaultValue = "false") boolean fallback) {
        return service.triage(ticketId, fallback);
    }

    @GetMapping("/suggestions/pending")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public List<SuggestionResponse> pending() {
        return service.pending();
    }

    @GetMapping("/suggestions/ticket/{ticketId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','TECHNICIAN')")
    public List<SuggestionResponse> forTicket(@PathVariable Long ticketId) {
        return service.forTicket(ticketId);
    }

    @PostMapping("/suggestions/{id}/review")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public SuggestionResponse review(@PathVariable Long id, @Valid @RequestBody ReviewRequest request) {
        return service.review(id, request);
    }
}
