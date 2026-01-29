# ArgoCD Image Updater v1.0.2 배포 가이드 (CRD 기반)

**작성일:** 2026-01-29
**버전:** v1.0.2 (CRD 기반)
**대상:** Zot Self-Hosted Registry 연동
**상태:** 구현 완료

---

## 📋 목차

1. [배포 완료 사항](#배포-완료-사항)
2. [아키텍처 개요](#아키텍처-개요)
3. [CRD 기반 vs Annotation 기반](#crd-기반-vs-annotation-기반)
4. [구현된 리소스](#구현된-리소스)
5. [ArgoCD Sync Wave 전략](#argocd-sync-wave-전략)
6. [ImageUpdater CR 생성 가이드](#imageupdater-cr-생성-가이드)
7. [트러블슈팅](#트러블슈팅)
8. [참고 자료](#참고-자료)

---

## 배포 완료 사항

### ✅ 완료된 작업

1. **CRD 설치**
   - `imageupdaters.argocd-image-updater.argoproj.io` CRD 설치
   - Sync Wave `-1`로 가장 먼저 배포

2. **Controller 배포**
   - Image: `quay.io/argoprojlabs/argocd-image-updater:v1.0.2`
   - ClusterRole 기반 RBAC (multi-namespace 지원)
   - Leader Election 비활성화 (단일 replica)
   - Zot Registry 인증 정보 구성

3. **Credentials 구조 개선**
   - Secret: `zot.credentials` (username:password 통합 형식)
   - ConfigMap: `env:ZOT_CREDENTIALS` 참조

4. **검증**
   - Pod Running 상태
   - Zot Registry 연결 확인

### 📂 배포된 파일 구조

```
module.platform/platform/image-manager/
├── 00-crd.yaml                      # CRD (sync-wave: -1)
├── 10-rbac.yaml                     # ClusterRole, ServiceAccount
├── 20-deployment.yaml               # Controller Deployment
├── 30-configmap.yaml                # Registry 설정
├── image-updater-secret.sops.yaml   # Zot Credentials (SOPS 암호화)
├── ksops-generator.yaml             # SOPS 통합
└── kustomization.yaml               # 리소스 통합
```

---

## 아키텍처 개요

### 전체 구조

```
┌─────────────────────────────────────────────────────────────────┐
│                    ArgoCD Image Updater v1.0.2                  │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌───────────────┐   ┌───────────────┐   ┌──────────────────┐  │
│  │  CRD          │   │  Controller   │   │  ImageUpdater CR │  │
│  │  (Cluster)    │   │  (argocd ns)  │   │  (사용자 정의)   │  │
│  └───────────────┘   └───────────────┘   └──────────────────┘  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
           │                      │                      │
           │                      │                      │
           ▼                      ▼                      ▼
┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐
│ Kubernetes API  │   │ Zot Registry    │   │ ArgoCD Apps     │
│ (CRD 저장)      │   │ (Image 폴링)    │   │ (Image 업데이트)│
└─────────────────┘   └─────────────────┘   └─────────────────┘
```

### 동작 흐름

```
1. ImageUpdater CR 생성
   └─> Controller가 CR 감지 (watch)

2. Controller 동작
   ├─> CR spec.applicationRefs에서 대상 Application 선택
   ├─> spec.images에 정의된 이미지 목록 확인
   └─> Registry API 호출 (5분 간격)

3. 새 태그 발견 시
   ├─> updateStrategy에 따라 태그 선택 (예: semver)
   ├─> ArgoCD Application 파라미터 업데이트
   └─> Application 자동 sync (syncPolicy.automated)

4. 배포
   └─> Kubernetes에 새 이미지 적용
```

---

## CRD 기반 vs Annotation 기반

### v1.x (CRD 기반) - 현재 구현

```yaml
# ImageUpdater CR 생성
apiVersion: argocd-image-updater.argoproj.io/v1alpha1
kind: ImageUpdater
metadata:
  name: profanity-filter
  namespace: argocd
spec:
  namespace: argocd
  applicationRefs:
    - namePattern: "profanity-*"
      images:
        - imageName: "docker-registry.bottle-note.com/profanity-api"
          updateStrategy: semver
          allowTags:
            - regex: ^v[0-9]+\.[0-9]+\.[0-9]+$
  writeBackConfig:
    method: argocd
```

**장점:**
- ✅ 독립적 리소스 관리
- ✅ Kubernetes 유효성 검증
- ✅ 여러 Application 패턴 매칭 가능
- ✅ 구조화된 YAML 설정

### v0.x (Annotation 기반)

```yaml
# Application에 annotation 추가
metadata:
  annotations:
    argocd-image-updater.argoproj.io/image-list: myapp=registry/image
    argocd-image-updater.argoproj.io/myapp.update-strategy: semver
```

**단점:**
- ❌ 문자열 기반 설정 (파싱 필요)
- ❌ Application마다 개별 설정
- ❌ 타입 검증 약함

---

## 구현된 리소스

### 1. CRD (00-crd.yaml)

```yaml
apiVersion: apiextensions.k8s.io/v1
kind: CustomResourceDefinition
metadata:
  name: imageupdaters.argocd-image-updater.argoproj.io
  annotations:
    argocd.argoproj.io/sync-wave: "-1"
    argocd.argoproj.io/sync-options: SkipDryRunOnMissingResource=true
spec:
  group: argocd-image-updater.argoproj.io
  names:
    kind: ImageUpdater
    plural: imageupdaters
  scope: Namespaced
  versions:
    - name: v1alpha1
      # ... (schema 생략)
```

**핵심 필드:**
- `spec.applicationRefs`: Application 선택 규칙
- `spec.images`: 관리할 이미지 목록
- `spec.commonUpdateSettings`: 전역 업데이트 전략
- `spec.writeBackConfig`: 업데이트 방식 (argocd/git)

### 2. RBAC (10-rbac.yaml)

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: argocd-image-updater
rules:
  # ArgoCD Application 관리
  - apiGroups: ["argoproj.io"]
    resources: ["applications"]
    verbs: ["get", "list", "watch", "patch", "update"]

  # Application 상태 확인
  - apiGroups: ["argoproj.io"]
    resources: ["applications/status"]
    verbs: ["get", "list", "watch"]

  # Secret/ConfigMap 읽기
  - apiGroups: [""]
    resources: ["secrets", "configmaps"]
    verbs: ["get", "list", "watch"]

  # Event 생성 (디버깅)
  - apiGroups: [""]
    resources: ["events"]
    verbs: ["create"]

  # Leader Election (필요 시)
  - apiGroups: ["coordination.k8s.io"]
    resources: ["leases"]
    verbs: ["get", "list", "create", "update", "patch"]
```

**주요 변경사항:**
- `Role` → `ClusterRole` (multi-namespace 지원)
- `applications/status` 권한 추가
- `leases` 권한 추가 (Leader Election용)

### 3. Deployment (20-deployment.yaml)

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: argocd-image-updater
  namespace: argocd
spec:
  replicas: 1
  template:
    spec:
      containers:
        - name: argocd-image-updater
          image: quay.io/argoprojlabs/argocd-image-updater:v1.0.2
          args:
            - run
            - --interval=5m
            - --health-probe-bind-address=:8080
            - --registries-conf-path=/app/config/registries.conf
            - --loglevel=info
            - --leader-election=false  # 단일 replica
          env:
            - name: ARGOCD_TOKEN
              valueFrom:
                secretKeyRef:
                  name: argocd-image-updater-secret
                  key: argocd.token
            - name: ZOT_CREDENTIALS
              valueFrom:
                secretKeyRef:
                  name: argocd-image-updater-secret
                  key: zot.credentials
```

**v1.0.2 변경사항:**
- `--health-port` → `--health-probe-bind-address`
- `--log-level` → `--loglevel`
- `--argocd-server-addr`, `--argocd-grpc-web` 제거 (ConfigMap 사용)
- 환경변수: `ZOT_USERNAME + ZOT_PASSWORD` → `ZOT_CREDENTIALS`

### 4. ConfigMap (30-configmap.yaml)

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: argocd-image-updater-config
  namespace: argocd
data:
  registries.conf: |
    registries:
      - name: Zot Self-Hosted
        prefix: docker-registry.bottle-note.com
        api_url: https://docker-registry.bottle-note.com
        ping: yes
        insecure: no
        default: yes
        credentials: env:ZOT_CREDENTIALS
```

**Credentials 형식:**
- v0.x: `env:USERNAME:PASSWORD` (분리)
- v1.x: `env:CREDENTIALS` (통합, `username:password` 형식)

### 5. Secret (image-updater-secret.sops.yaml)

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: argocd-image-updater-secret
  namespace: argocd
type: Opaque
stringData:
  argocd.token: "eyJ..."
  zot.credentials: "hgkim:hqu*yvw-nwn9NYA3bnz"  # username:password
```

**SOPS 암호화:**
- Age 키 기반 암호화
- Git에 안전하게 저장
- KSOPS로 ArgoCD 배포 시 복호화

---

## ArgoCD Sync Wave 전략

### Sync Wave 설정

```
Wave -1: CRD 설치
  └─> 00-crd.yaml

Wave 0 (기본): Controller 배포
  ├─> 10-rbac.yaml
  ├─> 20-deployment.yaml
  ├─> 30-configmap.yaml
  └─> image-updater-secret.sops.yaml

Wave 1: ImageUpdater CR 생성
  └─> 40-imageupdater-cr.yaml (미래)
```

### CRD Annotation

```yaml
metadata:
  annotations:
    argocd.argoproj.io/sync-wave: "-1"
    argocd.argoproj.io/sync-options: SkipDryRunOnMissingResource=true
```

**이유:**
1. CRD가 먼저 설치되어야 CR 생성 가능
2. `SkipDryRunOnMissingResource`: CRD 없을 때 dry-run 스킵
3. Wave 간 2초 대기 (기본값)

---

## ImageUpdater CR 생성 가이드

### 기본 예제

```yaml
apiVersion: argocd-image-updater.argoproj.io/v1alpha1
kind: ImageUpdater
metadata:
  name: profanity-filter
  namespace: argocd
  annotations:
    argocd.argoproj.io/sync-wave: "1"  # Controller 다음
spec:
  # Application 선택
  namespace: argocd
  applicationRefs:
    - namePattern: "profanity-filter"
      images:
        - imageName: "docker-registry.bottle-note.com/profanity-api"
          updateStrategy: semver
          allowTags:
            - regex: ^v[0-9]+\.[0-9]+\.[0-9]+$

  # 업데이트 방식
  writeBackConfig:
    method: argocd  # ArgoCD 파라미터 사용 (Git commit 없음)
```

### 여러 Application 관리

```yaml
spec:
  applicationRefs:
    - namePattern: "profanity-*"  # 패턴 매칭
      images:
        - imageName: "docker-registry.bottle-note.com/profanity-api"
          updateStrategy: semver

    - namePattern: "bottle-note-*"
      images:
        - imageName: "docker-registry.bottle-note.com/bottle-note-api"
          updateStrategy: latest
```

### Update Strategy

```yaml
# 1. Semver (추천)
updateStrategy: semver
allowTags:
  - regex: ^v[0-9]+\.[0-9]+\.[0-9]+$

# 2. Latest
updateStrategy: latest

# 3. Name (알파벳 순)
updateStrategy: name
```

### Git Write-Back

```yaml
writeBackConfig:
  method: git
  gitCommitUser: "argocd-image-updater"
  gitCommitEmail: "noreply@argoproj.io"
```

**주의:**
- Git write-back 사용 시 Repository 쓰기 권한 필요
- SSH 키 또는 Personal Access Token 구성 필요

---

## 트러블슈팅

### 1. CRD 설치 실패

**증상:**
```
The server could not find the requested resource (imageupdaters.argocd-image-updater.argoproj.io)
```

**해결:**
```bash
# CRD 수동 설치
kubectl apply -f https://raw.githubusercontent.com/argoproj-labs/argocd-image-updater/stable/config/install.yaml

# CRD 확인
kubectl get crd imageupdaters.argocd-image-updater.argoproj.io
```

### 2. Leader Election 에러

**증상:**
```
error retrieving resource lock argocd/c21b75f2.argoproj.io:
leases.coordination.k8s.io is forbidden
```

**해결:**
```yaml
# Deployment args에 추가
- --leader-election=false  # 단일 replica 환경
```

또는 RBAC에 leases 권한 추가.

### 3. Registry 연결 실패

**증상:**
```
Failed to get tags for docker-registry.bottle-note.com/profanity-api
```

**확인:**
```bash
# Secret 확인
kubectl get secret -n argocd argocd-image-updater-secret -o yaml

# Credentials 형식 확인 (username:password)
kubectl get secret -n argocd argocd-image-updater-secret \
  -o jsonpath='{.data.zot\.credentials}' | base64 -d

# Registry 직접 테스트
curl -u username:password \
  https://docker-registry.bottle-note.com/v2/_catalog
```

### 4. ImageUpdater CR 생성 안됨

**증상:**
```
Unable to create ImageUpdater: CRD not installed
```

**해결:**
```bash
# Sync Wave 확인
kubectl get crd imageupdaters.argocd-image-updater.argoproj.io \
  -o jsonpath='{.metadata.annotations}'

# Wave -1 확인
# argocd.argoproj.io/sync-wave: "-1"

# ArgoCD 재동기화
argocd app sync argocd/platform
```

### 5. Pod CrashLoopBackOff

**로그 확인:**
```bash
kubectl logs -n argocd deployment/argocd-image-updater --tail=100
```

**일반적인 원인:**
- 잘못된 CLI 플래그 (v0.x → v1.x 변경)
- ARGOCD_TOKEN 없음
- ConfigMap 마운트 실패

---

## 참고 자료

### 공식 문서
- [ArgoCD Image Updater Documentation](https://argocd-image-updater.readthedocs.io/)
- [v1.0.2 Release Notes](https://github.com/argoproj-labs/argocd-image-updater/releases/tag/v1.0.2)
- [Installation Guide](https://argocd-image-updater.readthedocs.io/en/stable/install/installation/)

### ArgoCD Sync Wave
- [Sync Waves Documentation](https://argo-cd.readthedocs.io/en/stable/user-guide/sync-waves/)
- [CRD Sync Discussion](https://github.com/argoproj/argo-cd/discussions/11883)
- [Sync Options](https://argo-cd.readthedocs.io/en/latest/user-guide/sync-options/)

### CRD 관리
- [Server-Side Apply for Large CRDs](https://medium.com/@paolocarta_it/argocd-server-side-apply-for-bulky-crds-373cd3c0ac2a)
- [CRD Best Practices](https://kubernetes.io/docs/tasks/extend-kubernetes/custom-resources/custom-resource-definitions/)

---

## 체크리스트

### 배포 완료
- [x] CRD 설치 (00-crd.yaml)
- [x] ClusterRole RBAC (10-rbac.yaml)
- [x] Deployment v1.0.2 (20-deployment.yaml)
- [x] ConfigMap Zot 설정 (30-configmap.yaml)
- [x] Secret 통합 형식 (zot.credentials)
- [x] SOPS 암호화
- [x] ArgoCD Sync 성공
- [x] Pod Running 확인
- [x] Sync Wave 적용

### 다음 단계
- [ ] ImageUpdater CR 생성 (40-imageupdater-cr.yaml)
- [ ] Application에 매칭 패턴 적용
- [ ] 이미지 업데이트 테스트
- [ ] Git write-back 설정 (선택)

---

**작성자:** Claude Sonnet 4.5
**최종 업데이트:** 2026-01-29