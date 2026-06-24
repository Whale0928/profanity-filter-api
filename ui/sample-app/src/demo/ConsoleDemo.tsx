import { FormEvent, useMemo, useState } from "react";

type FilterMode = "QUICK" | "NORMAL" | "FILTER";
type RequestKind = "api-key" | "filter" | "inquiry";

type FilterResponse = {
  trackingId: string;
  status: {
    code: number;
    message: string;
    description: string;
  };
  detected: Array<{ length: number; filteredWord: string }>;
  filtered: string;
  elapsed: string;
};

type ApiIssueRequest = {
  serviceName: string;
  email: string;
  purpose: string;
};

const fakeApi = {
  issueApiKey: (request: ApiIssueRequest) => ({
    status: { code: 2000, message: "Ok" },
    data: {
      name: request.serviceName,
      email: request.email,
      apiKey: "this_is_test_key_example",
      note: request.purpose,
    },
  }),
  filterText: (text: string, mode: FilterMode): FilterResponse => ({
    trackingId: "018f-demo-7f2a",
    status: {
      code: mode === "QUICK" ? 2000 : 2000,
      message: "Ok",
      description: "샘플 응답입니다. 실제 API 호출은 아직 연결하지 않았습니다.",
    },
    detected: text.includes("개자식") ? [{ length: 3, filteredWord: "개자식" }] : [],
    filtered: mode === "FILTER" ? text.replaceAll("개자식", "***") : "",
    elapsed: "0.000083 s / 0.083 ms / 83 µs",
  }),
  sendInquiry: (email: string, title: string) => ({
    status: { code: 2020, message: "Accepted" },
    data: {
      email,
      title,
      next: "문의 API가 생기면 이 흐름을 실제 POST 요청으로 교체합니다.",
    },
  }),
};

export function ConsoleDemo() {
  const [activeRequest, setActiveRequest] = useState<RequestKind>("filter");
  const [serviceName, setServiceName] = useState("dead-whale-console");
  const [email, setEmail] = useState("hello@example.com");
  const [purpose, setPurpose] = useState("포트폴리오 댓글 필터링 테스트");
  const [apiKey, setApiKey] = useState("this_is_test_key_example");
  const [text, setText] = useState("민트초코는 아주 개자식이야");
  const [mode, setMode] = useState<FilterMode>("FILTER");
  const [inquiryTitle, setInquiryTitle] = useState("API 사용 문의");
  const [inquiryBody, setInquiryBody] = useState("무료 사용 범위와 요청 제한 정책이 궁금합니다.");
  const [result, setResult] = useState<object>(() => fakeApi.filterText(text, mode));

  const requestPreview = useMemo(() => {
    if (activeRequest === "api-key") {
      return {
        endpoint: "POST /api/v1/clients/register",
        body: { name: serviceName, email, issuerInfo: "web-console", note: purpose },
      };
    }
    if (activeRequest === "inquiry") {
      return {
        endpoint: "POST /api/v1/inquiries",
        note: "현재 백엔드에 없는 API입니다. FE 요구사항으로 먼저 분리해야 합니다.",
        body: { email, title: inquiryTitle, body: inquiryBody },
      };
    }
    return {
      endpoint: "POST /api/v1/filter",
      headers: { "x-api-key": apiKey },
      body: { text, mode },
    };
  }, [activeRequest, apiKey, email, inquiryBody, inquiryTitle, mode, purpose, serviceName, text]);

  function issueApiKey(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setActiveRequest("api-key");
    setResult(fakeApi.issueApiKey({ serviceName, email, purpose }));
  }

  function filter(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setActiveRequest("filter");
    setResult(fakeApi.filterText(text, mode));
  }

  function sendInquiry(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setActiveRequest("inquiry");
    setResult(fakeApi.sendInquiry(email, inquiryTitle));
  }

  return (
    <main className="app-shell">
      <header className="hero">
        <div>
          <p className="eyebrow">profanity-filter-api console sample</p>
          <h1>API 발급부터 테스트, 문의까지 한 화면에서 보는 FE 샘플</h1>
          <p className="hero-copy">
            실제 백엔드 호출은 아직 연결하지 않고, 화면 구조와 개발 단위를 판단할 수 있게 만든
            Vite + React + TypeScript 샘플입니다.
          </p>
        </div>
        <div className="status-panel" aria-label="service status">
          <span className="status-dot" />
          <strong>API 상태 예시</strong>
          <span>health OK / ping PONG</span>
        </div>
      </header>

      <section className="layout-grid">
        <div className="stacked">
          <form className="panel" onSubmit={issueApiKey}>
            <PanelTitle label="API Key" title="발급 요청" />
            <div className="form-grid">
              <Field label="서비스명">
                <input value={serviceName} onChange={(event) => setServiceName(event.target.value)} />
              </Field>
              <Field label="이메일">
                <input value={email} onChange={(event) => setEmail(event.target.value)} />
              </Field>
              <Field label="사용 목적" wide>
                <textarea value={purpose} onChange={(event) => setPurpose(event.target.value)} />
              </Field>
            </div>
            <button className="primary-button" type="submit">
              발급 요청 샘플 실행
            </button>
          </form>

          <form className="panel" onSubmit={filter}>
            <PanelTitle label="Console" title="비속어 필터 테스트" />
            <div className="form-grid">
              <Field label="API Key">
                <input value={apiKey} onChange={(event) => setApiKey(event.target.value)} />
              </Field>
              <Field label="Mode">
                <select value={mode} onChange={(event) => setMode(event.target.value as FilterMode)}>
                  <option value="FILTER">FILTER</option>
                  <option value="NORMAL">NORMAL</option>
                  <option value="QUICK">QUICK</option>
                </select>
              </Field>
              <Field label="검사 문장" wide>
                <textarea value={text} onChange={(event) => setText(event.target.value)} />
              </Field>
            </div>
            <button className="primary-button" type="submit">
              필터 요청 샘플 실행
            </button>
          </form>

          <form className="panel" onSubmit={sendInquiry}>
            <PanelTitle label="Support" title="문의 요청" />
            <div className="form-grid">
              <Field label="이메일">
                <input value={email} onChange={(event) => setEmail(event.target.value)} />
              </Field>
              <Field label="제목">
                <input value={inquiryTitle} onChange={(event) => setInquiryTitle(event.target.value)} />
              </Field>
              <Field label="내용" wide>
                <textarea value={inquiryBody} onChange={(event) => setInquiryBody(event.target.value)} />
              </Field>
            </div>
            <button className="secondary-button" type="submit">
              문의 요청 샘플 실행
            </button>
          </form>
        </div>

        <aside className="stacked sticky-column">
          <section className="panel">
            <PanelTitle label="Preview" title="요청과 응답" />
            <h3>Request</h3>
            <pre>{JSON.stringify(requestPreview, null, 2)}</pre>
            <h3>Response</h3>
            <pre>{JSON.stringify(result, null, 2)}</pre>
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
