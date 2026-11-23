package com.ufinet.offers.infrastructure.config;

import com.ufinet.offers.application.port.out.OfferRepositoryPort;
import com.ufinet.offers.application.service.OfferApplicationService;
import com.ufinet.offers.domain.service.OfferFactory;
import com.ufinet.offers.domain.service.PresentationPolicy;
import com.ufinet.offers.domain.service.PricingPolicy;
import com.ufinet.offers.infrastructure.adapter.in.web.handler.ProblemDetailFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.Clock;

/**
 * Ensambla las capas: el dominio y la aplicación no dependen de Spring,
 * por eso sus objetos se registran aquí como beans.
 */
@Configuration
public class BeanConfiguration {

    @Bean
    public PricingPolicy pricingPolicy() {
        return new PricingPolicy();
    }

    @Bean
    public PresentationPolicy presentationPolicy() {
        return new PresentationPolicy();
    }

    @Bean
    public OfferFactory offerFactory(PricingPolicy pricingPolicy) {
        return new OfferFactory(pricingPolicy);
    }

    @Bean
    public OfferApplicationService offerApplicationService(OfferRepositoryPort offerRepositoryPort,
                                                           OfferFactory offerFactory,
                                                           PresentationPolicy presentationPolicy) {
        return new OfferApplicationService(offerRepositoryPort, offerFactory, presentationPolicy);
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public ProblemDetailFactory problemDetailFactory(Clock clock) {
        return new ProblemDetailFactory(clock);
    }

    /** Scheduler para ejecutar las llamadas bloqueantes de JPA fuera del event loop. */
    @Bean(name = "jpaScheduler", destroyMethod = "dispose")
    public Scheduler jpaScheduler() {
        return Schedulers.newBoundedElastic(
                Schedulers.DEFAULT_BOUNDED_ELASTIC_SIZE,
                Schedulers.DEFAULT_BOUNDED_ELASTIC_QUEUESIZE,
                "jpa");
    }
}
