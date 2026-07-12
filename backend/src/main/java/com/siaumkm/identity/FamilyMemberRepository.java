package com.siaumkm.identity;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface FamilyMemberRepository extends JpaRepository<FamilyMember, UUID> {
    List<FamilyMember> findByBusinessEntityId(UUID businessEntityId);
}
