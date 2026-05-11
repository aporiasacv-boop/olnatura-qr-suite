package com.company.olnaturaqr.api;

import java.util.List;

public class UserDto {

    public record LoginRequest(
            String username,
            String password
    ) {}

    public record Response(
            String id,
            String username,
            String email,
            List<String> roles
    ) {}

    public record LoginResponse(
            Response user
    ) {}

    // ===== REQUEST ACCESS =====
    public record RequestAccessRequest(
            String username,
            String email,
            String password,
            String roleRequested // "ALMACEN" o "INSPECCION" (NO ADMIN)
    ) {}

    public record RequestAccessResponse(
            String requestId,
            String status // "PENDING"
    ) {}
}