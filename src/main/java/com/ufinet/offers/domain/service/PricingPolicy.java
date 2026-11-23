package com.ufinet.offers.domain.service;

import com.ufinet.offers.domain.model.ContractTerm;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Regla de precio (MRC mensual en USD).
 * <pre>
 * MRC = mbps × 2,50 × (1 − descuento)
 * </pre>
 * Descuento por plazo: 12 meses 0 %, 24 meses 5 %, 36 meses 10 %.
 * Redondeo HALF_UP a 2 decimales.
 */
public class PricingPolicy {

    public static final BigDecimal PRICE_PER_MBPS = new BigDecimal("2.50");
    public static final int MONEY_SCALE = 2;

    public BigDecimal calculateMrc(int mbps, ContractTerm term) {
        Objects.requireNonNull(term, "term");
        BigDecimal discountFactor = BigDecimal.ONE.subtract(term.getDiscount());
        return BigDecimal.valueOf(mbps)
                .multiply(PRICE_PER_MBPS)
                .multiply(discountFactor)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
