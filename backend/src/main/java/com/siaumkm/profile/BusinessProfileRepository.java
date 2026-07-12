package com.siaumkm.profile;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BusinessProfileRepository extends JpaRepository<BusinessProfile, UUID> {
    Optional<BusinessProfile> findByDomainKustom(String domainKustom);
}
