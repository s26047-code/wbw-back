# 작문 플랫폼 백엔드 API 명세 (프론트엔드 연동용)

- 기본 주소: `http://localhost:8080` (개발)
- 모든 요청/응답은 JSON (`Content-Type: application/json`), 시간은 ISO-8601 UTC 문자열 (`2026-11-23T11:00:00.123456Z`)
- 인증: 구글 로그인 후 **세션 쿠키**(`WBW_SESSION`). fetch 시 `credentials: 'include'` 필수

> 💡 **Swagger UI**: 서버 실행 후 `/swagger-ui.html` 에서 모든 API를 보고 직접 호출할 수 있습니다. 로그인은 같은 브라우저에서 `/swagger-login` 접속.

## 0. 연동 전에 꼭 알아야 할 것

### 로그인 / 로그아웃
| 동작 | 방법 |
|---|---|
| 로그인 | 브라우저를 `GET /oauth2/authorization/google` 로 **이동**(fetch 아님). 성공 시 `FRONTEND_URL/` 로, 실패 시 `FRONTEND_URL/login?error=oauth` 로 돌아옴 |
| 로그인 여부 확인 | `GET /api/me` → 200(로그인됨) / 401(로그인 필요) |
| 로그아웃 | `POST /api/logout` (CSRF 헤더 필요) → 204 |

### CSRF (POST·PUT·PATCH·DELETE 필수)
1. 앱 시작 시 `GET /api/csrf` 호출 → `XSRF-TOKEN` 쿠키 발급 + 본문 `{ "headerName": "X-XSRF-TOKEN", "token": "..." }`
2. 변경 요청마다 헤더 `X-XSRF-TOKEN: <값>` 전송
   - 프론트와 API가 같은 도메인(또는 Vite 프록시)이면: `XSRF-TOKEN` 쿠키 값을 그대로 사용 (권장)
   - 쿠키를 읽을 수 없는 구성이면: `/api/csrf` 응답의 `token` 사용
3. 누락/불일치 시 403 `FORBIDDEN`

### 개발 환경 프록시 (Vite 권장 설정)
쿠키·CSRF 문제를 없애기 위해 프론트 개발 서버가 백엔드를 프록시하는 방식을 권장합니다.
```js
// vite.config.ts
server: {
  proxy: {
    '/api': 'http://localhost:8080',
    '/oauth2': 'http://localhost:8080',
    '/login/oauth2': 'http://localhost:8080',
  }
}
```
이 경우 구글 콘솔의 승인된 리디렉션 URI에 `http://localhost:5173/login/oauth2/code/google` 도 등록해야 합니다.

**학교 서버(개발 서버)에 붙을 때**는 target 만 바꾸면 됩니다. (학교 내부망/VPN 연결 필요)
```js
const API = 'http://10.0.0.203:8080'
server: { proxy: { '/api': API, '/oauth2': API, '/login/oauth2': API, '/swagger-ui': API, '/v3/api-docs': API } }
```
⚠️ `changeOrigin` 은 **켜지 마세요**(기본값 false 유지). Host 헤더가 `localhost:5173` 으로 유지되어야 구글 로그인이 동작합니다.
(구글은 `localhost` 가 아닌 IP 주소로의 로그인 리디렉션을 허용하지 않습니다)

### 에러 응답 형식 (공통)
```json
{
  "code": "DOCUMENT_VERSION_CONFLICT",
  "message": "다른 곳에서 먼저 수정된 글입니다. 최신 내용을 확인해 주세요.",
  "fieldErrors": [{ "field": "fontSize", "message": "글자 크기는 40px 이하여야 합니다." }],
  "details": { "currentVersion": 3, "updatedAt": "..." }
}
```
`fieldErrors`, `details` 는 있을 때만 포함됩니다. `message` 는 사용자에게 그대로 보여줘도 되는 한국어 문구입니다.

| HTTP | code | 상황 |
|---|---|---|
| 400 | INVALID_REQUEST | 유효성 검사 실패 (`fieldErrors` 참고) |
| 400 | DOCUMENT_CONTENT_INVALID | 본문이 TipTap doc JSON이 아님 |
| 401 | UNAUTHORIZED | 로그인 필요 |
| 403 | FORBIDDEN | CSRF 토큰 누락 등 |
| 404 | FOLDER_NOT_FOUND / DOCUMENT_NOT_FOUND / SHARE_NOT_FOUND | 없거나 **다른 사용자 소유** (존재 여부를 숨김) |
| 409 | FOLDER_NAME_DUPLICATED | 같은 이름의 폴더 존재 |
| 409 | DOCUMENT_VERSION_CONFLICT | 자동 저장 버전 충돌 |
| 410 | SHARE_EXPIRED | 만료된 공유 링크 |
| 401 | SHARE_PASSWORD_REQUIRED / SHARE_PASSWORD_MISMATCH | 공유 비밀번호 필요 / 불일치 |
| 413 | DOCUMENT_CONTENT_TOO_LARGE | 본문 JSON 5MB 초과 |
| 429 | SHARE_TOO_MANY_ATTEMPTS | 비밀번호 5회 실패 → 10분 잠금 |

