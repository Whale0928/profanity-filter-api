import { FormEvent, useMemo, useState } from "react";

import type { ClientRegisterRequest, ConsoleResponse, FilterMode, RequestKind } from "./types";

const API_BASE_URL = "https://api.kr-filter.com";

async function requestJson<TResponse>(endpoint: string, init: RequestInit): Promise<TResponse> {
  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...(init.headers ?? {}),
    },
  });
  const contentType = response.headers.get("content-type") ?? "";
  const body = contentType.includes("application/json") ? await response.json() : await response.text();

  if (!response.ok) {
    return {
      endpoint,
      error: `HTTP ${response.status}`,
      ...(typeof body === "object" && body !== null ? body : { status: { code: response.status, message: String(body) } }),
    } as TResponse;
  }

  return body as TResponse;
}

export function ConsoleDemo() {
  const [activeRequest, setActiveRequest] = useState<RequestKind>("filter");
  const [serviceName, setServiceName] = useState("");
  const [email, setEmail] = useState("");
  const [purpose, setPurpose] = useState("");
  const [apiKey, setApiKey] = useState("");
  const [text, setText] = useState("");
  const [mode, setMode] = useState<FilterMode>("FILTER");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [result, setResult] = useState<ConsoleResponse>(null);

  const requestPreview = useMemo(() => {
    if (activeRequest === "api-key") {
      return {
        endpoint: "POST /api/v1/clients/register",
        baseUrl: API_BASE_URL,
        body: buildClientRegisterRequest(serviceName, email, purpose),
      };
    }
    return {
      endpoint: "POST /api/v1/filter",
      baseUrl: API_BASE_URL,
      headers: { "x-api-key": apiKey },
      body: { text, mode },
    };
  }, [activeRequest, apiKey, email, mode, purpose, serviceName, text]);

  async function issueApiKey(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setActiveRequest("api-key");
    setIsSubmitting(true);
    try {
      const response = await requestJson<ConsoleResponse>("/api/v1/clients/register", {
        method: "POST",
        body: JSON.stringify(buildClientRegisterRequest(serviceName, email, purpose)),
      });
      setResult(response);
    } catch (error) {
      setResult(toClientError("/api/v1/clients/register", error));
    } finally {
      setIsSubmitting(false);
    }
  }

  async function filter(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setActiveRequest("filter");
    setIsSubmitting(true);
    try {
      const response = await requestJson<ConsoleResponse>("/api/v1/filter", {
        method: "POST",
        headers: { "x-api-key": apiKey },
        body: JSON.stringify({ text, mode }),
      });
      setResult(response);
    } catch (error) {
      setResult(toClientError("/api/v1/filter", error));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <main className="app-shell">
      <header className="hero">
        <div>
          <p className="eyebrow">profanity-filter-api console</p>
          <h1>API Key 발급과 필터 요청을 실제 서버로 확인합니다</h1>
          <p className="hero-copy">
            입력한 값으로 공개 API 서버에 요청을 보내고, 서버 응답 JSON을 그대로 확인합니다.
          </p>
        </div>
        <div className="status-panel" aria-label="service status">
          <span className="status-dot" />
          <strong>API 서버</strong>
          <span>{API_BASE_URL}</span>
        </div>
      </header>

      <section className="layout-grid">
        <div className="stacked">
          <form className="panel" onSubmit={issueApiKey}>
            <PanelTitle label="API Key" title="발급 요청" />
            <div className="form-grid">
              <Field label="서비스명">
                <input required value={serviceName} onChange={(event) => setServiceName(event.target.value)} />
              </Field>
              <Field label="이메일">
                <input required type="email" value={email} onChange={(event) => setEmail(event.target.value)} />
              </Field>
              <Field label="발급자 정보" wide>
                <textarea required value={purpose} onChange={(event) => setPurpose(event.target.value)} />
              </Field>
            </div>
            <button className="primary-button" disabled={isSubmitting} type="submit">
              API Key 발급 요청
            </button>
          </form>

          <form className="panel" onSubmit={filter}>
            <PanelTitle label="Console" title="비속어 필터 테스트" />
            <div className="form-grid">
              <Field label="API Key">
                <input required value={apiKey} onChange={(event) => setApiKey(event.target.value)} />
              </Field>
              <Field label="Mode">
                <select value={mode} onChange={(event) => setMode(event.target.value as FilterMode)}>
                  <option value="FILTER">FILTER</option>
                  <option value="NORMAL">NORMAL</option>
                  <option value="QUICK">QUICK</option>
                </select>
              </Field>
              <Field label="검사 문장" wide>
                <textarea required value={text} onChange={(event) => setText(event.target.value)} />
              </Field>
            </div>
            <button className="primary-button" disabled={isSubmitting} type="submit">
              필터 요청
            </button>
          </form>
        </div>

        <aside className="stacked sticky-column">
          <section className="panel">
            <PanelTitle label="Preview" title="요청과 응답" />
            <h3>Request</h3>
            <pre>{JSON.stringify(requestPreview, null, 2)}</pre>
            <h3>Response</h3>
            <pre>{result ? JSON.stringify(result, null, 2) : "요청 전입니다."}</pre>
          </section>
        </aside>
      </section>
    </main>
  );
}

function PanelTitle({ label, title }: { label: string; title: string }) {
  return (
    <div className="panel-title">
      <span>{label}</span>
      <h2>{title}</h2>
    </div>
  );
}

function Field({ label, wide, children }: { label: string; wide?: boolean; children: React.ReactNode }) {
  return (
    <label className={wide ? "field wide" : "field"}>
      <span>{label}</span>
      {children}
    </label>
  );
}

function buildClientRegisterRequest(name: string, email: string, issuerInfo: string): ClientRegisterRequest {
  return {
    name,
    email,
    issuerInfo,
    note: "sample-app console",
  };
}

function toClientError(endpoint: string, error: unknown): ConsoleResponse {
  return {
    endpoint,
    error: error instanceof Error ? error.message : "Unknown client error",
  };
}
