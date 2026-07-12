package com.siaumkm.profile;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface BrandThemeRepository extends JpaRepository<BrandTheme, UUID> {
    Optional<BrandTheme> findByBusinessProfileId(UUID businessProfileId);
}
