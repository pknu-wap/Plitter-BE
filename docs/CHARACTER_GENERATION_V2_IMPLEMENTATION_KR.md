# 캐릭터 생성 V2 구현 및 검증 결과

작성일: 2026-11-23

## 구현한 동작

- 음악 분위기 9종과 정보 부족 상태를 공통 분류기로 판정한다.
- 곡별로 한 번 집계하고, 누락·범위 밖 값과 실제 0을 구분한다.
- 장르 별칭을 정규화한 뒤 대표 장르를 결정한다. 동률은 장르 코드 순으로 결정한다.
- 버전이 있는 후보 사전에서 표정·포즈·소품·주변 표시를 가중치로 선택한다.
- 생성 요청별 seed를 사용하며, 최근 3개와 동일한 시각 조합·이름을 대안이 있으면 피한다.
- 그룹별 합계 60개 이름 후보와 상황별 태그 후보를 사용한다. 이름은 형용사구 + 스타, 태그는 서로 다른 3개다.
- 하나의 콘셉트에서 이미지 편집 지시와 이름·태그를 생성한다.
- 신규 캐릭터의 콘셉트와 최종 이름·태그를 JSONB로 저장한다.
- 상세·다운로드 조회 시 저장된 이름·태그를 반환하고, 기존 레코드는 기존 규칙으로 조회한다.
- V2 메타데이터가 손상되거나 누락된 경우 조용히 이름을 다시 고르지 않고 오류를 기록한다.
- 저장 재시도에서는 같은 이미지·콘셉트·메타데이터를 재사용한다.
- 반투명 검은 선을 불투명하게 만드는 후처리를 수정하고 회귀 테스트를 추가했다.

후보 사전은 `src/main/resources/character/character-concepts-v2.json`에 있다. 지원 상태는 분위기 그룹 10개(UNKNOWN 포함), 장르 그룹 15개다. 코드의 그룹·길이·후보 참조 검증을 통과해야 애플리케이션이 시작된다.

## 배포 전 스키마 적용

검증한 애플리케이션 DB에 `docs/sql/character_generation_v2.sql`을 적용해야 한다. 신규 컬럼은 다음 두 개이며 기존 행에는 NULL을 허용한다.

| 컬럼 | 내용 |
| --- | --- |
| generation_spec_json | 프로필, seed, 버전, 선택된 구성 요소·이름·태그 ID |
| result_metadata_json | 최종 characterName, tags |

이 작업에서는 운영 DB에 접속하거나 스키마 변경을 실행하지 않았다. 기존 설정의 `ddl-auto=update`만으로 운영 적용 여부를 판단하지 않는다.

신규 생성에 V2를 쓰는 기본 설정은 `character.generation.v2-enabled=true`다. false로 설정하면 신규 생성에 기존 규칙을 사용한다. 이 설정과 관계없이 이미 저장된 V2 이름·태그는 그대로 조회된다. 과거 버전으로 코드 전체를 되돌리는 것은 이 설정 전환과 다르다.

## 자동 검증

- 전체 main Java 소스를 Java 17 대상으로 컴파일했다.
- JUnit Platform 6.0.3으로 관련 테스트 39개를 실행했으며, 39개 성공·0개 실패·0개 건너뜀이다.
- H2의 PostgreSQL 호환 모드에서 실제 Spring Data JPA 저장·재조회를 검증했다.
- 샌드박스에서 Gradle 캐시 쓰기가 제한돼, 기존 애플리케이션 JAR의 의존성과 로컬 JUnit 의존성을 사용해 컴파일·실행했다.
- PR 준비 시 별도 체크아웃과 임시 Gradle 캐시로 표준 test·bootJar도 시도했으나, Gradle 데몬의 소켓 생성이 `Operation not permitted`로 차단됐다. 표준 Gradle 빌드 성공을 의미하지 않는다.

