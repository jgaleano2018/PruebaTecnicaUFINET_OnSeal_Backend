package com.ufinet.offers.domain.model;

import com.ufinet.offers.OfferFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Agregado Oferta")
class OfferTest {

    @Test
    @DisplayName("Una oferta nueva nace en BORRADOR sin id ni versión")
    void draftStartsInBorrador() {
        Offer offer = Offer.draft("Cliente", List.of(OfferFixtures.internet100Mbps36Months(true)));

        assertThat(offer.getStatus()).isEqualTo(OfferStatus.BORRADOR);
        assertThat(offer.isDraft()).isTrue();
        assertThat(offer.getId()).isNull();
        assertThat(offer.getVersion()).isNull();
    }

    @Test
    @DisplayName("El MRC total suma los servicios con 2 decimales (ejemplo = 343,75)")
    void mrcTotalIsTheSumOfServices() {
        Offer offer = OfferFixtures.draftOffer(1L);

        assertThat(offer.getMrcTotal()).isEqualByComparingTo("343.75");
        assertThat(offer.getMrcTotal().scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("Una oferta sin servicios tiene MRC total 0,00")
    void mrcTotalOfEmptyOfferIsZero() {
        Offer offer = new Offer(1L, "Cliente", OfferStatus.BORRADOR, 0L, List.of());

        assertThat(offer.getMrcTotal()).isEqualTo(new BigDecimal("0.00"));
        assertThat(offer.allServicesViable()).isTrue();
    }

    @Test
    @DisplayName("Detecta si todos los servicios son viables")
    void detectsViability() {
        assertThat(OfferFixtures.offer(1L, OfferStatus.BORRADOR, true).allServicesViable()).isTrue();
        assertThat(OfferFixtures.offer(1L, OfferStatus.BORRADOR, false).allServicesViable()).isFalse();
    }

    @Test
    @DisplayName("withStatus devuelve una copia con el nuevo estado")
    void withStatusReturnsCopy() {
        Offer draft = OfferFixtures.draftOffer(5L);

        Offer presented = draft.withStatus(OfferStatus.PRESENTADA);

        assertThat(presented).isNotSameAs(draft);
        assertThat(presented.getStatus()).isEqualTo(OfferStatus.PRESENTADA);
        assertThat(presented.isDraft()).isFalse();
        assertThat(presented.getId()).isEqualTo(5L);
        assertThat(presented.getVersion()).isEqualTo(draft.getVersion());
        assertThat(presented.getClient()).isEqualTo(draft.getClient());
        assertThat(presented.getServices()).isEqualTo(draft.getServices());
        assertThat(draft.getStatus()).isEqualTo(OfferStatus.BORRADOR);
    }

    @Test
    @DisplayName("La lista de servicios es inmutable y es una copia defensiva")
    void servicesAreImmutable() {
        List<OfferedService> source = new ArrayList<>(List.of(OfferFixtures.internet100Mbps36Months(true)));
        Offer offer = new Offer(1L, "Cliente", OfferStatus.BORRADOR, 0L, source);
        source.add(OfferFixtures.capacity50Mbps24Months(true));

        assertThat(offer.getServices()).hasSize(1);
        assertThatThrownBy(() -> offer.getServices().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Cliente, estado y servicios son obligatorios")
    void requiredFields() {
        List<OfferedService> services = List.of();
        assertThatNullPointerException().isThrownBy(() -> new Offer(1L, null, OfferStatus.BORRADOR, 0L, services));
        assertThatNullPointerException().isThrownBy(() -> new Offer(1L, "c", null, 0L, services));
        assertThatNullPointerException().isThrownBy(() -> new Offer(1L, "c", OfferStatus.BORRADOR, 0L, null));
    }

    @Test
    @DisplayName("El servicio expone sus datos y exige tipo, plazo y MRC")
    void offeredServiceAccessors() {
        OfferedService service = OfferFixtures.capacity50Mbps24Months(false);

        assertThat(service.getId()).isEqualTo(2L);
        assertThat(service.getType()).isEqualTo(ServiceType.CAPACIDAD);
        assertThat(service.getMbps()).isEqualTo(50);
        assertThat(service.getTerm()).isEqualTo(ContractTerm.TWENTY_FOUR_MONTHS);
        assertThat(service.getTermMonths()).isEqualTo(24);
        assertThat(service.isViable()).isFalse();
        assertThat(service.getMrc()).isEqualByComparingTo("118.75");

        BigDecimal mrc = BigDecimal.ONE;
        assertThatNullPointerException().isThrownBy(() ->
                new OfferedService(1L, null, 10, ContractTerm.TWELVE_MONTHS, true, mrc));
        assertThatNullPointerException().isThrownBy(() ->
                new OfferedService(1L, ServiceType.INTERNET, 10, null, true, mrc));
        assertThatNullPointerException().isThrownBy(() ->
                new OfferedService(1L, ServiceType.INTERNET, 10, ContractTerm.TWELVE_MONTHS, true, null));
    }
}