---

## 1. 사용자

### `GET /api/me`
```json
{ "id": 1, "email": "a@gmail.com", "name": "홍길동", "pictureUrl": "https://..." }
```

## 2. 개인 설정

### `GET /api/settings`
처음 호출 시 기본값으로 생성됩니다.
```json
{
  "theme": "LIGHT",            // LIGHT | DARK | CUSTOM
  "backgroundColor": null,     // CUSTOM일 때 사용 (#RRGGBB)
  "textColor": null,
  "fontFamily": "Noto Serif KR",
  "fontSize": 17,              // px, 10~40
  "letterSpacing": 0.0,        // em, -0.1~0.5
  "lineHeight": 1.8,           // 배수, 1.0~3.0
  "paragraphSpacing": 0.8,     // em, 0~3 (문단 아래 간격)
  "editorWidth": 720,          // px, 480~1200
  "updatedAt": "..."
}
```

### `PUT /api/settings`
위 객체에서 `updatedAt` 을 뺀 전체 값을 보냅니다. `theme=CUSTOM` 이면 두 색상이 모두 필요합니다. `fontFamily` 는 `/api/fonts` 목록의 `family` 중 하나여야 합니다.

### `GET /api/fonts` (로그인 불필요)
```json
[{ "family": "Noto Serif KR", "label": "본명조 (Noto Serif KR)", "generic": "serif", "pdfAvailable": true }, ...]
```
`family` 값을 CSS `font-family` 와 TipTap `textStyle.fontFamily` 에 그대로 사용하세요. 웹폰트(woff2)는 프론트에서 로드합니다.
제공 글꼴: Pretendard, Noto Serif KR, Noto Sans KR, Nanum Myeongjo, Nanum Gothic, Gowun Batang, Gowun Dodum
(학교 서버 PDF 사용 가능: Pretendard, 나눔명조, 나눔고딕. 나머지는 PDF에서 같은 계열 글꼴로 대체)

## 3. 폴더

| 메서드 | 경로 | 본문 | 응답 |
|---|---|---|---|
| GET | `/api/folders` | | `Folder[]` (sortOrder, id 순) |
| GET | `/api/folders/{id}` | | `Folder` |
| POST | `/api/folders` | `{ "name": "소설" }` (1~50자) | 201 `Folder` |
| PATCH | `/api/folders/{id}` | `{ "name"?: "...", "sortOrder"?: 0 }` | `Folder` |
| DELETE | `/api/folders/{id}` | | `{ "id": 3, "deletedDocumentCount": 5 }` |

```json
// Folder
{ "id": 3, "name": "소설", "sortOrder": 0, "documentCount": 5, "createdAt": "...", "updatedAt": "..." }
```

> ⚠️ **폴더 삭제 시 안의 글과 그 글의 공유 링크가 모두 영구 삭제됩니다.**
> 삭제 전에 `documentCount` 로 안내 문구를 보여주세요.
> 예: "‘소설’ 폴더와 안에 있는 글 5개가 모두 삭제되며 되돌릴 수 없습니다. 삭제할까요?"

## 4. 글

### `GET /api/documents` — 목록
| 쿼리 | 기본값 | 설명 |
|---|---|---|
| folderId | | 해당 폴더의 글만 |
| unfiled | false | true면 폴더 없는 글만 (folderId와 함께 사용 불가) |
| q | | 제목 검색 (부분 일치, 대소문자 무시) |
| sort | updatedAt | updatedAt / createdAt / title |
| direction | title이면 asc, 나머지 desc | asc / desc |
| page, size | 0, 20 | size 최대 100 |

```json
{
  "items": [{
    "id": 10, "title": "가을 이야기", "folderId": 3, "folderName": "소설",
    "preview": "본문 앞부분 최대 150자...", "charsWithSpaces": 1520, "charsWithoutSpaces": 1180,
    "createdAt": "...", "updatedAt": "..."
  }],
  "page": 0, "size": 20, "totalElements": 1, "totalPages": 1
}
```

