# 말조심하세욧 브랜드 에셋

후보 D "검열 바"로 확정된 마크와 배포용 파일입니다. 가려진 문장 세 줄을 그린 형태이며, 가운데 초록 막대가 검출되어 마스킹된 구간을 뜻합니다.

## 파일

| 파일 | 규격 | 용도 |
|---|---|---|
| `logo.svg` | viewBox 64×64 | 기본 마크. 밝은 배경에 씁니다. |
| `logo-inverse.svg` | viewBox 64×64 | 역상. 다크 배경에 씁니다. |
| `logo-mono.svg` | viewBox 64×64 | 단색. 초록 막대 없이 씁니다. |
| `favicon.ico` | 16 · 32 · 48 | 브라우저 탭. 세 해상도가 한 파일에 들어 있습니다. |
| `favicon-16.png` `favicon-32.png` `favicon-48.png` `favicon-64.png` | 각 해당 크기 | 개별 PNG가 필요할 때 씁니다. |
| `apple-touch-icon.png` | 180 × 180 | iOS 홈 화면. 모서리는 iOS가 직접 깎으므로 사각형 그대로입니다. |
| `icon-192.png` `icon-512.png` | 192 · 512 | PWA maskable. 막대 묶음이 안전 영역 80% 안에 들어갑니다. |
| `og-image.png` | 1200 × 630 | `og:image`, `twitter:image`. |

## 색

| 이름 | 값 | 쓰이는 곳 |
|---|---|---|
| container | `#17211d` | 마크 바탕 |
| bar | `#f2f2eb` | 위아래 막대 |
| accent | `#63cf88` | 가운데 막대. 다크 바탕 위에서 씁니다. |
| accent (밝은 바탕) | `#2f8d57` | 역상 마크의 가운데 막대 |

## 좌표

viewBox는 `0 0 64 64`이며, 모든 좌표가 4의 배수라서 16px과 32px에서 막대와 간격이 정수 픽셀에 떨어집니다. 값을 임의로 바꾸면 작은 크기에서 막대가 서로 붙습니다.

```
컨테이너   x=4  y=8   w=56 h=48 rx=12
막대 1     x=12 y=16  w=32 h=8  rx=4
막대 2     x=12 y=28  w=24 h=8  rx=4   ← accent
막대 3     x=12 y=40  w=40 h=8  rx=4
```

## 적용

이 디렉토리는 원본 보관용입니다. Vite가 실제로 서빙하려면 `ui/app/public/`으로 복사한 뒤 `ui/app/index.html`에 아래를 넣습니다.

```html
<link rel="icon" href="/favicon.ico" sizes="any" />
<link rel="icon" type="image/svg+xml" href="/logo.svg" />
<link rel="apple-touch-icon" href="/apple-touch-icon.png" />
<meta property="og:image" content="https://developers.kr-filter.com/og-image.png" />
<meta name="twitter:image" content="https://developers.kr-filter.com/og-image.png" />
```

`twitter:card`는 현재 `summary`인데, `og-image.png`가 1200×630이므로 `summary_large_image`로 바꿔야 넓게 나옵니다.

## 쓰지 않는 방식

막대 개수와 길이 비율을 바꾸지 않습니다. 가운데 막대만 accent 색을 씁니다. 그라디언트와 그림자를 얹지 않습니다. 화면에서는 16px, 인쇄에서는 6mm가 하한입니다.
