# SPAO 콜라보 수집

공식 페이지: https://www.spao.com/h/collaboration

해당 페이지가 사용하는 공개 배너 API `https://www.spao.com/v1/corner/banners/primary?cornerNo=R13701004_mainCollabo`를 수집합니다. 현재 노출된 콜라보 기획전 범위이며 전체 상품이나 과거 모든 콜라보를 수집하는 것은 아닙니다.

- `spao-{exhibitionNo}`를 ID로 사용하여 배너 교체나 재수집 시 중복을 방지합니다.
- 제목은 공식 표시 제목 우선, 비어 있으면 배너 이름의 관리용 접두/접미사를 제거합니다.
- 설명의 HTML 엔티티를 디코딩하고 태그를 제거합니다.
- 공식 기획전 링크와 SPAO 상품 CDN 이미지만 저장합니다.
- 이미지 파일 날짜나 기획전 ID는 발매일로 사용하지 않습니다. `COLLAB`, `UNKNOWN`, 발매일 null로 저장합니다.
- 수집 실패나 빈 응답에서는 기존 데이터를 유지합니다. 배너에서 내려간 과거 항목도 삭제하지 않습니다.
- SPAO 브랜드는 최초 수집 시 생성되어 기존 팔로우 API로 팔로우할 수 있습니다.

서버 시작 7초 후 최초 수집, 이후 1시간 간격입니다. `SPAO_CRAWLER_ENABLED=false`로 비활성화합니다. 새 자동 수집 코드를 적용하려면 서버 재시작이 필요합니다.

검증: `./gradlew test --tests '*SpaoParserTests'`. `SPAO_CAPTURE=/path/to/capture.json`을 설정하면 공식 캡처 응답도 검증합니다.

`SPAO_IMPORT=true SPAO_CAPTURE=/path/to/capture.json ./gradlew test --tests '*SpaoImportTests'`는 명시적으로 설정한 경우에만 해당 DB에 캡처 데이터를 실제 저장하는 수동 가져오기 명령입니다. 일반 테스트에서는 실행되지 않습니다.
