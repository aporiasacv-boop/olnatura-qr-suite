package com.company.olnaturaqr.support.workflow;

import com.company.olnaturaqr.domain.qr.QrLabel;
import com.company.olnaturaqr.repository.AuditEventRepository;
import com.company.olnaturaqr.support.security.AuthPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ApprovalServiceBusinessRulesTest {

    @Mock AuditEventRepository auditEventRepository;

    ApprovalService service;

    @BeforeEach
    void setUp() {
        service = new ApprovalService(auditEventRepository);
        lenient().when(auditEventRepository.findByLoteAndActionTypeInOrderByCreatedAtAsc(any(), any()))
                .thenReturn(List.of());
    }

    private static AuthPrincipal principal(String... roles) {
        return new AuthPrincipal(UUID.randomUUID(), "tester", List.of(roles));
    }

    private static QrLabel label(String tipo, String status) {
        QrLabel q = new QrLabel();
        q.setTipoMaterial(tipo);
        q.setStatus(status);
        q.setAdminStatus("ACTIVE");
        q.setLote("LOTE-TEST-" + UUID.randomUUID());
        try {
            var f = QrLabel.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(q, UUID.randomUUID());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return q;
    }

    @Nested
    @DisplayName("Estado operativo solo desde Dynamics")
    class SoloDynamics {

        @Test
        void viewNuncaPermiteCambiarEstado() {
            QrLabel q = label(MaterialType.EMPAQUE_PRIMARIO, WorkflowStatus.CUARENTENA);
            for (String role : List.of("CALIDAD", "INSPECCION", "ADMIN", "ALMACEN")) {
                var view = service.view(q, principal(role));
                assertFalse(view.canApproveCalidad());
                assertFalse(view.canApproveInspeccion());
                assertFalse(view.canReject());
                assertFalse(view.canChangeStatus());
                assertNull(view.pendingMessage());
            }
        }

        @Test
        void calidadNoPuedeAprobar() {
            QrLabel q = label(MaterialType.MATERIA_PRIMA, WorkflowStatus.CUARENTENA);
            ResponseStatusException ex = assertThrows(
                    ResponseStatusException.class,
                    () -> service.approve(q, principal("CALIDAD"))
            );
            assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
            assertEquals(WorkflowStatus.CUARENTENA, q.getStatus());
        }

        @Test
        void inspeccionNoPuedeAprobar() {
            QrLabel q = label(MaterialType.EMPAQUE_SECUNDARIO, WorkflowStatus.CUARENTENA);
            ResponseStatusException ex = assertThrows(
                    ResponseStatusException.class,
                    () -> service.approve(q, principal("INSPECCION"))
            );
            assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
            assertEquals(WorkflowStatus.CUARENTENA, q.getStatus());
        }

        @Test
        void nadiePuedeRechazar() {
            QrLabel q = label(MaterialType.EMPAQUE_PRIMARIO, WorkflowStatus.CUARENTENA);
            for (String role : List.of("CALIDAD", "INSPECCION", "ADMIN")) {
                ResponseStatusException ex = assertThrows(
                        ResponseStatusException.class,
                        () -> service.reject(q, principal(role))
                );
                assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
            }
            assertEquals(WorkflowStatus.CUARENTENA, q.getStatus());
        }
    }
}
