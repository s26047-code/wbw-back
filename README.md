# 작문 플랫폼 백엔드

Java 21 · Spring Boot 3.5 · Spring Data JPA · Spring Security(OAuth2 Google) · MySQL 8 · Flyway

프론트엔드 연동 명세는 [`docs/API.md`](docs/API.md), 설계 개요는 [`docs/DESIGN.md`](docs/DESIGN.md) 를 보세요.

## 1. 처음 실행하기 (Windows)

### ① MySQL 데이터베이스와 계정 만들기
MySQL Workbench 또는 `mysql -u root -p` 에서 한 번만 실행합니다. 비밀번호는 직접 정하세요.
```sql
CREATE DATABASE wbw CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'wbw'@'localhost' IDENTIFIED BY '여기에_비밀번호';
GRANT ALL PRIVILEGES ON wbw.* TO 'wbw'@'localhost';
```
테이블은 서버가 처음 시작할 때 Flyway가 자동으로 만듭니다(`src/main/resources/db/migration`).
기존 데이터를 지우거나 초기화하는 작업은 하지 않습니다(`flyway.clean-disabled=true`).

### ② 구글 로그인 키 발급
1. https://console.cloud.google.com → 프로젝트 생성
2. **API 및 서비스 → OAuth 동의 화면**: 외부(External), 앱 이름·이메일 입력, 범위 `openid`, `email`, `profile`
3. **사용자 인증 정보 → 사용자 인증 정보 만들기 → OAuth 클라이언트 ID** → 유형 "웹 애플리케이션"
4. **승인된 리디렉션 URI**에 추가:
   - `http://localhost:8080/login/oauth2/code/google`
   - `http://localhost:5173/login/oauth2/code/google` (프론트 Vite 프록시 사용 시)
   - 배포 후: `https://내도메인/login/oauth2/code/google`
5. 발급된 클라이언트 ID와 보안 비밀을 다음 단계의 `.env` 에 넣습니다.

> 동의 화면이 "테스트" 상태이면 등록한 테스트 사용자만 로그인할 수 있습니다. 실제 공개 전 "프로덕션으로 게시"가 필요합니다.

### ③ 환경 변수 파일 만들기
`backend` 폴더에서 `.env.example` 을 복사해 `.env` 로 저장하고 값을 채웁니다.
`.env` 는 `.gitignore` 에 포함되어 있어 Git에 올라가지 않습니다. 비밀번호와 키를 코드에 직접 쓰지 마세요.

### ④ (선택) PDF용 한글 글꼴 넣기
[`fonts/README.md`](fonts/README.md) 의 TTF 파일을 `backend/fonts/` 에 넣습니다. 없으면 PDF에서 한글이 보이지 않습니다.

### ⑤ 테스트 & 실행
```bat
cd backend
gradlew.bat test
gradlew.bat bootRun
```
- 첫 실행 때 Gradle과 라이브러리를 내려받느라 몇 분 걸릴 수 있습니다.
- 테스트는 메모리 DB(H2)를 써서 MySQL·구글 키 없이 돌아갑니다. 결과 보고서는 `build/reports/tests/test/index.html` 에 생깁니다.
- 실행 후 확인: http://localhost:8080/actuator/health → `{"status":"UP"}`
- 로그인 확인: http://localhost:8080/oauth2/authorization/google 로 들어가 로그인 → `FRONTEND_URL` 로 돌아옵니다.

### ⑥ Swagger (API 문서·테스트)
- http://localhost:8080/swagger-ui.html
- 같은 브라우저에서 http://localhost:8080/swagger-login 으로 구글 로그인하면 Swagger로 돌아오고, 이후 모든 API를 **Try it out** 으로 호출할 수 있습니다.
- 운영에서 숨기려면 `.env` 에 `SWAGGER_ENABLED=false`

## 2. 배포

