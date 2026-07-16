export type OpenApiDocument = {
  openapi?: string;
  info?: {
    title?: string;
    version?: string;
    summary?: string;
    description?: string;
  };
  tags?: Array<{ name: string; description?: string }>;
  paths?: Record<string, Record<string, OpenApiOperation | unknown>>;
  components?: unknown;
  servers?: unknown;
  security?: unknown;
};

export type OpenApiOperation = {
  description?: string;
  operationId?: string;
  responses?: unknown;
  summary?: string;
  tags?: string[];
};

export type ApiOperationNavigation = {
  method: string;
  path: string;
  slug: string;
  summary: string;
  tag: string;
};

export type ApiGroupNavigation = {
  name: string;
  operations: ApiOperationNavigation[];
  slug: string;
};

export type MarkdownNavigation = {
  anchor: string;
  title: string;
};

export type RemoteDocumentState<T> = {
  data: T | null;
  error: string;
  loading: boolean;
};
