package com.ufinet.offers.domain.service;

import com.ufinet.offers.domain.exception.DomainValidationException;
import com.ufinet.offers.domain.exception.ValidationError;
import com.ufinet.offers.domain.model.ContractTerm;
import com.ufinet.offers.domain.model.Offer;
import com.ufinet.offers.domain.model.OfferStatus;
import com.ufinet.offers.domain.model.ServiceSpec;
import com.ufinet.offers.domain.model.ServiceType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

@DisplayName("Fábrica de ofertas (invariantes del modelo)")
class OfferFactoryTest {

    private static final ServiceSpec VALID_SPEC = new ServiceSpec(ServiceType.INTERNET, 100, 36, true);

    private final OfferFactory factory = new OfferFactory(new PricingPolicy());

    @Test
    @DisplayName("Crea la oferta en BORRADOR y calcula el MRC de cada servicio")
    void createsDraftWithCalculatedMrc() {
        Offer offer = factory.createDraft("  Cliente Demo S.A.  ", List.of(
                new ServiceSpec(ServiceType.INTERNET, 100, 36, true),
                new ServiceSpec(ServiceType.CAPACIDAD, 50, 24, true)));

        assertThat(offer.getId()).isNull();
        assertThat(offer.getClient()).isEqualTo("Cliente Demo S.A.");
        assertThat(offer.getStatus()).isEqualTo(OfferStatus.BORRADOR);
        assertThat(offer.getServices()).extracting(s -> s.getMrc().toPlainString())
                .containsExactly("225.00", "118.75");
        assertThat(offer.getServices().get(0).getTerm()).isEqualTo(ContractTerm.THIRTY_SIX_MONTHS);
        assertThat(offer.getMrcTotal()).isEqualByComparingTo("343.75");
    }

    @Test
    @DisplayName("Requiere una política de precios")
    void requiresPricingPolicy() {
        assertThatNullPointerException().isThrownBy(() -> new OfferFactory(null));
    }

    @Nested
    @DisplayName("Validación del cliente")
    class ClientValidation {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("El cliente es obligatorio")
        void clientIsRequired(String client) {
            assertThat(errorsFor(client, List.of(VALID_SPEC)))
                    .containsExactly(new ValidationError("cliente", "El cliente es obligatorio"));
        }

        @Test
        @DisplayName("El cliente admite máximo 100 caracteres")
        void clientMaxLength() {
            assertThat(errorsFor("x".repeat(101), List.of(VALID_SPEC)))
                    .containsExactly(new ValidationError("cliente", "El cliente no puede superar 100 caracteres"));
            assertThat(factory.createDraft("x".repeat(100), List.of(VALID_SPEC)).getClient()).hasSize(100);
        }
    }

    @Nested
    @DisplayName("Validación de servicios")
    class ServicesValidation {

        @Test
        @DisplayName("Debe tener al menos 1 servicio")
        void requiresAtLeastOneService() {
            assertThat(errorsFor("Cliente", List.of())).extracting(ValidationError::field).containsExactly("servicios");
            assertThat(errorsFor("Cliente", null)).extracting(ValidationError::field).containsExactly("servicios");
        }

        @Test
        @DisplayName("Admite máximo 5 servicios")
        void allowsAtMostFiveServices() {
            assertThat(factory.createDraft("Cliente", Collections.nCopies(5, VALID_SPEC)).getServices()).hasSize(5);
            assertThat(errorsFor("Cliente", Collections.nCopies(6, VALID_SPEC)))
                    .containsExactly(new ValidationError("servicios", "La oferta debe tener entre 1 y 5 servicios"));
        }

        @ParameterizedTest
        @ValueSource(ints = {9, 10_001, 0, -5})
        @DisplayName("Los Mbps deben estar entre 10 y 10.000")
        void mbpsOutOfRange(int mbps) {
            assertThat(errorsFor("Cliente", List.of(new ServiceSpec(ServiceType.INTERNET, mbps, 12, true))))
                    .containsExactly(new ValidationError("servicios[0].mbps", "Los Mbps deben estar entre 10 y 10000"));
        }

        @ParameterizedTest
        @ValueSource(ints = {10, 10_000})
        @DisplayName("Acepta los límites de Mbps")
        void mbpsLimitsAreAccepted(int mbps) {
            Offer offer = factory.createDraft("Cliente", List.of(new ServiceSpec(ServiceType.INTERNET, mbps, 12, true)));
            assertThat(offer.getServices().get(0).getMbps()).isEqualTo(mbps);
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 6, 18, 48})
        @DisplayName("El plazo solo puede ser 12, 24 o 36")
        void invalidTerm(int term) {
            assertThat(errorsFor("Cliente", List.of(new ServiceSpec(ServiceType.CAPACIDAD, 100, term, true))))
                    .containsExactly(new ValidationError("servicios[0].plazoMeses", "El plazo debe ser 12, 24 o 36 meses"));
        }

        @Test
        @DisplayName("Reporta todos los campos obligatorios ausentes con su ruta")
        void reportsAllMissingFields() {
            List<ServiceSpec> specs = new ArrayList<>();
            specs.add(VALID_SPEC);
            specs.add(new ServiceSpec(null, null, null, null));
            specs.add(null);

            assertThat(errorsFor("", specs)).extracting(ValidationError::field).containsExactly(
                    "cliente",
                    "servicios[1].tipo",
                    "servicios[1].mbps",
                    "servicios[1].plazoMeses",
                    "servicios[1].viable",
                    "servicios[2]");
        }
    }

    private List<ValidationError> errorsFor(String client, List<ServiceSpec> specs) {
        DomainValidationException ex = catchThrowableOfType(DomainValidationException.class,
                () -> factory.createDraft(client, specs));
        assertThat(ex).as("se esperaba DomainValidationException").isNotNull();
        assertThat(ex.getMessage()).isEqualTo("La oferta contiene datos inválidos");
        return ex.getErrors();
    }
}
