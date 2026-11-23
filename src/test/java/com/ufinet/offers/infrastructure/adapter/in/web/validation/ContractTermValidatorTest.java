package com.ufinet.offers.infrastructure.adapter.in.web.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Validador @ValidContractTerm")
class ContractTermValidatorTest {

    private final ContractTermValidator validator = new ContractTermValidator();

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {12, 24, 36})
    @DisplayName("Acepta 12, 24, 36 y nulo (la obligatoriedad la valida @NotNull)")
    void acceptsAllowedValues(Integer months) {
        assertThat(validator.isValid(months, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 6, 18, 25, 48})
    @DisplayName("Rechaza otros plazos")
    void rejectsOtherValues(Integer months) {
        assertThat(validator.isValid(months, null)).isFalse();
    }
}
