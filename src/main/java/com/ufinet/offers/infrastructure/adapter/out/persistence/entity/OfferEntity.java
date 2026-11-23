package com.ufinet.offers.infrastructure.adapter.out.persistence.entity;

import com.ufinet.offers.domain.model.OfferStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.util.ArrayList;
import java.util.List;

/**
 * Entidad JPA de la tabla OFERTA.
 */
@Entity
@Table(name = "oferta")
public class OfferEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cliente", nullable = false, length = 100)
    private String client;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private OfferStatus status;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @OneToMany(mappedBy = "offer", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<ServiceEntity> services = new ArrayList<>();

    protected OfferEntity() {
        // requerido por JPA
    }

    public OfferEntity(Long id, String client, OfferStatus status, Long version) {
        this.id = id;
        this.client = client;
        this.status = status;
        this.version = version;
    }

    public void addService(ServiceEntity service) {
        service.setOffer(this);
        services.add(service);
    }

    public Long getId() {
        return id;
    }

    public String getClient() {
        return client;
    }

    public OfferStatus getStatus() {
        return status;
    }

    public Long getVersion() {
        return version;
    }

    public List<ServiceEntity> getServices() {
        return services;
    }
}