### 방법 A: 서버에 Docker로 배포 (권장)
Docker가 설치된 리눅스 서버(예: AWS Lightsail, 오라클 클라우드, 개인 서버)에서:
```bash
# 프로젝트 업로드 후
cd backend
cp .env.example .env   # 값 채우기 (아래 운영 설정 참고)
docker compose up -d --build
docker compose logs -f app
```
- MySQL과 백엔드가 함께 실행되고, DB 데이터는 `db-data` 볼륨에 보관됩니다.
- 앱은 서버 내부(127.0.0.1:8080)에서만 열리므로 앞단에 HTTPS 리버스 프록시를 둡니다. Caddy 예시:
  ```
  내도메인.com {
      handle /api/* { reverse_proxy 127.0.0.1:8080 }
      handle /oauth2/* { reverse_proxy 127.0.0.1:8080 }
      handle /login/oauth2/* { reverse_proxy 127.0.0.1:8080 }
      handle { root * /srv/frontend; try_files {path} /index.html; file_server }
  }
  ```
  프론트엔드 빌드 결과물과 API를 **같은 도메인**에서 서비스하면 쿠키·CSRF 설정이 가장 단순합니다.

### 방법 B: JAR 파일로 실행
```bat
gradlew.bat bootJar
java -jar build\libs\wbw-backend-0.1.0.jar
```
`.env` 는 실행 위치(작업 디렉터리)에서 읽습니다.

### 운영 환경 `.env` 체크리스트
| 항목 | 값 |
|---|---|
| `FRONTEND_URL`, `CORS_ALLOWED_ORIGINS` | `https://내도메인.com` |
| `SESSION_COOKIE_SECURE` | `true` |
| `SESSION_COOKIE_SAME_SITE` | 같은 도메인이면 `lax`, 프론트와 API 도메인이 다르면 `none` |
| `DB_PASSWORD`, `MYSQL_ROOT_PASSWORD` | 길고 무작위인 값 |
| 구글 콘솔 | 운영 리디렉션 URI 추가 |

## 3. 구조
```
src/main/java/com/wbw/
├─ auth/       구글 OIDC 사용자 처리, @LoginUser, SPA용 CSRF
├─ user/       사용자, /api/me
├─ settings/   개인 글쓰기 환경
├─ folder/     폴더 (삭제 시 안의 글까지 함께 삭제)
├─ document/   글 CRUD, 자동 저장 버전 충돌 처리, 글자 수 계산, TipTap JSON 처리
├─ share/      공유 링크, 비밀번호 인증, 시도 횟수 제한
├─ pdf/        TipTap JSON → XHTML → PDF, 글꼴 목록
├─ config/     보안·CORS·설정값
└─ common/     공통 예외/에러 응답
```

## 4. 알아둘 제약
- **세션은 서버 메모리에 저장**됩니다. 서버를 재시작하면 다시 로그인해야 하고, 서버 여러 대로 늘리려면 Spring Session(JDBC/Redis)을 추가해야 합니다.
- 공유 비밀번호 시도 제한도 메모리 기반이며 서버 1대를 전제로 합니다.
- 글 삭제와 폴더 삭제는 영구 삭제입니다(휴지통 없음).

## 5. CI/CD (GitHub Actions)
| 워크플로 | 언제 | 하는 일 |
|---|---|---|
| `CI` | `develop`·`main` 대상 PR, `main` 푸시 | 테스트 → Discord 알림 |
| `CD (개발 서버)` | `develop` 푸시(머지) | 테스트 → 학교 서버 배포 → Discord 알림 |

필요한 GitHub Secrets (Settings → Secrets and variables → Actions)
| 이름 | 필수 | 설명 |
|---|---|---|
| `DEPLOY_SSH_KEY` | ✅ | 서버 배포용 SSH 개인키 (ed25519) |
| `DISCORD_BACK_WEBHOOK_URL` | ✅ | 백엔드 알림 채널(back) 웹훅 |
| `DISCORD_DEV_WEBHOOK_URL` | 선택 | 배포 알림만 받을 채널(dev) 웹훅. 없으면 back 채널로 보냄 |

수동 배포가 필요하면 Actions 탭에서 `CD (개발 서버)` → **Run workflow**, 또는 PC에서 `deploy\deploy.ps1`.