### `POST /api/documents` — 새 글 (201)
```json
{ "title": "제목(선택)", "folderId": 3, "content": { "type": "doc", "content": [...] } }
```
모든 필드가 선택입니다. `{}` 를 보내면 빈 글이 만들어집니다. 응답은 `Document`.

### `GET /api/documents/{id}` — `Document`
```json
{
  "id": 10, "title": "가을 이야기", "folderId": 3, "folderName": "소설",
  "content": { "type": "doc", "content": [ ... TipTap JSON 그대로 ... ] },
  "charsWithSpaces": 1520, "charsWithoutSpaces": 1180,
  "version": 7, "createdAt": "...", "updatedAt": "..."
}
```

### `PUT /api/documents/{id}` — 저장 / 자동 저장
```json
{ "title": "새 제목", "content": { "type": "doc", ... }, "version": 7 }
```
- `version` 필수: 마지막으로 받은 버전. `title`, `content` 중 보낸 것만 변경 (최소 하나 필요)
- 응답 (본문 제외, 가벼움):
```json
{ "id": 10, "title": "새 제목", "charsWithSpaces": 1530, "charsWithoutSpaces": 1188, "version": 8, "updatedAt": "..." }
```
- **응답의 `version` 을 다음 저장에 사용**하세요. 내용이 같으면 버전은 바뀌지 않습니다.
- 버전이 다르면 **409 `DOCUMENT_VERSION_CONFLICT`** (`details.currentVersion`, `details.updatedAt`). 다른 탭/기기에서 먼저 저장된 경우이며, 서버는 덮어쓰지 않습니다. 사용자에게 "최신본 불러오기 / 내 내용으로 덮어쓰기(최신 version으로 재전송)"를 선택하게 하세요.
- 글자 수는 서버가 본문에서 다시 계산한 값이 정답입니다.

### `PATCH /api/documents/{id}/folder` — 폴더 이동
`{ "folderId": 3 }` 또는 `{ "folderId": null }`(폴더 없음). 응답 `Document`.
이동은 `version` 과 `updatedAt` 을 바꾸지 않으므로 편집 중인 글의 자동 저장과 충돌하지 않습니다.

### `DELETE /api/documents/{id}` — 204
글과 그 글의 공유 링크가 함께 삭제됩니다.

### `POST /api/documents/{id}/pdf/html` — PDF용 HTML 보기 (레이아웃 확인용)
PDF 내보내기와 같은 본문을 보내면, PDF를 만들 때 쓰는 HTML/CSS를 그대로 돌려줍니다. 화면과 PDF의 줄 간격·글꼴이 다를 때 실제 적용된 스타일 확인용입니다.

### `POST /api/documents/{id}/pdf` — PDF 다운로드
```json
{
  "pageSize": "A4",          // A4 | A5 | B5 | LETTER (기본 A4)
  "orientation": "PORTRAIT", // PORTRAIT | LANDSCAPE
  "marginTop": 25, "marginRight": 20, "marginBottom": 25, "marginLeft": 20,  // mm, 0~60
  "includeTitle": true
}
```
모두 선택 (`{}` 가능). 응답은 `application/pdf` 바이너리이고, `Content-Disposition` 에 파일명이 들어 있습니다.
요청 헤더 `Accept` 는 생략하거나 `*/*` 로 두세요 (오류 시 JSON 응답을 받기 위해).
```js
const res = await fetch(`/api/documents/${id}/pdf`, { method: 'POST', credentials: 'include',
  headers: { 'Content-Type': 'application/json', 'X-XSRF-TOKEN': token }, body: JSON.stringify(opts) });
const blob = await res.blob(); // a 태그 download 로 저장
```

## 5. 공유

### 작성자용 (로그인 필요)
| 메서드 | 경로 | 본문 | 응답 |
|---|---|---|---|
| GET | `/api/documents/{id}/shares` | | `Share[]` |
| POST | `/api/documents/{id}/shares` | `{ "password"?: "4~64자", "expiresAt"?: "ISO 시각" }` | 201 `Share` |
| PATCH | `/api/shares/{shareId}` | 아래 참고 | `Share` |
| DELETE | `/api/shares/{shareId}` | | 204 |

PATCH 본문 (보낸 값만 변경): `enabled`(bool), `password`(설정·변경), `removePassword: true`(공개로 전환), `expiresAt`, `clearExpiresAt: true`(만료 없음)

```json
// Share
{
  "id": 5, "documentId": 10, "token": "x8Gk...(43자)",
  "url": "http://localhost:5173/s/x8Gk...",
  "passwordProtected": true, "enabled": true, "expiresAt": null,
  "createdAt": "...", "updatedAt": "..."
}
```
`url` 은 `FRONTEND_URL + /s/{token}` 입니다. 프론트엔드에 `/s/:token` 열람 페이지를 만들어 주세요.

