package sn.senetech.intervit.catalog;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sn.senetech.intervit.common.BusinessException;
import sn.senetech.intervit.domain.Priority;
import sn.senetech.intervit.domain.SlaPolicy;
import sn.senetech.intervit.repository.SlaPolicyRepository;
import sn.senetech.intervit.ticket.SlaService;

import java.util.List;

@RestController
@RequestMapping("/api/sla-policies")
public class SlaPolicyController {

    public record SlaRequest(@NotBlank String label, @Min(1) int responseMinutes, @Min(1) int resolutionMinutes) {}
    public record SlaResponse(Long id, Priority priority, String label, int responseMinutes, int resolutionMinutes) {
        static SlaResponse from(SlaPolicy p) { return new SlaResponse(p.getId(), p.getPriority(), p.getLabel(), p.getResponseMinutes(), p.getResolutionMinutes()); }
    }

    private final SlaPolicyRepository repository;
    private final SlaService slaService;

    public SlaPolicyController(SlaPolicyRepository repository, SlaService slaService) { this.repository = repository; this.slaService = slaService; }

    @GetMapping
    public List<SlaResponse> list() { return repository.findAll().stream().sorted((a, b) -> a.getPriority().compareTo(b.getPriority())).map(SlaResponse::from).toList(); }

    @PutMapping("/{priority}")
    @PreAuthorize("hasRole('ADMIN')")
    public SlaResponse update(@PathVariable Priority priority, @Valid @RequestBody SlaRequest req) {
        if (req.resolutionMinutes() < req.responseMinutes()) throw new BusinessException("Le délai de résolution doit être supérieur ou égal au délai de réponse");
        SlaPolicy p = slaService.policyFor(priority);
        p.setLabel(req.label()); p.setResponseMinutes(req.responseMinutes()); p.setResolutionMinutes(req.resolutionMinutes());
        return SlaResponse.from(repository.save(p));
    }
}
