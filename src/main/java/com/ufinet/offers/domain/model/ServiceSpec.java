package com.ufinet.offers.domain.model;

/**
 * Datos de entrada de un servicio antes de ser validado y tarifado.
 * Los campos son objetos (no primitivos) para poder validar valores ausentes.
 */
public record ServiceSpec(ServiceType type, Integer mbps, Integer termMonths, Boolean viable) {
}
