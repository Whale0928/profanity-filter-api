import { useState } from "react";
import type { FormEvent } from "react";

import type { PagePath } from "../../constants/pagePath";

type RegisterPageProps = {
  onNavigate: (path: PagePath) => void;
};

type RegisterFormState = {
  name: string;
  email: string;
  issuerInfo: string;
  note: string;
};

type IssuedClient = RegisterFormState & {
  apiKey: string;
};

const INITIAL_FORM_STATE: RegisterFormState = {
  name: "",
  email: "",
  issuerInfo: "",
  note: "",
};

export function RegisterPage({ onNavigate }: RegisterPageProps) {
  const [formState, setFormState] = useState<RegisterFormState>(INITIAL_FORM_STATE);
  const [issuedClient, setIssuedClient] = useState<IssuedClient | null>(null);
  const [copyState, setCopyState] = useState<"idle" | "copied">("idle");
  const [emailCodeSent, setEmailCodeSent] = useState(false);
  const [verificationCode, setVerificationCode] = useState("");
  const [emailVerified, setEmailVerified] = useState(false);

  const canSendEmailCode = Boolean(formState.email.trim());
  const canVerifyEmail = Boolean(emailCodeSent && verificationCode.trim().length === 6);
  const canIssue = Boolean(
    emailVerified && formState.name.trim() && formState.email.trim() && formState.issuerInfo.trim(),
  );

  function updateField(field: keyof RegisterFormState, value: string) {
    setFormState((current) => ({ ...current, [field]: value }));
    setCopyState("idle");
    setIssuedClient(null);

    if (field === "email") {
      setEmailCodeSent(false);
      setVerificationCode("");
      setEmailVerified(false);
    }
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!canIssue) {
      return;
    }

    setIssuedClient({
      ...formState,
      apiKey: createLocalApiKey(),
    });
    setCopyState("idle");
  }

  function sendEmailCode() {
    if (!canSendEmailCode) {
      return;
    }

    setEmailCodeSent(true);
    setVerificationCode("");
    setEmailVerified(false);
  }

  function verifyEmailCode() {
    if (!canVerifyEmail) {
      return;
    }

    setEmailVerified(true);
  }

  async function copyIssuedKey() {
    if (!issuedClient) {
      return;
    }

    await navigator.clipboard.writeText(issuedClient.apiKey);
    setCopyState("copied");
  }

  return (
    <section className="register-page" aria-labelledby="register-title">
      <h1 className="visually-hidden" id="register-title">
        키 발급
      </h1>

      <div className="register-panel">
        <form className="register-form" onSubmit={handleSubmit}>
          <div className="register-verification">
            <label>
              이메일
              <input
                autoComplete="email"
                onChange={(event) => updateField("email", event.target.value)}
                placeholder="user@example.com"
                required
                type="email"
                value={formState.email}
              />
            </label>
            <button disabled={!canSendEmailCode || emailVerified} onClick={sendEmailCode} type="button">
              인증번호 발송
            </button>
            {emailCodeSent ? (
              <>
                <label>
                  인증번호
                  <input
                    inputMode="numeric"
                    maxLength={6}
                    onChange={(event) => setVerificationCode(event.target.value.replace(/\D/g, ""))}
                    placeholder="000000"
                    type="text"
                    value={verificationCode}
                  />
                </label>
                <button disabled={!canVerifyEmail || emailVerified} onClick={verifyEmailCode} type="button">
                  인증번호 확인
                </button>
              </>
            ) : null}
          </div>
          <label>
            이름 또는 조직명
            <input
              autoComplete="organization"
              maxLength={50}
              minLength={2}
              onChange={(event) => updateField("name", event.target.value)}
              placeholder="대학과제 프로젝트 X"
              required
              type="text"
              value={formState.name}
            />
          </label>
          <label>
            발급자 정보
            <input
              maxLength={200}
              onChange={(event) => updateField("issuerInfo", event.target.value)}
              placeholder="개인 프로젝트 비속어 필터링"
              required
              type="text"
              value={formState.issuerInfo}
            />
          </label>
          <label>
            메모
            <textarea
              maxLength={500}
              onChange={(event) => updateField("note", event.target.value)}
              placeholder="테스트 환경에서 사용"
              rows={4}
              value={formState.note}
            />
          </label>

          <div className="register-actions">
            <button className="primary-link" disabled={!canIssue} type="submit">
              발급
            </button>
            <button className="secondary-link" onClick={() => onNavigate("/docs")} type="button">
              문서
            </button>
          </div>
        </form>

        <aside className="register-result" aria-live="polite" data-issued={issuedClient ? "true" : "false"}>
          {issuedClient ? (
            <>
              <span>발급됨</span>
              <output>{issuedClient.apiKey}</output>
              <button onClick={copyIssuedKey} type="button">
                {copyState === "copied" ? "복사됨" : "복사"}
              </button>
            </>
          ) : emailVerified ? (
            <span>인증됨</span>
          ) : (
            <span>인증 전</span>
          )}
        </aside>
      </div>
    </section>
  );
}

function createLocalApiKey() {
  const bytes = new Uint8Array(32);
  crypto.getRandomValues(bytes);

  return btoa(String.fromCharCode(...bytes))
    .replaceAll("+", "-")
    .replaceAll("/", "_")
    .replaceAll("=", "");
}
