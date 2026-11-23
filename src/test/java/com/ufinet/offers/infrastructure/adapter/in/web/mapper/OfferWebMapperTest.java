package com.ufinet.offers.infrastructure.adapter.in.web.mapper;

import com.ufinet.offers.OfferFixtures;
import com.ufinet.offers.application.port.in.CreateOfferCommand;
import com.ufinet.offers.domain.model.OfferStatus;
import com.ufinet.offers.domain.model.ServiceSpec;
import com.ufinet.offers.domain.model.ServiceType;
import com.ufinet.offers.infrastructure.adapter.in.web.dto.CreateOfferRequest;
import com.ufinet.offers.infrastructure.adapter.in.web.dto.OfferResponse;
import com.ufinet.offers.infrastructure.adapter.in.web.dto.OfferSummaryResponse;
import com.ufinet.offers.infrastructure.adapter.in.web.dto.ServiceRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Mapper web")
class OfferWebMapperTest {

    @Test
    @DisplayName("Convierte la petición en comando de dominio")
    void toCommand() {
        List<ServiceRequest> services = new ArrayList<>();
        services.add(new ServiceRequest(ServiceType.INTERNET, 100, 36, true));
        services.add(null);

        CreateOfferCommand command = OfferWebMapper.toCommand(new CreateOfferRequest("Cliente", services));

        assertThat(command.client()).isEqualTo("Cliente");
        assertThat(command.services()).containsExactly(new ServiceSpec(ServiceType.INTERNET, 100, 36, true));
    }

    @Test
    @DisplayName("Una lista de servicios nula se convierte en lista vacía")
    void nullServices() {
        assertThat(OfferWebMapper.toCommand(new CreateOfferRequest("Cliente", null)).services()).isEmpty();
    }

    @Test
    @DisplayName("Convierte la oferta en respuesta detallada")
    void toResponse() {
        OfferResponse response = OfferWebMapper.toResponse(OfferFixtures.draftOffer(3L));

        assertThat(response.id()).isEqualTo(3L);
        assertThat(response.cliente()).isEqualTo(OfferFixtures.CLIENT);
        assertThat(response.estado()).isEqualTo(OfferStatus.BORRADOR);
        assertThat(response.version()).isZero();
        assertThat(response.mrcTotal()).isEqualByComparingTo("343.75");
        assertThat(response.servicios()).hasSize(2);
        assertThat(response.servicios().get(0).tipo()).isEqualTo(ServiceType.INTERNET);
        assertThat(response.servicios().get(0).mbps()).isEqualTo(100);
        assertThat(response.servicios().get(0).plazoMeses()).isEqualTo(36);
        assertThat(response.servicios().get(0).viable()).isTrue();
        assertThat(response.servicios().get(0).mrc()).isEqualByComparingTo("225.00");
    }

    @Test
    @DisplayName("Convierte la oferta en resumen para el listado")
    void toSummary() {
        OfferSummaryResponse summary = OfferWebMapper.toSummary(OfferFixtures.offer(4L, OfferStatus.NO_VIABLE, false));

        assertThat(summary).isEqualTo(new OfferSummaryResponse(4L, OfferFixtures.CLIENT, OfferStatus.NO_VIABLE,
                new java.math.BigDecimal("343.75")));
    }
}
