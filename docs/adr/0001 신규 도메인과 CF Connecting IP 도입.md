# 1. 신규 도메인과 CF-Connecting-IP 기반 클라이언트 IP 보존

## Status
승인 (2026.06.19)

## Context
`records.ip`에 클라이언트의 실제 IP가 아닌 k3s 내부망 IP(`10.42.x`)가 기록되는 문제가 있었다.

- 원인은 klipper-lb의 L4 SNAT다. 패킷이 노드를 거치며 출발지 IP가 내부망으로 치환되어, 애플리케이션이나 Envoy Gateway 레벨에서는 원본 IP를 복구할 수 없다.
- 기존 운영 도메인 `api.profanity.kr-filter.com`은 2-level 서브도메인이라 Cloudflare 무료 Universal SSL이 인증서를 발급하지 못한다. 따라서 Cloudflare proxied(주황 구름)를 켤 수 없어 `CF-Connecting-IP` 헤더를 받을 수 없다.

## Decision
1-level 신규 도메인 `api.kr-filter.com`을 추가하고 Cloudflare proxied로 운영한다. 애플리케이션은 `HttpClient.getClientIP`에서 `CF-Connecting-IP` → `X-Forwarded-For`(첫 IP) → 폴백 헤더 → `getRemoteAddr` 순으로 실제 IP를 추출한다.

## Consequences
- 신규 도메인 경로의 요청은 실제 공인 IP가 기록된다 (운영 검증: `1.227.241.137` 확인).
- 기존 도메인 `api.profanity.kr-filter.com`은 DNS-only로 남아 여전히 내부망 IP가 기록된다 → 클라이언트를 신규 도메인으로 이전 유도하는 후속 과제가 남는다.
- 보안 주의: `CF-Connecting-IP`는 헤더라 위조 가능하므로, origin은 Cloudflare IP 대역만 허용해야 신뢰할 수 있다.

## Alternatives
- MetalLB(L2): Tailscale 오버레이(전 노드 100.x CGNAT, flannel vxlan) 환경과 맞지 않아 제외.
- hostNetwork: 파드를 호스트 네트워크에 직접 노출하는 방식으로 운영자가 거부.
- 기존 도메인에 유료 Cloudflare 플랜으로 2-level 인증서 확보: 비용 대비 신규 1-level 도메인이 단순.
