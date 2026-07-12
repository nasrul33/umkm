package com.siaumkm.identity;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface BusinessEntityRepository extends JpaRepository<BusinessEntity, UUID> {
}
