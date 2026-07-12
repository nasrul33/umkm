package com.siaumkm.masterdata;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface ChartOfAccountRepository extends JpaRepository<ChartOfAccount, UUID> {
    Optional<ChartOfAccount> findByKodeAkun(String kodeAkun);
}
