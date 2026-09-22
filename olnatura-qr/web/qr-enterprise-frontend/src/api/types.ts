export type Role = "ADMIN" | "ALMACEN" | "PRODUCCION" | "CALIDAD" | "INSPECCION" | "VALIDACION" | string;

export type Me = {
  id: string | number;
  username: string;
  roles: Role[];
  canCreateLoteComments?: boolean;
};

export type LoginRequest = {
  username: string;
  password: string;
};

export type RequestAccessPayload = {
  username: string;
  email: string;
  password: string;
  roleRequested: "ALMACEN" | "PRODUCCION" | "CALIDAD" | "INSPECCION" | "VALIDACION";
};

export type RequestAccessResponse = {
  requestId: string | number;
  status: string;
};

export type AccessRequestItem = {
  id: string | number;
  username: string;
  email: string;
  role: string;
  enabled: boolean;
  createdAt: string;
};

export type ProblemReportItem = {
  id: string;
  kind: string;
  lote?: string | null;
  reason: string;
  comment?: string | null;
  reporterUsername?: string | null;
  status: string;
  createdAt?: string | null;
  resolvedAt?: string | null;
  resolvedByUsername?: string | null;
};

export type ScanEvent = Record<string, any>;

export type LoteComment = {
  id: string;
  lote: string;
  userId: string;
  username: string;
  displayName: string;
  role: string;
  createdAt: string;
  comment: string;
};

export type DynamicsLookupResponse = {
  codigo: string;
  nombre: string | null;
  lote: string;
  caducidad: string | null;
  cantidadAlmacen: number | null;
  cantidadRecibida?: number | null;
  unidadInventario?: string | null;
  fechaEntrada?: string | null;
  statusDynamics: string | null;
  qualityOrderStatus?: string | null;
  passedBatchDispositionCode?: string | null;
  batchDispositionCode?: string | null;
  almacen: string | null;
  ubicacion: string | null;
  fuente: string;
  status?: string | null;
  operationalStatus?: string | null;
  operationalStatusRule?: string | null;
  statusSource?: string | null;
  platformStatus?: string | null;
  lastSyncedAt?: string | null;
  fechaLiberacion?: string | null;
  liberadoPor?: string | null;
};

export type ApprovalLeg = {
  approved?: boolean;
  actorEmail?: string | null;
  at?: string | null;
  rol?: string | null;
};

export type QrPermissions = {
  canChangeStatus: boolean;
  canRegisterScan: boolean;
  canCreateLabel: boolean;
  canApproveCalidad?: boolean;
  canApproveInspeccion?: boolean;
  canReject?: boolean;
  canDownloadAuditPdf?: boolean;
  canCorrectLabel?: boolean;
  canCorrectStatus?: boolean;
  allowedStatusCorrections?: string[];
  calidadApproved?: boolean;
  inspeccionApproved?: boolean;
  pendingMessage?: string | null;
  tipoMaterialDisplay?: string | null;
  calidad?: ApprovalLeg | null;
  inspeccion?: ApprovalLeg | null;
};

export type QrResponse = {
  label: Record<string, any>;
  dynamic: Record<string, any>;
  availableTransitions?: string[];
  permissions?: QrPermissions;
} & Record<string, any>;
