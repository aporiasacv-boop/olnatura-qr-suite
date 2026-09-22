package com.company.olnaturaqr.api;

import com.company.olnaturaqr.domain.user.Role;
import com.company.olnaturaqr.domain.user.User;
import com.company.olnaturaqr.repository.RoleRepository;
import com.company.olnaturaqr.repository.UserRepository;
import com.company.olnaturaqr.support.config.AuthCookieProperties;
import com.company.olnaturaqr.support.security.AuthPrincipal;
import com.company.olnaturaqr.support.audit.AuditService;
import com.company.olnaturaqr.support.security.CredentialRules;
import com.company.olnaturaqr.support.security.CookieWriter;
import com.company.olnaturaqr.support.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthCookieProperties cookieProps;
    private final AuditService auditService;

    public AuthController(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            AuthCookieProperties cookieProps,
            AuditService auditService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.cookieProps = cookieProps;
        this.auditService = auditService;
    }

    @PostMapping("/login")
public ResponseEntity<UserDto.LoginResponse> login(
        @RequestBody UserDto.LoginRequest request,
        HttpServletResponse response
) {
    String raw = request.username() == null ? "" : request.username().trim();
    String pwd = request.password() == null ? "" : request.password();

    if (raw.isBlank() || pwd.isBlank()) {
        log.info("LOGIN FAILED: blank username or password");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    boolean byEmail = raw.contains("@");
    var userOpt = byEmail
            ? userRepository.findByEmailIgnoreCase(raw)
            : userRepository.findByUsernameIgnoreCase(raw);

    if (userOpt.isEmpty()) {
        log.info("LOGIN FAILED: user not found (lookup={})", byEmail ? "email" : "username");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    User user = userOpt.get();

    if (!user.isEnabled()) {
        log.info("LOGIN FAILED: user disabled username={}", user.getUsername());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    if (!passwordEncoder.matches(pwd, user.getPasswordHash())) {
        log.info("LOGIN FAILED: password mismatch username={}", user.getUsername());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
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
    log.info("LOGIN SUCCESS username={}", user.getUsername());
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

   
    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal AuthPrincipal principal) {
        if (principal == null || principal.id() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userRepository.findById(principal.id()).orElse(null);
        boolean canCreateLoteComments = user != null && user.isCanCreateLoteComments();

        return ResponseEntity.ok(new MeResponse(
                principal.id().toString(),
                principal.username(),
                principal.roles(),
                canCreateLoteComments
        ));
    }

    @PostMapping("/request-access")
public ResponseEntity<?> requestAccess(@RequestBody UserDto.RequestAccessRequest req) {
    String username = req.username() == null ? "" : req.username().trim();
    String email = req.email() == null ? "" : req.email().trim();
    String password = req.password() == null ? "" : req.password();
    String roleName = req.roleRequested() == null ? "" : req.roleRequested().trim().toUpperCase();

    if (username.isBlank() || email.isBlank() || password.isBlank() || roleName.isBlank()) {
        return ResponseEntity.badRequest().body(new ErrorResponse("Datos incompletos"));
    }

    String emailErr = CredentialRules.emailError(email);
    if (emailErr != null) {
        return ResponseEntity.badRequest().body(new ErrorResponse(emailErr));
    }

    String passwordErr = CredentialRules.passwordError(password);
    if (passwordErr != null) {
        return ResponseEntity.badRequest().body(new ErrorResponse(passwordErr));
    }

    if (!roleName.equals("ALMACEN") && !roleName.equals("INSPECCION")
            && !roleName.equals("PRODUCCION") && !roleName.equals("CALIDAD")
            && !roleName.equals("VALIDACION")) {
        return ResponseEntity.badRequest().body(new ErrorResponse(
                "Rol inválido. Usa ALMACEN, PRODUCCION, CALIDAD, INSPECCION o VALIDACION."));
    }

    if (userRepository.existsByUsernameIgnoreCase(username)) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(
                "Ese usuario ya existe. Si olvidaste la contraseña, un administrador puede restablecerla en Usuarios."));
    }
    if (userRepository.existsByEmailIgnoreCase(email)) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(
                "Ese correo ya está registrado. Si olvidaste la contraseña, un administrador puede restablecerla en Usuarios."));
    }

    Role role = roleRepository.findByName(roleName)
            .orElseThrow(() -> new IllegalStateException("Rol no existe en DB: " + roleName));

    User u = new User();
    u.setUsername(username);
    u.setEmail(email.toLowerCase());
    u.setPasswordHash(passwordEncoder.encode(password));
    u.setRole(role);
    u.setEnabled(false);

    User saved = userRepository.save(u);

    auditService.logUnauthenticated("ACCESS_REQUEST", null,
            Map.of("username", username, "email", email.toLowerCase(), "roleRequested", roleName, "userId", saved.getId().toString()),
            null);

    return ResponseEntity.status(HttpStatus.CREATED)
            .body(new UserDto.RequestAccessResponse(saved.getId().toString(), "PENDING"));
}

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
            List<String> roles,
            boolean canCreateLoteComments
    ) {}

    public record ErrorResponse(String message) {}
}