package com.company.olnaturaqr.support.workflow;

import com.company.olnaturaqr.domain.qr.QrLabel;
import com.company.olnaturaqr.support.security.AuthPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;

@Service
public class AdminStatusCorrectionService {

    private static final String STATUS_ONLY_FROM_DYNAMICS =
            "El estado operativo del lote solo puede cambiarse desde Dynamics";

    public record StatusCorrectionRequest(String status, String motivo) {}

    public record StatusCorrectionResult(QrLabel label, String from, String to, String motivo) {}

    public static List<String> allowedTargets(String currentStatus) {
        return Collections.emptyList();
    }

    public static boolean isAllowed(String fromRaw, String toRaw) {
        return false;
    }

    @Transactional
    public StatusCorrectionResult correct(QrLabel label, AuthPrincipal principal, StatusCorrectionRequest req) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, STATUS_ONLY_FROM_DYNAMICS);
    }
}
