package com.siaumkm.identity;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface RelatedEntityRepository extends JpaRepository<RelatedEntity, UUID> {
    List<RelatedEntity> findByBusinessEntityId(UUID businessEntityId);
    long countByBusinessEntityIdAndOmzetTahunanDiketahuiIsNull(UUID businessEntityId);
}
