package com.siaumkm.profile;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ProductShowcaseRepository extends JpaRepository<ProductShowcase, UUID> {
    List<ProductShowcase> findByBusinessProfileIdOrderByUrutanTampilAsc(UUID businessProfileId);
}
