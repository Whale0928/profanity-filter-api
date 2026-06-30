export type HttpMethod = "get" | "post" | "put" | "patch" | "delete" | "options" | "head" | "trace";

export type MarkdownState = {
  content: string;
  source: "local" | "fallback";
  url: string;
};

export type OpenApiDocumentState = {
  document: OpenApiDocument | null;
  error: string;
};

export type OpenApiDocument = {
  openapi?: string;
  info?: {
    title?: string;
    version?: string;
    summary?: string;
    description?: string;
  };
  tags?: Array<{ name: string; description?: string }>;
  paths?: Record<string, Record<string, OperationObject | unknown>>;
  components?: unknown;
  servers?: unknown;
  security?: unknown;
};

export type OperationObject = {
  tags?: string[];
  summary?: string;
  description?: string;
  operationId?: string;
  parameters?: unknown;
  requestBody?: unknown;
  responses?: unknown;
};

export type SectionView = {
  name: string;
  slug: string;
  description?: string;
  operations: OperationView[];
};

export type OperationView = {
  method: HttpMethod;
  path: string;
  tagName: string;
  summary: string;
  operationId?: string;
};

export type OverviewLink = {
  title: string;
  anchor: string;
  level: number;
};
