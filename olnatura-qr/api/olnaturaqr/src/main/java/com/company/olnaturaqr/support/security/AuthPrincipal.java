package com.company.olnaturaqr.support.security;

import java.util.List;
import java.util.UUID;

public record AuthPrincipal(
        UUID id,
        String username,
        List<String> roles
) {}