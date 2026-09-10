import { useState } from "react";
import { MagnifyingGlass, Plus, X } from "@phosphor-icons/react";

type Menu = "dictionary" | "inquiries" | "users" | "keys";
type Row = { id: string; name: string; detail: string; kind: string; state: string; date: string; body?: string };
const examples: Record<Menu, Row[]> = {
  dictionary: [
    { id: "w1", name: "바보", detail: "기본 사전", kind: "비속어", state: "사용 중", date: "2026.09.10" },
    { id: "w2", name: "멍청이", detail: "기본 사전", kind: "비속어", state: "사용 중", date: "2026.09.09" },
    { id: "w3", name: "테스트 표현", detail: "검토용 예시", kind: "사용자 제안", state: "사용 안 함", date: "2026.09.08" },
  ],
  inquiries: [
    { id: "q1", name: "문맥에 따라 검출되는 표현을 검토해 주세요", detail: "개발자 A · support-a@example.test", kind: "단어 요청", state: "접수", date: "2026.09.10", body: "일반적인 대화에서도 특정 표현이 검출됩니다. 사전 적용 범위를 검토해 주세요. 예시 문의이며 실제 요청 내용이 아닙니다." },
    { id: "q2", name: "API Key 재발급 방법이 궁금합니다", detail: "개발자 B · support-b@example.test", kind: "일반 문의", state: "처리 중", date: "2026.09.09", body: "기존 연동에 사용하는 API Key를 교체하려고 합니다. 재발급 후 적용 절차를 안내해 주세요." },
    { id: "q3", name: "새로운 표현 추가를 요청합니다", detail: "개발자 C · support-c@example.test", kind: "단어 요청", state: "완료", date: "2026.09.08", body: "필터 사전에 새로운 표현을 추가할 수 있는지 문의합니다." },
  ],
  users: [
    { id: "u1", name: "운영 담당자", detail: "operator@example.test", kind: "관리자", state: "활성", date: "2026.09.10" },
    { id: "u2", name: "개발자 A", detail: "developer-a@example.test", kind: "일반 사용자", state: "활성", date: "2026.09.09" },
    { id: "u3", name: "개발자 B", detail: "developer-b@example.test", kind: "일반 사용자", state: "비활성", date: "2026.09.07" },
  ],
  keys: [
    { id: "k1", name: "커뮤니티 서비스", detail: "DEMO · •••• A001", kind: "개발자 A", state: "사용 중", date: "2026.09.10" },
    { id: "k2", name: "개발 환경", detail: "DEMO · •••• B002", kind: "개발자 B", state: "사용 중", date: "2026.09.09" },
    { id: "k3", name: "이전 프로젝트", detail: "DEMO · •••• C003", kind: "개발자 A", state: "만료", date: "2026.09.01" },
  ],
};
const config = {
  dictionary: { description: "검출할 표현과 사전 적용 상태를 관리합니다.", first: "표현", kind: "출처 / 유형", date: "수정일", filters: ["전체", "사용 중", "사용 안 함"] },
  inquiries: { description: "단어 요청과 서비스 문의를 확인하고 답변합니다.", first: "문의 내용", kind: "유형", date: "접수일", filters: ["전체", "단어 요청", "일반 문의"] },
  users: { description: "가입한 사용자와 계정 상태를 확인합니다.", first: "사용자", kind: "역할", date: "최근 접속", filters: ["전체", "관리자", "일반 사용자"] },
  keys: { description: "발급된 키의 소유자와 사용 상태를 확인합니다.", first: "API Key", kind: "소유자", date: "최근 사용", filters: ["전체", "사용 중", "만료"] },
};
// Mock state survives menu switches, but a reload restores the examples.
const mockRows = { ...examples };
export default function AdminMockPanels({ menu }: { menu: Menu }) {
  const [rows, setRows] = useState(mockRows[menu]);
  const [query, setQuery] = useState("");
  const [filter, setFilter] = useState("전체");
  const [selected, setSelected] = useState<Row | null>(null);
  const [adding, setAdding] = useState(false);
  const [word, setWord] = useState("");
  const [reply, setReply] = useState("");
  const [message, setMessage] = useState("");
  const c = config[menu];
  const visible = rows.filter(row => (filter === "전체" || row.state === filter || row.kind === filter) && `${row.name} ${row.detail} ${row.kind}`.toLowerCase().includes(query.toLowerCase()));
  function update(next: Row[]) { mockRows[menu] = next; setRows(next); }
  function change(row: Row, changes: Partial<Row>, text: string) { update(rows.map(item => item.id === row.id ? { ...item, ...changes } : item)); setSelected(null); setMessage(text); }
  function close() { setSelected(null); setAdding(false); setReply(""); }
  return <div className="console-panel mock-panel">
    <header className="admin-header"><p>{c.description}</p>{menu === "dictionary" && <button className="compact-button filled" type="button" onClick={() => { setAdding(true); setWord(""); }}><Plus size={14} />표현 추가</button>}</header>
    {message && <div role="status" className="admin-feedback">{message}<button type="button" aria-label="알림 닫기" onClick={() => setMessage("")}><X size={14} /></button></div>}
    <div className="admin-list-toolbar"><div className="admin-status-tabs" role="group" aria-label="목록 필터">{c.filters.map(value => <button key={value} type="button" aria-pressed={filter === value} onClick={() => setFilter(value)}>{value}</button>)}</div><label className="updates-search"><MagnifyingGlass size={14} /><input aria-label={`${c.first} 검색`} placeholder={`${c.first} 검색`} value={query} onChange={event => setQuery(event.target.value)} /></label></div>
    <div className="mock-table-scroll"><table className="mock-table"><thead><tr><th>{c.first}</th><th>{c.kind}</th><th>상태</th><th>{c.date}</th><th><span className="sr-only">작업</span></th></tr></thead><tbody>{visible.map(row => <tr key={row.id}><td><button className="mock-title" type="button" onClick={() => { setSelected(row); setWord(row.name); setReply(""); }}>{row.name}</button><small>{row.detail}</small></td><td>{row.kind}</td><td><span className={`mock-state ${["활성", "사용 중", "완료"].includes(row.state) ? "is-active" : ""}`}>{row.state}</span></td><td><time>{row.date}</time></td><td><button className="mock-action" type="button" aria-label={`${row.name} 상세`} onClick={() => { setSelected(row); setWord(row.name); setReply(""); }}>상세</button></td></tr>)}</tbody></table>{!visible.length && <p className="updates-empty">조건에 맞는 항목이 없습니다.</p>}</div>
    <p className="admin-table-note">총 {visible.length}개 · 예시 데이터</p>
    {(selected || adding) && <dialog className="admin-dialog mock-dialog" aria-labelledby="mock-dialog-title" ref={node => { if (node && !node.open) node.showModal(); }} onCancel={close}><header><h2 id="mock-dialog-title">{adding ? "표현 추가" : `${c.first} 상세`}</h2><button type="button" className="mock-action" aria-label="상세 닫기" onClick={close}><X size={16} /></button></header>
      {menu === "dictionary" ? <label className="mock-field">표현<input autoFocus value={word} maxLength={80} onChange={event => setWord(event.target.value)} /></label> : selected && <><h3>{selected.name}</h3><p>{selected.detail}</p><dl><dt>{c.kind}</dt><dd>{selected.kind}</dd><dt>상태</dt><dd>{selected.state}</dd></dl></>}
      {menu === "inquiries" && selected && <><p className="mock-inquiry-body">{selected.body}</p><label className="mock-field">답변<textarea value={reply} onChange={event => setReply(event.target.value)} placeholder="답변을 작성하세요" /></label><p className="mock-hint">답변은 목업 안에서만 반영되며 실제로 전송되지 않습니다.</p></>}
      {menu === "keys" && <p className="mock-hint">만료 동작을 미리 확인하는 예시 키입니다.</p>}
      <div className="mock-dialog-actions"><button type="button" className="compact-button" onClick={close}>닫기</button>
        {menu === "dictionary" && <><button type="button" className="compact-button filled" disabled={!word.trim()} onClick={() => { if (rows.some(row => row.name === word.trim() && row.id !== selected?.id)) { setMessage("이미 등록된 표현입니다."); close(); return; } if (selected) change(selected, { name: word.trim() }, "목업에서 표현을 수정했습니다."); else { update([...rows, { id: crypto.randomUUID(), name: word.trim(), detail: "관리자 등록", kind: "사용자 제안", state: "사용 중", date: "2026.09.10" }]); close(); setMessage("목업에 표현을 추가했습니다."); } }}>저장</button>{selected && <button type="button" className="compact-button" onClick={() => change(selected, { state: selected.state === "사용 중" ? "사용 안 함" : "사용 중" }, "목업에서 적용 상태를 변경했습니다.")}>{selected.state === "사용 중" ? "사용 중지" : "사용하기"}</button>}</>}
        {menu === "inquiries" && selected && <button type="button" className="compact-button filled" disabled={!reply.trim()} onClick={() => change(selected, { state: "완료", body: `${selected.body}\n\n관리자 답변: ${reply.trim()}` }, "목업에 답변을 저장하고 문의를 완료했습니다.")}>답변 저장 · 완료</button>}
        {menu === "users" && selected && <button type="button" className="compact-button" onClick={() => change(selected, { state: selected.state === "활성" ? "비활성" : "활성" }, "목업에서 계정 상태를 변경했습니다.")}>{selected.state === "활성" ? "계정 비활성화" : "계정 활성화"}</button>}
        {menu === "keys" && selected && <button type="button" className="compact-button" disabled={selected.state === "만료"} onClick={() => change(selected, { state: "만료" }, "예시 키를 만료했습니다.")}>예시 키 만료</button>}
      </div></dialog>}
  </div>;
}
