package com.ufinet.offers.application.port.in;

import com.ufinet.offers.domain.model.ServiceSpec;

import java.util.List;

/**
 * Comando de creación de una oferta.
 */
public record CreateOfferCommand(String client, List<ServiceSpec> services) {
}
