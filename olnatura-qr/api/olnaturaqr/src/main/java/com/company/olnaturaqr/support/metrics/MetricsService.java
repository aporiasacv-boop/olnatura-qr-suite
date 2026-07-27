package com.company.olnaturaqr.support.metrics;

import com.company.olnaturaqr.domain.audit.AuditEvent;
import com.company.olnaturaqr.domain.user.User;
import com.company.olnaturaqr.repository.AuditEventRepository;
import com.company.olnaturaqr.repository.QrLabelRepository;
import com.company.olnaturaqr.repository.ScanEventRepository;
import com.company.olnaturaqr.repository.UserRepository;
import com.company.olnaturaqr.api.AuditEventView;
import com.company.olnaturaqr.support.workflow.AdminLotStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MetricsService {

    public static final ZoneId ZONE = ZoneId.of("America/Mexico_City");
    private static final int RECENT_LIMIT = 25;

    private final QrLabelRepository qrLabelRepository;
    private final ScanEventRepository scanEventRepository;
    private final AuditEventRepository auditEventRepository;
    private final UserRepository userRepository;

    public MetricsService(
            QrLabelRepository qrLabelRepository,
            ScanEventRepository scanEventRepository,
            AuditEventRepository auditEventRepository,
            UserRepository userRepository
    ) {
        this.qrLabelRepository = qrLabelRepository;
        this.scanEventRepository = scanEventRepository;
        this.auditEventRepository = auditEventRepository;
        this.userRepository = userRepository;
    }

    public OperationalMetrics snapshot(int rangeDays) {
        int days = Math.max(1, Math.min(rangeDays, 31));
        ZonedDateTime nowZ = ZonedDateTime.now(ZONE);
        LocalDate today = nowZ.toLocalDate();
        Instant startOfToday = today.atStartOfDay(ZONE).toInstant();
        Instant startOfTomorrow = today.plusDays(1).atStartOfDay(ZONE).toInstant();
        LocalDate rangeStartDate = today.minusDays(days - 1L);
        Instant rangeStart = rangeStartDate.atStartOfDay(ZONE).toInstant();

        long labelsToday = qrLabelRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                startOfToday, startOfTomorrow);
        long scansToday = scanEventRepository.countByCreatedAtInstantBetween(startOfToday, startOfTomorrow);
        long auditInRange = auditEventRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                rangeStart, startOfTomorrow);
        long activeLots = qrLabelRepository.countByAdminStatusIgnoreCase(AdminLotStatus.ACTIVE);

        Map<LocalDate, Long> labelsByDay = toDayMap(
                qrLabelRepository.countLabelsGroupedByDay(rangeStart, startOfTomorrow));
        Map<LocalDate, Long> scansByDay = toDayMap(
                scanEventRepository.countScansGroupedByDay(rangeStart, startOfTomorrow));

        List<DailyPoint> dailySeries = new ArrayList<>(days);
        for (int i = 0; i < days; i++) {
            LocalDate d = rangeStartDate.plusDays(i);
            dailySeries.add(new DailyPoint(
                    d.toString(),
                    labelsByDay.getOrDefault(d, 0L),
                    scansByDay.getOrDefault(d, 0L)
            ));
        }

        List<AuditEvent> recentEvents = auditEventRepository
                .findAllByOrderByCreatedAtDesc(PageRequest.of(0, RECENT_LIMIT))
                .getContent();
        Set<UUID> actorIds = recentEvents.stream()
                .map(AuditEvent::getActorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<UUID, User> actorsById = actorIds.isEmpty()
                ? Map.of()
                : userRepository.findAllById(actorIds).stream()
                        .collect(Collectors.toMap(User::getId, u -> u));
        List<RecentActivityItem> recent = recentEvents.stream()
                .map(e -> toRecent(e, actorsById.get(e.getActorId())))
                .toList();

        return new OperationalMetrics(
                Instant.now().toString(),
                days,
                new Summary(labelsToday, scansToday, activeLots, auditInRange),
                dailySeries,
                recent,
                resolveLastPowerBiExport()
        );
    }

    private LastPowerBiExport resolveLastPowerBiExport() {
        AuditEvent e = auditEventRepository.findFirstByActionTypeOrderByCreatedAtDesc(
                "EXPORT_EXECUTIVE_DASHBOARD");
        if (e == null) {
            return null;
        }
        Map<String, Object> md = e.getMetadata() != null ? e.getMetadata() : Map.of();
        return new LastPowerBiExport(
                e.getCreatedAt() != null ? e.getCreatedAt().toString() : null,
                e.getActorEmail(),
                asLong(md.get("labelsExported")),
                asLong(md.get("scansExported")),
                asLong(md.get("auditsExported")),
                asLong(md.get("usersExported"))
        );
    }

    private static Long asLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private RecentActivityItem toRecent(AuditEvent e, User actor) {
        AuditEventView view = AuditEventView.from(e, actor);
        return new RecentActivityItem(
                e.getId() != null ? e.getId().toString() : null,
                e.getCreatedAt() != null ? e.getCreatedAt().toString() : null,
                e.getActionType(),
                view.actionTypeDisplay(),
                view.actorDisplay(),
                view.actorRoleDisplay(),
                e.getLote(),
                e.getMetadata()
        );
    }

    private static Map<LocalDate, Long> toDayMap(List<Object[]> rows) {
        Map<LocalDate, Long> map = new HashMap<>();
        if (rows == null) return map;
        for (Object[] row : rows) {
            if (row == null || row.length < 2 || row[0] == null) continue;
            LocalDate day;
            if (row[0] instanceof java.sql.Date sqlDate) {
                day = sqlDate.toLocalDate();
            } else if (row[0] instanceof LocalDate ld) {
                day = ld;
            } else {
                day = LocalDate.parse(row[0].toString());
            }
            long count = row[1] instanceof Number n ? n.longValue() : 0L;
            map.put(day, count);
        }
        return map;
    }

    public record OperationalMetrics(
            String generatedAt,
            int rangeDays,
            Summary summary,
            List<DailyPoint> dailySeries,
            List<RecentActivityItem> recentActivity,
            LastPowerBiExport lastPowerBiExport
    ) {}

    public record LastPowerBiExport(
            String exportedAt,
            String actorEmail,
            Long labelsExported,
            Long scansExported,
            Long auditsExported,
            Long usersExported
    ) {}

    public record Summary(
            long labelsCreatedToday,
            long scansToday,
            long activeLots,
            long auditEventsInRange
    ) {}

    public record DailyPoint(String date, long labelsCreated, long scans) {}

    public record RecentActivityItem(
            String id,
            String createdAt,
            String actionType,
            String actionTypeDisplay,
            String actorDisplay,
            String actorRoleDisplay,
            String lote,
            Map<String, Object> metadata
    ) {}
}
