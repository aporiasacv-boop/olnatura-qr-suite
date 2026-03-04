package com.company.olnaturaqr.support.workflow;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Allowed next states from current status.
 * Kept simple: all-to-all transitions (all valid statuses allowed from any
 * state).
 * Easy to adjust for stricter workflow rules later.
 */
public final class WorkflowTransitions {

    private static final List<String> ALL = Arrays.asList(
            WorkflowStatus.PENDING,
            WorkflowStatus.APROBADO,
            WorkflowStatus.LIBERADO,
            WorkflowStatus.RECHAZADO,
            WorkflowStatus.CUARENTENA,
            WorkflowStatus.DESCONOCIDO);

    private WorkflowTransitions() {
    }

    /**
     * Returns list of allowed next statuses from current.
     * For now: all valid statuses; DESCONOCIDO also allowed as transition target.
     */
    public static List<String> allowedFrom(String currentStatus) {
        String normalized = WorkflowStatus.normalize(currentStatus);
        if (normalized.equals(WorkflowStatus.DESCONOCIDO)) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(ALL);
    }
}
