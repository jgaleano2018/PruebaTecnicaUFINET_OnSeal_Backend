package com.ufinet.offers.infrastructure.adapter.out.persistence.entity;

import com.ufinet.offers.domain.model.ServiceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Entidad JPA de la tabla SERVICIO (relación N:1 con OFERTA).
 */
@Entity
@Table(name = "servicio")
public class ServiceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "oferta_id", nullable = false)
    private OfferEntity offer;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private ServiceType type;

    @Column(name = "mbps", nullable = false)
    private Integer mbps;

    @Column(name = "plazo_meses", nullable = false)
    private Integer termMonths;

    @Column(name = "viable", nullable = false)
    private Boolean viable;

    @Column(name = "mrc", nullable = false, precision = 12, scale = 2)
    private BigDecimal mrc;

    protected ServiceEntity() {
        // requerido por JPA
    }

    public ServiceEntity(Long id, ServiceType type, Integer mbps, Integer termMonths, Boolean viable, BigDecimal mrc) {
        this.id = id;
        this.type = type;
        this.mbps = mbps;
        this.termMonths = termMonths;
        this.viable = viable;
        this.mrc = mrc;
    }

    void setOffer(OfferEntity offer) {
        this.offer = offer;
    }

    public Long getId() {
        return id;
    }

    public OfferEntity getOffer() {
        return offer;
    }

    public ServiceType getType() {
        return type;
    }

    public Integer getMbps() {
        return mbps;
    }

    public Integer getTermMonths() {
        return termMonths;
    }

    public Boolean getViable() {
        return viable;
    }

    public BigDecimal getMrc() {
        return mrc;
    }
}
