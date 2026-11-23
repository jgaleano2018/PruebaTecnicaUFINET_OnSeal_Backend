package com.ufinet.offers.domain.service;

import com.ufinet.offers.OfferFixtures;
import com.ufinet.offers.domain.exception.InvalidOfferStateException;
import com.ufinet.offers.domain.model.Offer;
import com.ufinet.offers.domain.model.OfferStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Regla de presentación")
class PresentationPolicyTest {

    private final PresentationPolicy presentationPolicy = new PresentationPolicy();

    @Test
    @DisplayName("Test mínimo 2: presentar con todos los servicios viables -> PRESENTADA")
    void allViableServicesResultInPresented() {
        Offer draft = OfferFixtures.offer(1L, OfferStatus.BORRADOR, true);

        Offer presented = presentationPolicy.present(draft);

        assertThat(presented.getStatus()).isEqualTo(OfferStatus.PRESENTADA);
        assertThat(presented.getId()).isEqualTo(1L);
        assertThat(presented.getServices()).hasSize(2);
    }

    @Test
    @DisplayName("Test mínimo 3: presentar con un servicio no viable -> NO_VIABLE")
    void oneNonViableServiceResultsInNotViable() {
        Offer draft = OfferFixtures.offer(1L, OfferStatus.BORRADOR, false);

        Offer presented = presentationPolicy.present(draft);

        assertThat(presented.getStatus()).isEqualTo(OfferStatus.NO_VIABLE);
    }

    @ParameterizedTest
    @EnumSource(value = OfferStatus.class, names = {"PRESENTADA", "NO_VIABLE"})
    @DisplayName("Test mínimo 4 (dominio): una oferta que no está en BORRADOR no se puede presentar")
    void offerNotInDraftCannotBePresented(OfferStatus status) {
        Offer offer = OfferFixtures.offer(7L, status, true);

        assertThatThrownBy(() -> presentationPolicy.present(offer))
                .isInstanceOf(InvalidOfferStateException.class)
                .hasMessageContaining("7")
                .hasMessageContaining(status.name())
                .satisfies(ex -> {
                    InvalidOfferStateException stateException = (InvalidOfferStateException) ex;
                    assertThat(stateException.getOfferId()).isEqualTo(7L);
                    assertThat(stateException.getCurrentStatus()).isEqualTo(status);
                });
        assertThat(offer.getStatus()).isEqualTo(status);
    }

    @Test
    @DisplayName("No modifica la oferta original (inmutabilidad)")
    void doesNotMutateOriginalOffer() {
        Offer draft = OfferFixtures.draftOffer(1L);

        presentationPolicy.present(draft);

        assertThat(draft.getStatus()).isEqualTo(OfferStatus.BORRADOR);
    }

    @Test
    @DisplayName("Rechaza una oferta nula")
    void rejectsNullOffer() {
        assertThatNullPointerException().isThrownBy(() -> presentationPolicy.present(null));
    }
}