### 열람용 (로그인 불필요)
| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | `/api/public/shares/{token}` | 글 조회. 비밀번호 공유이고 아직 인증 전이면 401 `SHARE_PASSWORD_REQUIRED` |
| POST | `/api/public/shares/{token}/verify` | `{ "password": "..." }` → 성공 시 글 내용 반환 (CSRF 헤더 필요) |

```json
{ "title": "가을 이야기", "content": { "type": "doc", ... }, "charsWithSpaces": 1520, "charsWithoutSpaces": 1180, "updatedAt": "..." }
```
- 인증에 성공하면 같은 브라우저 세션에서는 다시 묻지 않습니다. 작성자가 비밀번호를 바꾸면 다시 인증해야 합니다.
- 비활성화했거나 없는 링크는 404, 만료된 링크는 410입니다.
- 공유 글은 현재 최신 내용을 보여줍니다 (스냅샷이 아님).

---

## 6. 편집기(TipTap) 서식 규약

서버는 본문 JSON을 **그대로 저장하고 그대로 돌려주므로** 어떤 확장을 써도 저장·복원됩니다.
다만 **PDF 변환과 글자 수 계산**이 서식을 이해하려면 아래 이름을 지켜 주세요.

| 서식 | TipTap 표현 | 값 형식 |
|---|---|---|
| 굵게 / 기울임 / 밑줄 / 취소선 | mark `bold` / `italic` / `underline` / `strike` | |
| 글꼴 | mark `textStyle`, attr `fontFamily` | `/api/fonts` 의 family (예: `"Nanum Myeongjo"`) |
| 글자 크기 | mark `textStyle`, attr `fontSize` | `"18px"` (또는 pt, 숫자는 px로 간주) |
| 자간 | mark `textStyle`, attr `letterSpacing` | `"0.05em"` (또는 px) |
| 글자색 | mark `textStyle`, attr `color` | `"#333333"` |
| 줄 간격 | node `paragraph`/`heading`, attr `lineHeight` | 숫자 배수 `1.6` (권장). `"160%"`, `"28px"` 도 가능 |
| 문단 간격 | node `paragraph`/`heading`, attr `paragraphSpacing` | 숫자 em `0.8` (권장, 문단 **아래** 여백). `"12px"` 도 가능, 단위 없는 숫자가 5보다 크면 px로 해석 |
| 정렬 | node attr `textAlign` | left / center / right / justify |
| 쪽 나눔 (PDF) | node `paragraph`, attr `pageBreak` | `true` 면 PDF에서 이 문단 다음부터 새 쪽. 내용 없는 문단이면 빈 줄 없이 쪽만 넘김 |

지원 노드: paragraph, heading(1~3), blockquote, bulletList, orderedList, listItem, codeBlock, horizontalRule, hardBreak.
문단 속성이 없으면 사용자 설정(`/api/settings`)의 값이 기본으로 적용됩니다.

### 글자 수 규칙 (화면 표시와 서버 값을 일치시키기)
- 화면에 보이는 글자 1개 = 1자 (이모지·결합 문자 포함)
- 줄바꿈(문단 구분, 강제 줄바꿈)은 세지 않음
- 공백 포함: 띄어쓰기·탭 등 공백 포함 / 공백 제외: 모든 공백 문자 제외
```js
const seg = new Intl.Segmenter('ko', { granularity: 'grapheme' });
function countChars(text /* editor.getText({ blockSeparator: '\n' }) */) {
  let withSpaces = 0, withoutSpaces = 0;
  for (const { segment } of seg.segment(text)) {
    if (/^(\r\n|\n|\r| | )$/.test(segment)) continue;
    withSpaces++;
    if (!/^\s+$/u.test(segment)) withoutSpaces++;
  }
  return { withSpaces, withoutSpaces };
}
```

### 자동 저장 권장 흐름
1. 입력이 멈추고 1~1.5초 뒤 저장, 계속 입력 중이면 최대 10초마다 저장 (한글 조합 중 `compositionend` 전에는 저장하지 않음)
2. 저장 전 `localStorage` 에 `{id, version, content, savedAt}` 백업 → 성공하면 삭제
3. 저장은 한 번에 하나만 보내고, 응답의 `version` 으로 다음 요청을 보냄
4. 네트워크 오류 시 재시도. 409면 충돌 안내
5. 페이지를 떠날 때는 `fetch(..., { keepalive: true })` 로 마지막 저장
