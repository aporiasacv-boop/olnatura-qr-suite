export type Role = "ADMIN" | "ALMACEN" | "PRODUCCION" | "CALIDAD" | "INSPECCION" | string;

export type Me = {
  id: string | number;
  username: string;
  roles: Role[];
};

export type LoginRequest = {
  username: string;
  password: string;
};

export type RequestAccessPayload = {
  username: string;
  email: string;
  password: string;
  roleRequested: "ALMACEN" | "PRODUCCION" | "CALIDAD" | "INSPECCION";
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
  unidadInventario?: string | null;
  /** MIN(DatePhysical) Received desde InventTrans; ISO Dynamics. */
  fechaEntrada?: string | null;
  statusDynamics: string | null;
  /** QualityOrderHeaders — diagnóstico / referencia. */
  qualityOrderStatus?: string | null;
  /** QualityOrderHeaders — diagnóstico / referencia. */
  passedBatchDispositionCode?: string | null;
  /** ItemBatches — diagnóstico / referencia. */
  batchDispositionCode?: string | null;
  almacen: string | null;
  ubicacion: string | null;
  fuente: string;
  /**
   * Estado Operativo (Dynamics vía OperationalStatusResolver). Solo lectura.
   * En GET /qr/{lote} el campo JSON es `status` (mismo significado).
   */
  status?: string | null;
  /** Alias tipado; preferir `status` del payload QR. */
  operationalStatus?: string | null;
  operationalStatusRule?: string | null;
  statusSource?: string | null;
  /** Estado de plataforma (`qr_labels.status`); no es el banner. */
  platformStatus?: string | null;
  /**
   * Momento de la última lectura OData exitosa (ISO-8601). No se persiste en BD.
   * Se actualiza en cada GET /qr/{lote} y POST /qr/{lote}/sync-dynamics.
   */
  lastSyncedAt?: string | null;
};

export type ApprovalLeg = {
  approved?: boolean;
  actorEmail?: string | null;
  at?: string | null;
  rol?: string | null;
};

export type QrPermissions = {
  /** Workflow interno (aprobar/rechazar plataforma). No modifica Estado Operativo Dynamics. */
  canChangeStatus: boolean;
  canRegisterScan: boolean;
  canCreateLabel: boolean;
  canApproveCalidad?: boolean;
  canApproveInspeccion?: boolean;
  canReject?: boolean;
  canDownloadAuditPdf?: boolean;
  canCorrectLabel?: boolean;
  /** Corrección admin de platformStatus (`qr_labels.status`). Nunca Estado Operativo. */
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
