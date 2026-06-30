export type FilterMode = "QUICK" | "NORMAL" | "FILTER";
export type RequestKind = "api-key" | "filter";

export type ApiStatus = {
  code: number;
  message: string;
  description?: string;
  DetailDescription?: string;
};

export type FilterResponse = {
  trackingId: string;
  status: ApiStatus;
  detected: Array<{ length: number; filteredWord: string }>;
  filtered: string;
  elapsed: string;
};

export type ClientRegisterRequest = {
  name: string;
  email: string;
  issuerInfo: string;
  note: string;
};

export type ClientRegisterResponse = {
  status: ApiStatus;
  data: {
    name: string;
    email: string;
    apiKey: string;
    note: string;
  };
  meta?: Record<string, unknown>;
};

export type ApiFailureResponse = {
  status?: ApiStatus;
  error?: string;
  endpoint?: string;
};

export type ConsoleResponse = FilterResponse | ClientRegisterResponse | ApiFailureResponse | null;
