package com.siaumkm.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Login aplikasi akuntansi (dipanggil Login.vue). Pesan gagal sengaja generik —
 * jangan bocorkan apakah username terdaftar atau password yang salah.
 */
@RestController
@RequestMapping("/app/auth")
public class AuthController {

    public record LoginRequest(String username, String password) {}
    public record LoginResponse(String token, String role, String nama) {}

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final String dummyHash; // anti username-enumeration via timing

    public AuthController(AppUserRepository appUserRepository,
                          PasswordEncoder passwordEncoder,
                          JwtService jwtService) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.dummyHash = passwordEncoder.encode("dummy-anti-timing-" + System.nanoTime());
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        AppUser user = appUserRepository.findByUsername(request.username()).orElse(null);

        // Selalu jalankan BCrypt matches — juga saat username tak terdaftar —
        // agar durasi respons tidak membocorkan username mana yang valid.
        String hash = user != null ? user.getPasswordHash() : dummyHash;
        boolean passwordCocok = passwordEncoder.matches(request.password(), hash);

        if (user == null || !user.isActive() || !passwordCocok) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(new LoginResponse(
                jwtService.terbitkan(user), user.getPeran().name(), user.getNama()));
    }
}
