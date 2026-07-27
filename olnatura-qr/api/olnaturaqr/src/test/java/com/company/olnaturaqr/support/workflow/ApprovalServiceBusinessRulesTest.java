package com.company.olnaturaqr.support.workflow;

import com.company.olnaturaqr.domain.qr.QrLabel;
import com.company.olnaturaqr.repository.AuditEventRepository;
import com.company.olnaturaqr.repository.QrLabelRepository;
import com.company.olnaturaqr.support.audit.AuditService;
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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Validación de reglas oficiales de liberación (no altera lógica de producción).
 */
@ExtendWith(MockitoExtension.class)
class ApprovalServiceBusinessRulesTest {

    @Mock QrLabelRepository qrLabelRepository;
    @Mock AuditService auditService;
    @Mock AuditEventRepository auditEventRepository;

    ApprovalService service;

    @BeforeEach
    void setUp() {
        service = new ApprovalService(qrLabelRepository, auditService, auditEventRepository);
        lenient().when(auditEventRepository.findByLoteAndActionTypeInOrderByCreatedAtAsc(any(), any()))
                .thenReturn(List.of());
        lenient().when(qrLabelRepository.save(any(QrLabel.class))).thenAnswer(inv -> inv.getArgument(0));
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
    @DisplayName("Regla 1 — Materia Prima solo Calidad")
    class MateriaPrima {

        @Test
        void calidadPuedeLiberar() {
            QrLabel q = label(MaterialType.MATERIA_PRIMA, WorkflowStatus.CUARENTENA);
            QrLabel saved = service.approve(q, principal("CALIDAD"));
            assertEquals(WorkflowStatus.APROBADO, saved.getStatus());
        }

        @Test
        void inspeccionNuncaPuedeAprobar() {
            QrLabel q = label(MaterialType.MATERIA_PRIMA, WorkflowStatus.CUARENTENA);
            ResponseStatusException ex = assertThrows(
                    ResponseStatusException.class,
                    () -> service.approve(q, principal("INSPECCION"))
            );
            assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
            assertFalse(service.view(q, principal("INSPECCION")).canApproveInspeccion());
            assertFalse(service.view(q, principal("INSPECCION")).canApproveCalidad());
        }
    }

    @Nested
    @DisplayName("Regla 2 — Empaque Secundario solo Inspección")
    class EmpaqueSecundario {

        @Test
        void inspeccionPuedeLiberar() {
            QrLabel q = label(MaterialType.EMPAQUE_SECUNDARIO, WorkflowStatus.CUARENTENA);
            QrLabel saved = service.approve(q, principal("INSPECCION"));
            assertEquals(WorkflowStatus.APROBADO, saved.getStatus());
        }

        @Test
        void calidadNuncaPuedeAprobar() {
            QrLabel q = label(MaterialType.EMPAQUE_SECUNDARIO, WorkflowStatus.CUARENTENA);
            ResponseStatusException ex = assertThrows(
                    ResponseStatusException.class,
                    () -> service.approve(q, principal("CALIDAD"))
            );
            assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
            assertFalse(service.view(q, principal("CALIDAD")).canApproveCalidad());
            assertFalse(service.view(q, principal("CALIDAD")).canApproveInspeccion());
        }
    }

    @Nested
    @DisplayName("Regla 3 / Casos A–D — Empaque Primario doble aprobación")
    class EmpaquePrimario {

        @Test
        @DisplayName("Caso A: sin aprobaciones → faltan ambas, no libera")
        void casoA() {
            QrLabel q = label(MaterialType.EMPAQUE_PRIMARIO, WorkflowStatus.CUARENTENA);
            var view = service.view(q, principal("CALIDAD"));
            assertEquals(WorkflowStatus.CUARENTENA, view.status());
            assertEquals("Falta aprobación de Calidad e Inspección", view.pendingMessage());
            assertFalse(view.calidadApproved());
            assertFalse(view.inspeccionApproved());
        }

        @Test
        @DisplayName("Caso B: solo Calidad → pendiente Inspección, no libera")
        void casoB() {
            QrLabel q = label(MaterialType.EMPAQUE_PRIMARIO, WorkflowStatus.CUARENTENA);
            q.setCalidadApprovedAt(Instant.parse("2026-01-01T10:00:00Z"));

            var view = service.view(q, principal("CALIDAD"));
            assertEquals(WorkflowStatus.CUARENTENA, view.status());
            assertEquals("Falta aprobación de Inspección", view.pendingMessage());
            assertTrue(view.calidadApproved());
            assertFalse(view.inspeccionApproved());
            assertFalse(view.canApproveCalidad());
            assertTrue(service.view(q, principal("INSPECCION")).canApproveInspeccion());
        }

        @Test
        @DisplayName("Caso C: solo Inspección → pendiente Calidad, no libera")
        void casoC() {
            QrLabel q = label(MaterialType.EMPAQUE_PRIMARIO, WorkflowStatus.CUARENTENA);
            q.setInspeccionApprovedAt(Instant.parse("2026-01-01T10:00:00Z"));

            var view = service.view(q, principal("INSPECCION"));
            assertEquals(WorkflowStatus.CUARENTENA, view.status());
            assertEquals("Falta aprobación de Calidad", view.pendingMessage());
            assertFalse(view.calidadApproved());
            assertTrue(view.inspeccionApproved());
            assertFalse(view.canApproveInspeccion());
            assertTrue(service.view(q, principal("CALIDAD")).canApproveCalidad());
        }

        @Test
        @DisplayName("Caso D: ambas → Liberado (APROBADO)")
        void casoD() {
            QrLabel q = label(MaterialType.EMPAQUE_PRIMARIO, WorkflowStatus.CUARENTENA);
            service.approve(q, principal("CALIDAD"));
            assertEquals(WorkflowStatus.CUARENTENA, q.getStatus());

            q.setCalidadApprovedAt(Instant.parse("2026-01-01T10:00:00Z"));

            QrLabel saved = service.approve(q, principal("INSPECCION"));
            assertEquals(WorkflowStatus.APROBADO, saved.getStatus());

            q.setInspeccionApprovedAt(Instant.parse("2026-01-01T11:00:00Z"));
            var view = service.view(saved, principal("CALIDAD"));
            assertNull(view.pendingMessage());
            assertEquals(WorkflowStatus.APROBADO, view.status());
        }

        @Test
        @DisplayName("Independencia: Calidad no sustituye Inspección")
        void calidadNoSustituyeInspeccion() {
            QrLabel q = label(MaterialType.EMPAQUE_PRIMARIO, WorkflowStatus.CUARENTENA);
            q.setCalidadApprovedAt(Instant.parse("2026-01-01T10:00:00Z"));

            ResponseStatusException ex = assertThrows(
                    ResponseStatusException.class,
                    () -> service.approve(q, principal("CALIDAD"))
            );
            assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
            assertTrue(ex.getReason() != null && ex.getReason().contains("Inspección"));
            assertEquals(WorkflowStatus.CUARENTENA, q.getStatus());
        }

        @Test
        @DisplayName("Independencia: Inspección no sustituye Calidad")
        void inspeccionNoSustituyeCalidad() {
            QrLabel q = label(MaterialType.EMPAQUE_PRIMARIO, WorkflowStatus.CUARENTENA);
            q.setInspeccionApprovedAt(Instant.parse("2026-01-01T10:00:00Z"));

            ResponseStatusException ex = assertThrows(
                    ResponseStatusException.class,
                    () -> service.approve(q, principal("INSPECCION"))
            );
            assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
            assertTrue(ex.getReason() != null && ex.getReason().contains("Calidad"));
            assertEquals(WorkflowStatus.CUARENTENA, q.getStatus());
        }
    }
}
