package sn.senetech.intervit.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "equipment")
public class Equipment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "asset_tag", nullable = false, unique = true, length = 40) private String assetTag;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private EquipmentType type;
    private String brand;
    private String model;
    @Column(name = "serial_number") private String serialNumber;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "site_id") private Site site;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private EquipmentStatus status = EquipmentStatus.IN_SERVICE;
    @Column(name = "purchase_date") private LocalDate purchaseDate;
    @Column(name = "warranty_end") private LocalDate warrantyEnd;
    private String notes;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public String getAssetTag() { return assetTag; }
    public void setAssetTag(String assetTag) { this.assetTag = assetTag; }
    public EquipmentType getType() { return type; }
    public void setType(EquipmentType type) { this.type = type; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }
    public Site getSite() { return site; }
    public void setSite(Site site) { this.site = site; }
    public EquipmentStatus getStatus() { return status; }
    public void setStatus(EquipmentStatus status) { this.status = status; }
    public LocalDate getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(LocalDate purchaseDate) { this.purchaseDate = purchaseDate; }
    public LocalDate getWarrantyEnd() { return warrantyEnd; }
    public void setWarrantyEnd(LocalDate warrantyEnd) { this.warrantyEnd = warrantyEnd; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Instant getCreatedAt() { return createdAt; }
}
