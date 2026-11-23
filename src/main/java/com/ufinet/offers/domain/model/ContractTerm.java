package com.ufinet.offers.domain.model;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * Plazos de contrato permitidos y su descuento asociado.
 * <ul>
 *     <li>12 meses: 0 %</li>
 *     <li>24 meses: 5 %</li>
 *     <li>36 meses: 10 %</li>
 * </ul>
 */
public enum ContractTerm {

    TWELVE_MONTHS(12, new BigDecimal("0.00")),
    TWENTY_FOUR_MONTHS(24, new BigDecimal("0.05")),
    THIRTY_SIX_MONTHS(36, new BigDecimal("0.10"));

    private final int months;
    private final BigDecimal discount;

    ContractTerm(int months, BigDecimal discount) {
        this.months = months;
        this.discount = discount;
    }

    public int getMonths() {
        return months;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public static Optional<ContractTerm> fromMonths(Integer months) {
        return Arrays.stream(values())
                .filter(term -> Objects.equals(term.months, months))
                .findFirst();
    }

    public static boolean isAllowed(Integer months) {
        return fromMonths(months).isPresent();
    }
}
