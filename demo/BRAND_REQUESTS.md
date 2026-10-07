# 브랜드 요청

- 사용자 페이지: `/#brand-request` — 로그인 후 브랜드명, 공식 URL, 요청 사유 제출 및 본인 요청 조회.
- 관리자 페이지: `/#admin-requests` — 전체 요청을 최신순으로 20건씩 조회.
- 저장 테이블: `brand_requests`. 기존 `ddl-auto=update` 설정으로 서버 시작 시 생성됩니다.
- Spring 실행 환경에 `ADMIN_USERNAMES=관리자아이디`를 설정하세요. 여러 계정은 쉼표로 구분합니다. 해당 계정은 users에 등록된 계정이어야 합니다. 기본값은 비어 있으며 어떤 계정에도 자동으로 관리자 권한을 부여하지 않습니다.
- 서버 재시작 후 다시 로그인하면 권한이 적용됩니다. 로그인 세션은 서버에서 관리하며 요청 작성과 로그아웃에 CSRF 토큰을 사용합니다.
- 관리자는 요청을 승인·거절할 수 있습니다. 이는 검토 상태 변경이며 브랜드 자동 등록은 수행하지 않습니다.

상태는 `PENDING`(검토 대기), `APPROVED`(승인), `REJECTED`(거절)입니다. 신규/기존 요청의 기본값은 `PENDING`이며 사용자와 관리자 목록 응답에 `status`가 포함됩니다.
관리자 변경 API: `PATCH /api/brand/admin/requests/{id}/status`, 본문 `{"status":"APPROVED"}` 또는 `{"status":"REJECTED"}`. 관리자 세션과 CSRF 토큰이 필요합니다. 잘못된 입력은 400, 없는 요청은 404이며 처리한 상태를 다시 변경할 수도 있습니다.
기존 DB의 상태 컬럼 추가 SQL은 `migrations/20260910_request_status.sql`입니다.

API: `POST /api/brand/requests`, `GET /api/brand/requests?page=0`, `GET /api/brand/admin/requests?page=0`.

## 사용자 관계와 팔로우

`brand_requests.user_id → users.id`는 필수 외래키입니다. 기존 `requested_by`는 요청 당시 이름 기록으로 보존하지만 소유자 조회는 사용자 ID로 수행합니다. API에는 DTO만 반환하여 사용자 엔티티와 비밀번호 해시가 직렬화되지 않도록 합니다.

`brand_follows`는 `user_id → users.id`, `brand_id → brands.id`와 `(user_id, brand_id)` 유니크 제약을 가집니다. 팔로우 API는 세션의 사용자만 대상으로 하며 사용자 ID를 클라이언트에서 받지 않습니다.

- `GET /api/me/follows`: 현재 사용자의 브랜드 ID 목록
- `PUT /api/me/follows/{brandId}`: 중복 없이 팔로우
- `DELETE /api/me/follows/{brandId}`: 본인의 팔로우 해제

My Drop은 서버에서 가져온 사용자별 팔로우 목록으로 브랜드의 릴리즈를 필터링합니다. 브라우저의 `drop-demo`에 남아 있는 예전 목록은 계정 소유가 확정되지 않으므로 자동 이관하지 않습니다. DB에 등록되지 않은 데모 브랜드는 팔로우할 수 없으며 브랜드 요청으로 안내합니다.

기존 DB는 새 서버를 실행하기 전에 다음 SQL을 적용합니다. 이 작업 공간의 로컬 dropout DB에는 적용 완료했습니다.

```sh
psql -X -v ON_ERROR_STOP=1 -h localhost -d dropout -f migrations/20260909_user_relations.sql
```

이관은 트랜잭션 안에서 기존 요청의 `requested_by`와 `users.username`을 매칭합니다. 매칭되지 않는 요청이 있으면 전체 롤백하며 데이터를 삭제하지 않습니다. 신규 빈 DB는 Hibernate update로 테이블을 생성할 수 있습니다.
