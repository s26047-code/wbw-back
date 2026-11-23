# 설계 개요 (백엔드)

## 범위
프론트엔드는 별도 담당자가 개발합니다. 이 저장소의 `backend/` 는 REST API 서버이고, 연동 규약은 `API.md` 에 정리했습니다.

## 구조
```
[프론트엔드 SPA (TipTap 편집기)] ──HTTPS / 세션 쿠키 + CSRF──▶ [Spring Boot API] ──▶ [MySQL 8]
                                                              └─ OpenHTMLtoPDF (PDF)
```
- 인증: Google OIDC 로그인 → 서버 세션(`WBW_SESSION`). JWT를 쓰지 않아 토큰 저장·갱신 문제가 없습니다.
- CSRF: `XSRF-TOKEN` 쿠키 + `X-XSRF-TOKEN` 헤더.
- 스키마: Flyway가 관리합니다. Hibernate의 자동 생성(`ddl-auto`)은 끄고, DB 초기화(clean)는 차단했습니다.

## 데이터
| 테이블 | 주요 컬럼 |
|---|---|
| users | google_sub(UNIQUE), email, name, picture_url |
| user_settings | theme, background/text color, font_family, font_size, letter_spacing, line_height, paragraph_spacing, editor_width |
| folders | user_id, name (사용자별 UNIQUE), sort_order |
| documents | user_id, folder_id(NULL=폴더 없음), title, content(TipTap JSON), plain_text, preview, chars_with/without_spaces, version |
| shares | document_id, token(UNIQUE, 256비트 난수), password_hash(BCrypt, NULL=공개), enabled, expires_at |

## 주요 결정
| 주제 | 결정 | 이유 |
|---|---|---|
| 서식 저장 | TipTap JSON 원본 저장 | 저장 후에도 서식이 그대로 유지되고, HTML 저장으로 인한 XSS 위험이 없음 |
| 사용자 데이터 격리 | 모든 조회를 `id + user_id` 로 수행, 다른 사람 데이터는 404 | 권한 누락 방지, 존재 여부도 숨김 |
| 자동 저장 | `@Version` 낙관적 락 + 클라이언트 version 비교, 불일치 시 409 | 다른 탭·기기의 저장을 조용히 덮어써서 생기는 유실 방지 |
| 폴더 이동 | 별도 쿼리로 version을 건드리지 않음 | 편집 중인 글이 이동돼도 자동 저장이 충돌하지 않음 |
| 폴더 삭제 | 안의 글·공유 링크까지 함께 삭제, 삭제 개수 반환 | 요구사항 (프론트에서 안내 후 삭제) |
| 글자 수 | 서버가 본문에서 다시 계산 (자소 클러스터 단위, 줄바꿈 제외) | 클라이언트 값을 신뢰하지 않고 일관성 확보 |
| 공유 비밀번호 | BCrypt + 세션 인증 + (링크, IP)당 5회 실패 시 10분 잠금 | 무차별 대입 방지 |
| PDF | 서버에서 JSON → 검증된 XHTML → PDF, 한글 TTF 내장 | 실제 파일 다운로드, 허용된 서식만 출력 |

## 확장 시 고려
- 서버를 여러 대로 늘리면: Spring Session(JDBC/Redis), 시도 제한 저장소를 공용으로 교체
- 휴지통이 필요하면: documents에 `deleted_at` 추가 (Flyway V2)
- Spring Boot 4.x 로 올릴 때: 스타터 이름 변경(예: `spring-boot-starter-webmvc`)과 테스트 어노테이션 패키지 이동 확인
