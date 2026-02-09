package com.company.olnaturaqr.api;

import com.company.olnaturaqr.domain.user.Role;
import com.company.olnaturaqr.domain.user.User;
import com.company.olnaturaqr.repository.RoleRepository;
import com.company.olnaturaqr.repository.UserRepository;
import com.company.olnaturaqr.support.config.AuthCookieProperties;
import com.company.olnaturaqr.support.security.AuthPrincipal;
import com.company.olnaturaqr.support.security.CookieWriter;
import com.company.olnaturaqr.support.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthCookieProperties cookieProps;

    public AuthController(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            AuthCookieProperties cookieProps
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.cookieProps = cookieProps;
    }

    @PostMapping("/login")
public ResponseEntity<UserDto.LoginResponse> login(
        @RequestBody UserDto.LoginRequest request,
        HttpServletResponse response
) {
    String raw = request.username() == null ? "" : request.username().trim();
    String pwd = request.password() == null ? "" : request.password();

    if (raw.isBlank() || pwd.isBlank()) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    // Permite login por username o email
    var userOpt = raw.contains("@")
            ? userRepository.findByEmailIgnoreCase(raw)
            : userRepository.findByUsernameIgnoreCase(raw);

    if (userOpt.isEmpty()) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    User user = userOpt.get();

    if (!user.isEnabled()) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    if (!passwordEncoder.matches(pwd, user.getPasswordHash())) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    if (raw.contains("@")) {
        String emailLower = raw.toLowerCase();
        if (!emailLower.endsWith("@olnatura.com")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    String jwt = jwtTokenProvider.generateToken(user);

    CookieWriter.setJwtCookie(
            response,
            cookieProps.name(),
            jwt,
            cookieProps.secure(),
            cookieProps.sameSite(),
            cookieProps.maxAgeSeconds()
    );

    return ResponseEntity.ok(new UserDto.LoginResponse(toResponse(user)));
}

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        CookieWriter.clearCookie(
                response,
                cookieProps.name(),
                cookieProps.secure(),
                cookieProps.sameSite()
        );
        return ResponseEntity.noContent().build();
    }

    /**
     * Contrato estable: GET /auth/me -> {id, username, roles}
     */
    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal AuthPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(new MeResponse(
                principal.id().toString(),
                principal.username(),
                principal.roles()
        ));
    }

    @PostMapping("/request-access")
public ResponseEntity<?> requestAccess(@RequestBody UserDto.RequestAccessRequest req) {
    // normaliza
    String username = req.username() == null ? "" : req.username().trim();
    String email = req.email() == null ? "" : req.email().trim();
    String password = req.password() == null ? "" : req.password();
    String roleName = req.roleRequested() == null ? "" : req.roleRequested().trim().toUpperCase();

    if (username.isBlank() || email.isBlank() || password.isBlank() || roleName.isBlank()) {
        return ResponseEntity.badRequest().body(new ErrorResponse("Datos incompletos"));
    }

    // Solo estos roles se pueden solicitar (ADMIN no)
    if (!roleName.equals("ALMACEN") && !roleName.equals("INSPECCION")) {
        return ResponseEntity.badRequest().body(new ErrorResponse("Rol inválido. Usa ALMACEN o INSPECCION."));
    }

    // Evita duplicados
    if (userRepository.existsByUsernameIgnoreCase(username)) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("Username ya existe"));
    }
    if (userRepository.existsByEmailIgnoreCase(email)) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("Email ya existe"));
    }

    Role role = roleRepository.findByName(roleName)
            .orElseThrow(() -> new IllegalStateException("Rol no existe en DB: " + roleName));

    User u = new User();
    u.setUsername(username);
    u.setEmail(email);
    u.setPasswordHash(passwordEncoder.encode(password));
    u.setRole(role);
    u.setEnabled(false); // pendiente de aprobación por admin

    User saved = userRepository.save(u);

    return ResponseEntity.status(HttpStatus.CREATED)
            .body(new UserDto.RequestAccessResponse(saved.getId().toString(), "PENDING"));
}

    // === helpers ===

    private static UserDto.Response toResponse(User u) {
    String roleName = (u.getRole() == null ? "UNKNOWN" : u.getRole().getName());
    return new UserDto.Response(
            u.getId().toString(),
            u.getUsername(),
            u.getEmail(),
            java.util.List.of(roleName)
    );
}

    public record MeResponse(
            String id,
            String username,
            List<String> roles
    ) {}

    public record ErrorResponse(String message) {}
}