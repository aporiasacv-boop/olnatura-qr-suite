package com.company.olnaturaqr.support.workflow;

import com.company.olnaturaqr.domain.qr.QrLabel;
import com.company.olnaturaqr.repository.LoteCommentRepository;
import com.company.olnaturaqr.repository.ProblemReportRepository;
import com.company.olnaturaqr.repository.QrLabelRepository;
import com.company.olnaturaqr.repository.ScanEventRepository;
import com.company.olnaturaqr.support.audit.AuditService;
import com.company.olnaturaqr.support.security.AuthPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminLotDeleteServiceTest {

    @Mock QrLabelRepository qrLabelRepository;
    @Mock LoteCommentRepository loteCommentRepository;
    @Mock ScanEventRepository scanEventRepository;
    @Mock ProblemReportRepository problemReportRepository;
    @Mock AuditService auditService;
    @InjectMocks AdminLotDeleteService service;

    @Test
    void deletesOperationalRecordsAndAuditsWithoutTouchingAuditStore() throws Exception {
        UUID id = UUID.randomUUID();
        QrLabel q = new QrLabel();
        q.setLote("LOTE-1");
        q.setPublicToken("tok123");
        java.lang.reflect.Field idField = QrLabel.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(q, id);
        when(qrLabelRepository.findById(id)).thenReturn(Optional.of(q));

        AuthPrincipal principal = new AuthPrincipal(UUID.randomUUID(), "admin", List.of("ADMIN"));
        AdminLotDeleteService.DeleteResult result = service.deleteLot(
                id,
                principal,
                new AdminLotDeleteService.DeleteRequest("ELIMINAR_LOTE", "LOTE-1")
        );

        assertEquals("LOTE-1", result.lote());
        assertEquals(id.toString(), result.id());
        verify(loteCommentRepository).deleteByLote("LOTE-1");
        verify(scanEventRepository).deleteByLote("LOTE-1");
        verify(problemReportRepository).deleteByLote("LOTE-1");
        verify(qrLabelRepository).delete(q);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> md = ArgumentCaptor.forClass(Map.class);
        verify(auditService).log(eq(principal), eq("ELIMINAR_LOTE"), eq("LOTE-1"), md.capture(), isNull());
        assertEquals("LOTE-1", md.getValue().get("lote"));
        assertEquals(id.toString(), md.getValue().get("labelId"));
        verify(auditService, never()).logUnauthenticated(any(), any(), any(), any());
    }

    @Test
    void requiresConfirmAndMatchingLote() {
        UUID id = UUID.randomUUID();
        AuthPrincipal principal = new AuthPrincipal(UUID.randomUUID(), "admin", List.of("ADMIN"));
        assertThrows(ResponseStatusException.class, () ->
                service.deleteLot(id, principal, new AdminLotDeleteService.DeleteRequest("NO", "LOTE-1")));
        QrLabel q = new QrLabel();
        q.setLote("LOTE-1");
        when(qrLabelRepository.findById(id)).thenReturn(Optional.of(q));
        assertThrows(ResponseStatusException.class, () ->
                service.deleteLot(id, principal, new AdminLotDeleteService.DeleteRequest("ELIMINAR_LOTE", "OTRO")));
        verify(qrLabelRepository, never()).delete(any());
    }
}
