import { useState } from "react";
import { ArrowRight, Check } from "@phosphor-icons/react";

const examples = {
  QUICK: { label: "첫 단어 검출", result: "바보", detail: "처음 검출한 단어를 확인합니다." },
  NORMAL: { label: "전체 단어 검출", result: "바보 · 멍청이", detail: "검출한 단어 목록을 확인합니다." },
  FILTER: { label: "문장 마스킹", result: "이런 ** 같은 ***.", detail: "검출한 단어를 가린 문장을 받습니다." },
} as const;

export default function FilterExample() {
  const [mode, setMode] = useState<keyof typeof examples>("FILTER");
  const example = examples[mode];
  return (
    <div className="filter-example" aria-label="필터링 모드 예시">
      <div className="example-heading"><span>모드별 처리 예시</span><span className="example-badge">예시</span></div>
      <div className="mode-picker" role="group" aria-label="처리 모드">
        {(Object.keys(examples) as Array<keyof typeof examples>).map((value) => (
          <button aria-pressed={value === mode} key={value} onClick={() => setMode(value)} type="button">{value}</button>
        ))}
      </div>
      <div className="example-input"><span className="field-caption">입력 문장</span><p>이런 바보 같은 멍청이.</p></div>
      <div className="example-output" aria-live="polite" aria-atomic="true">
        <span className="field-caption"><ArrowRight size={15} />{example.label}</span>
        <p>{example.result}</p>
        <small><Check size={14} />{example.detail}</small>
      </div>
      <p className="example-note">모드 차이를 설명하는 고정 예시입니다. 실제 검출 결과는 적용된 사전에 따라 달라질 수 있습니다.</p>
    </div>
  );
}
