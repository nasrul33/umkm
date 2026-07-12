package com.siaumkm.masterdata;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface CashAccountRepository extends JpaRepository<CashAccount, UUID> {}
