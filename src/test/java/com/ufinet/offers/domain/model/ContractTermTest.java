package com.ufinet.offers.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Plazos de contrato")
class ContractTermTest {

    @ParameterizedTest
    @CsvSource({"12, TWELVE_MONTHS, 0.00", "24, TWENTY_FOUR_MONTHS, 0.05", "36, THIRTY_SIX_MONTHS, 0.10"})
    @DisplayName("Resuelve el plazo y su descuento")
    void resolvesTerm(int months, ContractTerm expected, String discount) {
        assertThat(ContractTerm.fromMonths(months)).contains(expected);
        assertThat(expected.getMonths()).isEqualTo(months);
        assertThat(expected.getDiscount()).isEqualByComparingTo(discount);
        assertThat(ContractTerm.isAllowed(months)).isTrue();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {0, 1, 13, 48})
    @DisplayName("Rechaza plazos no permitidos")
    void rejectsUnknownTerms(Integer months) {
        assertThat(ContractTerm.fromMonths(months)).isEmpty();
        assertThat(ContractTerm.isAllowed(months)).isFalse();
    }

    @Test
    @DisplayName("Solo existen tres plazos")
    void onlyThreeTerms() {
        assertThat(ContractTerm.values()).hasSize(3);
    }
}
