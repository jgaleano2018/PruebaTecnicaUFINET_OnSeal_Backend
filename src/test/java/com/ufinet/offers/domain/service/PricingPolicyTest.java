package com.ufinet.offers.domain.service;

import com.ufinet.offers.domain.model.ContractTerm;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

@DisplayName("Regla de precio (MRC)")
class PricingPolicyTest {

    private final PricingPolicy pricingPolicy = new PricingPolicy();

    @Test
    @DisplayName("Test mínimo 1: MRC para los tres plazos según el ejemplo del enunciado")
    void calculatesMrcForTheThreeTerms() {
        assertThat(pricingPolicy.calculateMrc(100, ContractTerm.TWELVE_MONTHS)).isEqualByComparingTo("250.00");
        assertThat(pricingPolicy.calculateMrc(50, ContractTerm.TWENTY_FOUR_MONTHS)).isEqualByComparingTo("118.75");
        assertThat(pricingPolicy.calculateMrc(100, ContractTerm.THIRTY_SIX_MONTHS)).isEqualByComparingTo("225.00");

        BigDecimal total = pricingPolicy.calculateMrc(100, ContractTerm.THIRTY_SIX_MONTHS)
                .add(pricingPolicy.calculateMrc(50, ContractTerm.TWENTY_FOUR_MONTHS));
        assertThat(total).isEqualByComparingTo("343.75");
    }

    @ParameterizedTest(name = "{0} Mbps a {1} meses = {2}")
    @CsvSource({
            "10, TWELVE_MONTHS, 25.00",
            "10000, TWELVE_MONTHS, 25000.00",
            "10000, THIRTY_SIX_MONTHS, 22500.00",
            "33, TWENTY_FOUR_MONTHS, 78.38",   // 78.375 -> HALF_UP
            "11, THIRTY_SIX_MONTHS, 24.75",
            "7, TWENTY_FOUR_MONTHS, 16.63"     // 16.625 -> HALF_UP
    })
    @DisplayName("Aplica el descuento y redondea HALF_UP a 2 decimales")
    void appliesDiscountAndRounding(int mbps, ContractTerm term, String expected) {
        BigDecimal mrc = pricingPolicy.calculateMrc(mbps, term);

        assertThat(mrc).isEqualByComparingTo(expected);
        assertThat(mrc.scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("Rechaza un plazo nulo")
    void rejectsNullTerm() {
        assertThatNullPointerException().isThrownBy(() -> pricingPolicy.calculateMrc(100, null));
    }
}
