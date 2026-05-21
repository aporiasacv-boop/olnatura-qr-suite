export type Role = "ADMIN" | "ALMACEN" | "INSPECCION" | string;

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
  roleRequested: "ALMACEN" | "INSPECCION";
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

export type QrPermissions = {
  canChangeStatus: boolean;
  canRegisterScan: boolean;
  canCreateLabel: boolean;
};

export type QrResponse = {
  label: Record<string, any>;
  dynamic: Record<string, any>;
  availableTransitions?: string[];
  permissions?: QrPermissions;
} & Record<string, any>;

export type DynamicsPreview = {
  itemNumber: string | null;
  productName: string | null;
  productType: string | null;
  productGroup: string | null;
  warehouse: string | null;
  site: string | null;
  availableQuantity: number | null;
  onHandQuantity: number | null;
  unit: string | null;
  batchNumber: string | null;
  batchAttribute: string | null;
  batchValue: string | null;
  qualityResult: string | null;
  qualityValue: number | null;
};