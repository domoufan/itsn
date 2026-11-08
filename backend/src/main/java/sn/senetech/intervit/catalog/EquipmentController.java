package sn.senetech.intervit.catalog;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sn.senetech.intervit.common.BusinessException;
import sn.senetech.intervit.common.NotFoundException;
import sn.senetech.intervit.domain.Equipment;
import sn.senetech.intervit.domain.EquipmentStatus;
import sn.senetech.intervit.domain.EquipmentType;
import sn.senetech.intervit.repository.EquipmentRepository;
import sn.senetech.intervit.repository.SiteRepository;

import java.time.LocalDate;
import java.util.List;

/** Inventaire simplifié des équipements (CMDB) rattachés aux sites clients. */
@RestController
@RequestMapping("/api/equipment")
public class EquipmentController {

    public record EquipmentRequest(@NotBlank @Size(max = 40) String assetTag, @NotNull EquipmentType type, String brand, String model,
                                   String serialNumber, Long siteId, EquipmentStatus status, LocalDate purchaseDate, LocalDate warrantyEnd,
                                   @Size(max = 500) String notes) {}

    public record EquipmentResponse(Long id, String assetTag, EquipmentType type, String brand, String model, String serialNumber,
                                    Long siteId, String siteName, EquipmentStatus status, LocalDate purchaseDate, LocalDate warrantyEnd, String notes) {
        static EquipmentResponse from(Equipment e) {
            return new EquipmentResponse(e.getId(), e.getAssetTag(), e.getType(), e.getBrand(), e.getModel(), e.getSerialNumber(),
                e.getSite() == null ? null : e.getSite().getId(), e.getSite() == null ? null : e.getSite().getName(),
                e.getStatus(), e.getPurchaseDate(), e.getWarrantyEnd(), e.getNotes());
        }
    }

    private final EquipmentRepository equipment;
    private final SiteRepository sites;

    public EquipmentController(EquipmentRepository equipment, SiteRepository sites) { this.equipment = equipment; this.sites = sites; }

    @GetMapping
    public List<EquipmentResponse> list(@RequestParam(required = false) Long siteId) {
        List<Equipment> l = siteId == null ? equipment.findAll() : equipment.findBySiteIdOrderByAssetTag(siteId);
        return l.stream().map(EquipmentResponse::from).toList();
    }

    @GetMapping("/{id}")
    public EquipmentResponse get(@PathVariable Long id) {
        return EquipmentResponse.from(equipment.findById(id).orElseThrow(() -> new NotFoundException("Équipement", id)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','TECHNICIAN')")
    public EquipmentResponse create(@Valid @RequestBody EquipmentRequest req) {
        if (equipment.existsByAssetTag(req.assetTag())) throw new BusinessException("Numéro d'inventaire déjà existant");
        Equipment e = new Equipment();
        apply(e, req);
        return EquipmentResponse.from(equipment.save(e));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','TECHNICIAN')")
    public EquipmentResponse update(@PathVariable Long id, @Valid @RequestBody EquipmentRequest req) {
        Equipment e = equipment.findById(id).orElseThrow(() -> new NotFoundException("Équipement", id));
        apply(e, req);
        return EquipmentResponse.from(equipment.save(e));
    }

    private void apply(Equipment e, EquipmentRequest r) {
        e.setAssetTag(r.assetTag().trim().toUpperCase()); e.setType(r.type()); e.setBrand(r.brand()); e.setModel(r.model());
        e.setSerialNumber(r.serialNumber()); e.setPurchaseDate(r.purchaseDate()); e.setWarrantyEnd(r.warrantyEnd()); e.setNotes(r.notes());
        if (r.status() != null) e.setStatus(r.status());
        e.setSite(r.siteId() == null ? null : sites.findById(r.siteId()).orElseThrow(() -> new NotFoundException("Site", r.siteId())));
    }
}
