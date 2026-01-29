# ArgoCD Image Updater 배포 계획

**작성일:** 2026-01-29
**대상 프로젝트:** profanity-filter-api
**배포 방식:** 빅뱅 (Big Bang)
**목표:** 릴리즈 태그 기반 자동 배포 구현

---

## 📋 목차

1. [현재 상태 분석](#현재-상태-분석)
2. [목표 아키텍처](#목표-아키텍처)
3. [구현 계획](#구현-계획)
4. [변경 파일 목록](#변경-파일-목록)
5. [설정값 및 Secret](#설정값-및-secret)
6. [배포 순서](#배포-순서)
7. [검증 방법](#검증-방법)
8. [롤백 방안](#롤백-방안)

---

## 현재 상태 분석

### 배포 구조
```yaml
레포지토리: profanity-filter-api
ArgoCD Application:
  - 소스: deploy 브랜치
  - Auto-sync: enabled
  - 이미지: ghcr.io/whale0928/profanity-api:deploy-877aa7d (하드코딩)

워크플로우:
  - release.yaml: 로그만 출력 (실제 배포 없음)
  - build_and_health_check.yml: Docker 로컬 테스트만
```

### 문제점
1. ❌ deploy 브랜치 수동 관리 필요
2. ❌ 이미지 태그 하드코딩 (수동 업데이트)
3. ❌ 릴리즈 태그 생성 시 배포 자동화 없음
4. ❌ main 브랜치 커밋마다 배포되는 구조 불가능

---

## 목표 아키텍처

### 배포 플로우
```
┌─────────────────────────────────────────────────────────┐
│ 1. Developer: GitHub Release v1.2.3 생성               │
└─────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────┐
│ 2. GitHub Actions (release.yaml)                        │
│    → Gradle 빌드                                         │
│    → Docker 이미지 빌드                                  │
│    → Zot 푸시: docker-registry.kr-filter.com/profanity-api:v1.2.3 │
└─────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────┐
│ 3. ArgoCD Image Updater (5분 간격)                      │
│    → Zot API 폴링                                        │
│    → v*.*.* 패턴 태그 발견                              │
│    → Kustomization images.newTag 업데이트               │
│    → ArgoCD 파라미터 오버라이드                          │
└─────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────┐
│ 4. ArgoCD Application                                   │
│    → Source: main 브랜치 (manifest 추적)                │
│    → Auto-sync: enabled                                 │
│    → 이미지 파라미터 변경 감지 → 배포 시작               │
└─────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────┐
│ 5. Kubernetes Cluster                                   │
│    → Pod 재시작 (Rolling Update)                        │
│    → 새 이미지 v1.2.3 적용 완료                          │
└─────────────────────────────────────────────────────────┘
```

### 배포 트리거 정책
| 이벤트 | ArgoCD 동작 |
|--------|-----------|
| main 브랜치 코드 커밋 | ❌ 배포 안됨 (이미지 태그 동일) |
| main 브랜치 manifest 변경 | ✅ 즉시 배포 (ConfigMap, Service 등) |
| Release v*.*.* 태그 생성 | ✅ 이미지 빌드 → Image Updater 감지 → 배포 |
| 수동 이미지 푸시 | ✅ Image Updater 감지 → 배포 |

---

## 구현 계획

### Phase 1: Platform에 Image Manager 설치

#### 1.1 디렉토리 생성
```bash
module.platform/
└── platform/
    └── image-manager/
        ├── README.md              # ArgoCD Image Updater 사용 명시
        ├── kustomization.yaml
        ├── 00-namespace.yaml
        ├── 10-rbac.yaml
        ├── 20-deployment.yaml
        └── 30-configmap.yaml
```

**폴더명 선택 이유:**
- 기능 중심 네이밍 (`monitoring`과 일관성)
- 향후 다른 이미지 관리 도구 추가 가능
- 프로젝트별 선택적 참조 지원

#### 1.2 RBAC 설정
```yaml
# 10-rbac.yaml
- ServiceAccount: argocd-image-updater
- Role: ArgoCD Application 읽기/쓰기 권한
- RoleBinding: ServiceAccount ↔ Role 연결
```

#### 1.3 Deployment 설정
```yaml
# 20-deployment.yaml
image: quay.io/argoprojlabs/argocd-image-updater:v0.14.0
args:
  - run
  - --interval=5m
  - --health-port=8080
  - --registries-conf-path=/app/config/registries.conf
  - --argocd-server-addr=argocd-server.argocd
```

#### 1.4 레지스트리 설정
```yaml
# 30-configmap.yaml
registries:
  - name: GitHub Container Registry
    prefix: ghcr.io
    api_url: https://ghcr.io
    default: true
    # GHCR은 public 레지스트리면 인증 불필요
```

#### 1.5 README 작성
```markdown
# platform/image-manager/README.md

# Image Manager

컨테이너 이미지 자동 업데이트 관리 컴포넌트

## Implementation

현재 구현: **ArgoCD Image Updater v0.14.0**
- Repository: https://github.com/argoproj-labs/argocd-image-updater
- Documentation: https://argocd-image-updater.readthedocs.io/

## Usage

각 Application의 `metadata.annotations`에 추가:

\`\`\`yaml
argocd-image-updater.argoproj.io/image-list: <name>=<registry>/<image>
argocd-image-updater.argoproj.io/<name>.update-strategy: semver
argocd-image-updater.argoproj.io/<name>.allow-tags: regexp:^v[0-9]+\.[0-9]+\.[0-9]+$
\`\`\`

## Supported Registries

- GitHub Container Registry (ghcr.io)
- Zot Self-Hosted (docker-registry.kr-filter.com)

## Future Plans

- Flux Image Automation Controller (선택적 추가)
- Keel (대안)
- 프로젝트별 이미지 매니저 선택 지원
```

#### 1.6 Platform Kustomization 업데이트
```yaml
# platform/kustomization.yaml
resources:
  - cert-manager
  - ingress-nginx
  - external-secrets
  - image-manager  # ← 추가
```

---

### Phase 2: profanity-filter-api 설정

#### 2.1 ArgoCD Application 수정
```yaml
# deploy/application.yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: profanity-filter
  namespace: argocd
  annotations:
    # ✅ Image Updater 활성화
    argocd-image-updater.argoproj.io/image-list: profanity-api=docker-registry.kr-filter.com/profanity-api
    argocd-image-updater.argoproj.io/profanity-api.update-strategy: semver
    argocd-image-updater.argoproj.io/profanity-api.allow-tags: regexp:^v[0-9]+\.[0-9]+\.[0-9]+$
    argocd-image-updater.argoproj.io/write-back-method: argocd
spec:
  source:
    targetRevision: main  # ✅ deploy → main 변경
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
```

#### 2.2 Kustomization 수정
```yaml
# deploy/overlays/production/kustomization.yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization

namespace: profanity-production

resources:
  - resources.yaml
  - deployment.yaml
  - external-secret.yaml

# ✅ 추가: Image Updater가 이 필드 업데이트
images:
  - name: docker-registry.kr-filter.com/profanity-api
    newName: docker-registry.kr-filter.com/profanity-api
    newTag: v1.0.0  # 초기값 (Image Updater가 변경)
```

#### 2.3 Deployment 수정
```yaml
# deploy/overlays/production/deployment.yaml
spec:
  containers:
    - name: profanity-api
      # ❌ 삭제: image: ghcr.io/whale0928/profanity-api:deploy-877aa7d
      # ✅ 변경: 태그 없이 (Kustomize가 주입)
      image: docker-registry.kr-filter.com/profanity-api
```

#### 2.4 Release Workflow 수정
```yaml
# .github/workflows/release.yaml
on:
  release:
    types: [published]

jobs:
  build-and-push:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout tag
        uses: actions/checkout@v4
        with:
          ref: ${{ github.event.release.tag_name }}

      - name: Setup Java 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: gradle

      - name: Configure 1Password
        uses: 1password/load-secrets-action/configure@v2
        with:
          service-account-token: ${{ secrets.OP_SERVICE_ACCOUNT_TOKEN }}

      - name: Load secrets
        uses: 1password/load-secrets-action@v2
        with:
          export-env: true
        env:
          ENV_FILE: op://instance/.env/.env

      - name: Create env file
        run: echo "${{ env.ENV_FILE }}" > .env

      - name: Build with Gradle
        run: ./gradlew :profanity-api:bootJar

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Login to Zot Registry
        uses: docker/login-action@v3
        with:
          registry: docker-registry.kr-filter.com
          username: ${{ secrets.ZOT_USERNAME }}
          password: ${{ secrets.ZOT_PASSWORD }}

      - name: Extract version
        id: version
        run: |
          TAG_NAME="${{ github.event.release.tag_name }}"
          echo "tag=${TAG_NAME}" >> $GITHUB_OUTPUT

      - name: Build and push Docker image
        uses: docker/build-push-action@v5
        with:
          context: .
          file: ./profanity-api/Dockerfile
          push: true
          tags: |
            docker-registry.kr-filter.com/profanity-api:${{ steps.version.outputs.tag }}
            docker-registry.kr-filter.com/profanity-api:latest
          cache-from: type=gha
          cache-to: type=gha,mode=max

      - name: Log deployment info
        run: |
          echo "✅ Image pushed successfully"
          echo "Registry: Zot Self-Hosted"
          echo "Image: docker-registry.kr-filter.com/profanity-api:${{ steps.version.outputs.tag }}"
          echo "ArgoCD Image Updater will detect this tag within 5 minutes"
```

---

## 변경 파일 목록

### module.platform (k8s-platform 저장소)

```
✅ 신규 생성:
  platform/image-manager/README.md
  platform/image-manager/kustomization.yaml
  platform/image-manager/00-namespace.yaml
  platform/image-manager/10-rbac.yaml
  platform/image-manager/20-deployment.yaml
  platform/image-manager/30-configmap.yaml

✅ 수정:
  platform/kustomization.yaml (resources에 image-manager 추가)
```

### profanity-filter-api 저장소

```
✅ 수정:
  deploy/application.yaml (annotations 추가, targetRevision 변경)
  deploy/overlays/production/kustomization.yaml (images 필드 추가)
  deploy/overlays/production/deployment.yaml (이미지 태그 제거)
  .github/workflows/release.yaml (이미지 빌드/푸시 로직 추가)
```

---

## 설정값 및 Secret

### ArgoCD Image Updater 설정

| 설정 | 값 | 설명 |
|------|---|------|
| `interval` | `5m` | 레지스트리 폴링 간격 |
| `argocd-server-addr` | `argocd-server.argocd` | ArgoCD 서버 주소 |
| `registries.prefix` | `docker-registry.kr-filter.com` | 레지스트리 prefix |
| `registries.api_url` | `https://docker-registry.kr-filter.com` | API 엔드포인트 |

### Application Annotations

| Annotation | 값 | 설명 |
|-----------|---|------|
| `image-list` | `profanity-api=docker-registry.kr-filter.com/profanity-api` | 추적할 이미지 |
| `update-strategy` | `semver` | Semantic Versioning |
| `allow-tags` | `regexp:^v[0-9]+\.[0-9]+\.[0-9]+$` | v1.2.3 형식만 허용 |
| `write-back-method` | `argocd` | Git commit 없이 ArgoCD 파라미터 사용 |

### GitHub Secrets

| Secret | 용도 | 생성 방법 |
|--------|------|----------|
| `ZOT_USERNAME` | Zot 레지스트리 인증 | htpasswd 사용자명 (hgkim 또는 신규) |
| `ZOT_PASSWORD` | Zot 레지스트리 인증 | htpasswd 비밀번호 |
| `OP_SERVICE_ACCOUNT_TOKEN` | 1Password 연동 | 이미 존재 |

---

## 배포 순서

### 1단계: Platform 배포 (Image Manager 설치)

```bash
# 1. module.platform 저장소로 이동
cd /Users/hgkim/workspace/etc/profanity-filter-api/module.platform

# 2. image-manager 디렉토리 및 파일 생성 (위 내용대로)
mkdir -p platform/image-manager

# 3. Git commit & push
git add platform/image-manager/
git add platform/kustomization.yaml
git commit -m "feat: add Image Manager (ArgoCD Image Updater) to platform"
git push origin main

# 4. ArgoCD 자동 동기화 대기 (약 3분)
# 또는 수동 sync
kubectl apply -k apps/

# 5. Image Updater Pod 확인
kubectl get pods -n argocd -l app.kubernetes.io/name=argocd-image-updater
kubectl logs -n argocd -l app.kubernetes.io/name=argocd-image-updater --tail=50
```

### 2단계: profanity-filter-api 설정 변경

```bash
# 1. profanity-filter-api 저장소로 이동
cd /Users/hgkim/workspace/etc/profanity-filter-api

# 2. 파일 수정 (위 내용대로)
#    - deploy/application.yaml
#    - deploy/overlays/production/kustomization.yaml
#    - deploy/overlays/production/deployment.yaml
#    - .github/workflows/release.yaml

# 3. Git commit & push to main
git add deploy/ .github/workflows/release.yaml
git commit -m "feat: integrate ArgoCD Image Updater for tag-based deployment"
git push origin main

# 4. ArgoCD Application 재배포 확인
kubectl get application -n argocd profanity-filter -o yaml
# annotations에 image-updater 설정 확인
```

### 3단계: 검증 (테스트 릴리즈)

```bash
# 1. GitHub에서 Release v1.0.0 생성 (또는 다음 버전)
# https://github.com/Whale0928/profanity-filter-api/releases/new

# 2. release.yaml 워크플로우 실행 확인
# https://github.com/Whale0928/profanity-filter-api/actions

# 3. Zot 레지스트리에 이미지 푸시 확인
# https://docker-registry.kr-filter.com (Zot UI)

# 4. Image Updater 로그 모니터링 (5분 이내)
kubectl logs -n argocd -l app.kubernetes.io/name=argocd-image-updater -f

# 5. ArgoCD Application 상태 확인
argocd app get profanity-filter
# 또는
kubectl get application -n argocd profanity-filter -o yaml | grep newTag

# 6. Pod 재시작 확인
kubectl get pods -n profanity-production -w

# 7. 새 이미지로 배포 완료 확인
kubectl describe pod -n profanity-production -l app=profanity-api | grep Image:
```

---

## 검증 방법

### 1. Image Updater 정상 동작 확인

```bash
# Pod 실행 확인
kubectl get pods -n argocd -l app.kubernetes.io/name=argocd-image-updater

# 로그 확인 (정상 케이스)
kubectl logs -n argocd -l app.kubernetes.io/name=argocd-image-updater --tail=100

# 예상 로그:
# INFO  Connecting to ArgoCD server at argocd-server.argocd
# INFO  Checking registry ghcr.io for new tags
# INFO  Found new tag v1.0.0 for profanity-api
# INFO  Updating application profanity-filter
```

### 2. Application Annotation 확인

```bash
kubectl get application -n argocd profanity-filter -o yaml | grep -A 10 annotations
```

**예상 출력:**
```yaml
annotations:
  argocd-image-updater.argoproj.io/image-list: profanity-api=ghcr.io/whale0928/profanity-api
  argocd-image-updater.argoproj.io/profanity-api.allow-tags: regexp:^v[0-9]+\.[0-9]+\.[0-9]+$
  argocd-image-updater.argoproj.io/profanity-api.update-strategy: semver
  argocd-image-updater.argoproj.io/write-back-method: argocd
```

### 3. 배포 시나리오 테스트

#### 시나리오 A: ConfigMap 변경 (즉시 배포되어야 함)
```bash
# 1. ConfigMap 수정 후 main에 push
# 2. ArgoCD 즉시 동기화 확인 (이미지 변경 없음)
# 3. Pod 재시작 없이 ConfigMap만 업데이트
```

#### 시나리오 B: 릴리즈 태그 생성 (5분 이내 배포)
```bash
# 1. GitHub Release v1.0.1 생성
# 2. GitHub Actions 워크플로우 성공 확인
# 3. 5분 이내 Image Updater 로그에서 태그 감지 확인
# 4. Pod 재시작 및 새 이미지 적용 확인
```

#### 시나리오 C: main 브랜치 코드 커밋 (배포 안됨)
```bash
# 1. 소스 코드 수정 후 main에 push (manifest 변경 없음)
# 2. ArgoCD Sync 안됨 (이미지 태그 동일)
# 3. Pod 재시작 없음
```

---

## 롤백 방안

### 긴급 롤백 (Image Manager 문제 발생 시)

```bash
# 1. Image Updater Deployment 스케일 다운
kubectl scale deployment -n argocd argocd-image-updater --replicas=0

# 2. Application annotations 제거
kubectl patch application -n argocd profanity-filter --type=json \
  -p='[{"op": "remove", "path": "/metadata/annotations/argocd-image-updater.argoproj.io~1image-list"}]'

# 3. deploy 브랜치로 targetRevision 복구
kubectl patch application -n argocd profanity-filter --type=merge \
  -p='{"spec":{"source":{"targetRevision":"deploy"}}}'

# 4. 이전 이미지 태그로 수동 배포
kubectl set image deployment/profanity-api -n profanity-production \
  profanity-api=docker-registry.kr-filter.com/profanity-api:v1.0.0
```

### 이미지 버전 롤백 (잘못된 릴리즈)

```bash
# 방법 1: Image Updater 우회하고 직접 이미지 변경
kubectl set image deployment/profanity-api -n profanity-production \
  profanity-api=ghcr.io/whale0928/profanity-api:v1.0.0

# 방법 2: Kustomization newTag 수정 후 ArgoCD sync
# deploy/overlays/production/kustomization.yaml의 newTag를 이전 버전으로 수정
# git push → ArgoCD sync

# 방법 3: ArgoCD Application 파라미터 오버라이드
argocd app set profanity-filter \
  -p image.tag=v1.0.0 \
  --grpc-web
```

### 완전 복구 (이전 구조로)

```bash
# 1. profanity-filter-api 저장소에서 변경사항 revert
git revert <commit-hash>
git push origin main

# 2. module.platform에서 Image Manager 제거
kubectl delete -k platform/image-manager/

# 3. platform/kustomization.yaml에서 image-manager 제거
git revert <commit-hash>
git push origin main
```

---

## 참고 자료

- [ArgoCD Image Updater 공식 문서](https://argocd-image-updater.readthedocs.io/)
- [Kustomize images 필드](https://kubectl.docs.kubernetes.io/references/kustomize/kustomization/images/)
- [GitHub Container Registry](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry)

---

## 체크리스트

### Platform 준비
- [ ] `platform/image-manager/` 디렉토리 생성
- [ ] `README.md` 작성
- [ ] `kustomization.yaml` 작성
- [ ] `00-namespace.yaml` 작성
- [ ] `10-rbac.yaml` 작성
- [ ] `20-deployment.yaml` 작성
- [ ] `30-configmap.yaml` 작성
- [ ] `platform/kustomization.yaml` 수정
- [ ] Git commit & push
- [ ] ArgoCD 배포 확인
- [ ] Image Updater Pod 정상 실행 확인

### Application 준비
- [ ] `deploy/application.yaml` annotations 추가
- [ ] `deploy/application.yaml` targetRevision 변경
- [ ] `deploy/overlays/production/kustomization.yaml` images 필드 추가
- [ ] `deploy/overlays/production/deployment.yaml` 이미지 태그 제거
- [ ] `.github/workflows/release.yaml` 빌드/푸시 로직 추가
- [ ] Git commit & push to main
- [ ] ArgoCD Application 재배포 확인

### 검증
- [ ] 테스트 릴리즈 생성 (v1.0.0 또는 다음 버전)
- [ ] GitHub Actions 워크플로우 성공 확인
- [ ] GHCR 이미지 푸시 확인
- [ ] Image Updater 로그에서 태그 감지 확인 (5분 이내)
- [ ] Pod 재시작 및 새 이미지 적용 확인
- [ ] Health check 통과 확인
- [ ] ConfigMap 변경 테스트 (즉시 배포)
- [ ] 코드 커밋 테스트 (배포 안됨)

---

**작성자:** Claude

**폴더명 결정:**
- ✅ `image-manager` 선택 (기능 중심 네이밍)
- 이유: 향후 다른 이미지 관리 도구 추가 및 프로젝트별 선택적 참조 지원

**향후 확장 계획:**
```
platform/image-manager/
├── README.md                      # 전체 개요
├── argocd-image-updater/          # 현재 구현
│   ├── kustomization.yaml
│   ├── 00-namespace.yaml
│   ├── 10-rbac.yaml
│   ├── 20-deployment.yaml
│   └── 30-configmap.yaml
├── flux-image-automation/         # 향후 추가 가능
│   └── ...
└── keel/                          # 향후 추가 가능
    └── ...
```

**검토 필요 사항:**
1. Dockerfile 경로 확인: `./profanity-api/Dockerfile` 존재 여부
2. 초기 이미지 태그: `v1.0.0`으로 시작할지, 현재 버전으로 시작할지
3. Image Updater 버전: `v0.14.0` (최신 안정 버전 확인 필요)
4. Zot 레지스트리 도메인: `docker-registry.kr-filter.com` 활성화 필요
5. Zot htpasswd: GitHub Actions용 계정 추가 또는 기존 hgkim 사용
6. GitHub Secrets: `ZOT_USERNAME`, `ZOT_PASSWORD` 추가 필요
7. ArgoCD namespace에 `zot-credentials` Secret 생성 필요