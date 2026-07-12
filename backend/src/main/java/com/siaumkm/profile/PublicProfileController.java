package com.siaumkm.profile;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;

/**
 * SRS-A-01..06: endpoint publik (di bawah /public/**, permitAll — lihat SecurityConfig).
 * Karena deployment single-tenant per klien, TIDAK perlu parameter tenant — hanya
 * satu business_profile per instalasi. Response dicache (SRS-A-01) via anotasi
 * @Cacheable jika volume traffic klien memerlukan (tambahkan saat implementasi nyata).
 */
@RestController
@RequestMapping("/public/api")
public class PublicProfileController {

    private final BusinessProfileRepository profileRepository;
    private final ProductShowcaseRepository productRepository;
    private final GalleryItemRepository galleryRepository;
    private final TestimonialRepository testimonialRepository;
    private final BrandThemeRepository brandThemeRepository;

    public PublicProfileController(BusinessProfileRepository profileRepository,
                                    ProductShowcaseRepository productRepository,
                                    GalleryItemRepository galleryRepository,
                                    TestimonialRepository testimonialRepository,
                                    BrandThemeRepository brandThemeRepository) {
        this.profileRepository = profileRepository;
        this.productRepository = productRepository;
        this.galleryRepository = galleryRepository;
        this.testimonialRepository = testimonialRepository;
        this.brandThemeRepository = brandThemeRepository;
    }

    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getProfile() {
        BusinessProfile profile = profileRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException(
                    "business_profile belum di-setup — selesaikan wizard onboarding (Modul B1) dahulu."));

        List<ProductShowcase> products = productRepository
                .findByBusinessProfileIdOrderByUrutanTampilAsc(profile.getId());
        List<GalleryItem> gallery = galleryRepository
                .findByBusinessProfileIdOrderByUrutanTampilAsc(profile.getId());
        List<Testimonial> testimonials = testimonialRepository
                .findByBusinessProfileIdAndDitampilkanTrue(profile.getId());
        BrandTheme theme = brandThemeRepository.findByBusinessProfileId(profile.getId())
                .orElse(new BrandTheme());

        return ResponseEntity.ok(Map.of(
                "profile", profile,
                "products", products,
                "gallery", gallery,
                "testimonials", testimonials,
                "theme", theme
        ));
    }
}
