package sn.senetech.intervit.catalog;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sn.senetech.intervit.common.NotFoundException;
import sn.senetech.intervit.domain.Site;
import sn.senetech.intervit.repository.SiteRepository;

import java.util.List;

@RestController
@RequestMapping("/api/sites")
public class SiteController {

    public record SiteRequest(@NotBlank @Size(max = 120) String name, @NotBlank @Size(max = 120) String clientName,
                              String address, String city, String contactPhone, Boolean active) {}

    public record SiteResponse(Long id, String name, String clientName, String address, String city, String contactPhone, boolean active) {
        static SiteResponse from(Site s) { return new SiteResponse(s.getId(), s.getName(), s.getClientName(), s.getAddress(), s.getCity(), s.getContactPhone(), s.isActive()); }
    }

    private final SiteRepository sites;

    public SiteController(SiteRepository sites) { this.sites = sites; }

    @GetMapping
    public List<SiteResponse> list() { return sites.findAll().stream().map(SiteResponse::from).toList(); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public SiteResponse create(@Valid @RequestBody SiteRequest req) {
        Site s = new Site();
        apply(s, req);
        return SiteResponse.from(sites.save(s));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public SiteResponse update(@PathVariable Long id, @Valid @RequestBody SiteRequest req) {
        Site s = sites.findById(id).orElseThrow(() -> new NotFoundException("Site", id));
        apply(s, req);
        return SiteResponse.from(sites.save(s));
    }

    private void apply(Site s, SiteRequest r) {
        s.setName(r.name()); s.setClientName(r.clientName()); s.setAddress(r.address()); s.setCity(r.city()); s.setContactPhone(r.contactPhone());
        if (r.active() != null) s.setActive(r.active());
    }
}
