package com.ufinet.offers;

import com.ufinet.offers.domain.model.ContractTerm;
import com.ufinet.offers.domain.model.Offer;
import com.ufinet.offers.domain.model.OfferStatus;
import com.ufinet.offers.domain.model.OfferedService;
import com.ufinet.offers.domain.model.ServiceType;

import java.math.BigDecimal;
import java.util.List;

/**
 * Datos de prueba reutilizables basados en el ejemplo del enunciado.
 */
public final class OfferFixtures {

    public static final String CLIENT = "Cliente Demo S.A.";

    private OfferFixtures() {
    }

    public static OfferedService internet100Mbps36Months(boolean viable) {
        return new OfferedService(1L, ServiceType.INTERNET, 100, ContractTerm.THIRTY_SIX_MONTHS, viable,
                new BigDecimal("225.00"));
    }

    public static OfferedService capacity50Mbps24Months(boolean viable) {
        return new OfferedService(2L, ServiceType.CAPACIDAD, 50, ContractTerm.TWENTY_FOUR_MONTHS, viable,
                new BigDecimal("118.75"));
    }

    public static Offer offer(Long id, OfferStatus status, boolean secondServiceViable) {
        return new Offer(id, CLIENT, status, 0L,
                List.of(internet100Mbps36Months(true), capacity50Mbps24Months(secondServiceViable)));
    }

    public static Offer draftOffer(Long id) {
        return offer(id, OfferStatus.BORRADOR, true);
    }

    public static String validCreateRequestJson() {
        return """
                {
                  "cliente": "Cliente Demo S.A.",
                  "servicios": [
                    { "tipo": "INTERNET", "mbps": 100, "plazoMeses": 36, "viable": true },
                    { "tipo": "CAPACIDAD", "mbps": 50, "plazoMeses": 24, "viable": true }
                  ]
                }
                """;
    }
}
