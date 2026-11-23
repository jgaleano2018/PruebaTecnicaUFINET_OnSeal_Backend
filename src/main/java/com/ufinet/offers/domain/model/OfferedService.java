package com.ufinet.offers.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Servicio incluido en una oferta, con su MRC ya calculado (inmutable).
 */
public final class OfferedService {

    public static final int MIN_MBPS = 10;
    public static final int MAX_MBPS = 10_000;

    private final Long id;
    private final ServiceType type;
    private final int mbps;
    private final ContractTerm term;
    private final boolean viable;
    private final BigDecimal mrc;

    public OfferedService(Long id, ServiceType type, int mbps, ContractTerm term, boolean viable, BigDecimal mrc) {
        this.id = id;
        this.type = Objects.requireNonNull(type, "type");
        this.mbps = mbps;
        this.term = Objects.requireNonNull(term, "term");
        this.viable = viable;
        this.mrc = Objects.requireNonNull(mrc, "mrc");
    }

    public Long getId() {
        return id;
    }

    public ServiceType getType() {
        return type;
    }

    public int getMbps() {
        return mbps;
    }

    public ContractTerm getTerm() {
        return term;
    }

    public int getTermMonths() {
        return term.getMonths();
    }

    public boolean isViable() {
        return viable;
    }

    public BigDecimal getMrc() {
        return mrc;
    }
}
