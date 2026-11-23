package com.ufinet.offers.infrastructure.adapter.out.persistence.repository;

import com.ufinet.offers.infrastructure.adapter.out.persistence.entity.OfferEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio Spring Data JPA. Carga los servicios con fetch join para evitar N+1.
 */
public interface SpringDataOfferRepository extends JpaRepository<OfferEntity, Long> {

    @Query("select distinct o from OfferEntity o left join fetch o.services order by o.id")
    List<OfferEntity> findAllWithServices();

    @Query("select o from OfferEntity o left join fetch o.services where o.id = :id")
    Optional<OfferEntity> findByIdWithServices(@Param("id") Long id);
}
