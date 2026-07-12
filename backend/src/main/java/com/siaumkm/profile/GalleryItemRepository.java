package com.siaumkm.profile;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface GalleryItemRepository extends JpaRepository<GalleryItem, UUID> {
    List<GalleryItem> findByBusinessProfileIdOrderByUrutanTampilAsc(UUID businessProfileId);
}