| 검증 영역 | 확인한 내용 |
| --- | --- |
| 분위기·장르 | 경계값, 실제 0, 누락·범위 밖 값, 표본 부족, 별칭 |
| 후보 선택 | 모든 지원 프로필·장르에 고정 seed 200개를 적용해 다양성 확인, 동일 입력 재현, 최근 3개 회피 |
| 표현 | 이름 끝·길이, 태그 개수·길이, 프롬프트와 콘셉트 연결 |
| 집계 | 중복 곡 추천 표본 수, 누락값, 장르 분포 합산·동률 |
| 저장·조회 | 두 JSONB 컬럼 저장, 생성·상세·다운로드 이름·태그 일치 |
| 호환·조건 | 기존 이름, V1 전환, 기존 V2 이름 유지, 소유자·추천 5건 조건 |
| 이미지 후처리 | 검은 선의 반투명 알파 보존, 회색 선 변환, 흰색 제거 |

일반 개발 환경의 실행 명령:

```bash
./gradlew test \
  --tests 'com.playlist.plitter.character.application.CharacterEditSpecGeneratorTest' \
  --tests 'com.playlist.plitter.character.application.CharacterV2PersistenceTest' \
  --tests 'com.playlist.plitter.character.infrastructure.s3.ImageStrokeNormalizationTest' \
  --tests 'com.playlist.plitter.track.application.feature.MetadataTagFeatureEstimatorTest'
```

## 시각 샘플 4종

`CharacterV2Preview`는 외부 DB·OpenAI·S3를 호출하지 않고, 실제 V2 생성 클래스에서 고정 테스트 입력의 콘셉트·이름·태그·편집 지시를 추출한다. 확인할 이름과 태그가 포함되는 seed를 탐색하는 큐레이션된 테스트 입력이며, 운영 요청의 결과 분포를 대표하는 표본은 아니다.

그 편집 지시와 `base_character.png`를 이미지 생성 도구에 전달해 투명 배경 샘플 4종을 만들었다. 운영 애플리케이션의 `OpenAiCharacterImageEditClient` 실호출이나 S3 업로드를 검증한 결과는 아니다. 생성 이미지에 앱의 S3 후처리를 적용한 결과도 아니다.

| 샘플 | 이름 | 입력 |
| --- | --- | --- |
| 1 | 괜히 센치한 스타 | 발라드, energy 0.28, valence 0.30 |
| 2 | 유난히 말랑한 스타 | 어쿠스틱, energy 0.25, valence 0.80 |
| 3 | 쓸데없이 비장한 스타 | 록, energy 0.83, valence 0.25 |
| 4 | 묘하게 잔망스러운 스타 | 댄스, energy 0.82, valence 0.80 |

생성된 원본을 보존한 채 `output/character-v2/assets/`에 샘플을 복사했다. `image-manifest.json`에 각 편집 지시의 SHA-256을 기록한다. 지시가 바뀌면 미리보기 렌더러가 이전 이미지와 섞어 표시하지 않고 중단한다.

아래 산출물은 이번 로컬 검증에서 만든 파일이며 PR과 운영 배포에는 포함하지 않는다.

- `output/character-v2/gallery.html`: 이미지가 포함된 독립 실행 HTML. 생성 지시 펼쳐 보기 지원.
- `output/character-v2/preview.png`: 동일 데이터·이미지를 문서 렌더러로 배치한 미리보기. 브라우저 스크린샷이 아니다.
- `output/character-v2/samples.json`: 실제 Java 클래스에서 추출한 입력·선택 결과·프롬프트·이름·태그.
- `output/character-v2/image-manifest.json`: 이미지 출처와 지시 해시.
- `output/character-v2/test-report.txt`: 이번 검증 결과와 범위.

Java 생성 규칙의 샘플 메타데이터 추출:

```bash
./gradlew characterV2Preview
```

이 명령은 `samples.json`에 입력·콘셉트·프롬프트·이름·태그를 기록한다. 이미지 생성이나 화면 렌더링, DB·S3 호출은 수행하지 않는다. 미리보기 제작용 Python 도구와 관련 패키지는 백엔드 실행에 필요하지 않다.

## 남은 실환경 검증

- PostgreSQL 운영 버전에서 스키마 적용과 저장·재조회.
- 애플리케이션에 설정된 이미지 모델로 실제 편집 요청 후 프롬프트 반영 확인.
- S3 업로드·다운로드와 후처리 이미지의 육안 확인.
- 재생성 반복률과 UNKNOWN 비율에 따른 후보 가중치·경계값 조정.

Last.fm 실패 시 기존 성공값 보존, HTTP 요청 멱등성, 생성 작업 비동기화, S3 보상 삭제는 별도 작업으로 남아 있다.
