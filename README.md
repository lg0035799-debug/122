# MurimSkills (무협 RPG 스킬 시스템) — Paper 1.21.4

## 필요한 것
- Paper 1.21.4 서버, Java 21
- MythicMobs 5.x (스킬 실행용)
- 리소스팩 (`resourcepack/` 폴더 → zip 으로 묶어 서버 `resource-pack` 설정에 등록)

## 빌드
```
gradle build        # 또는 IDE(IntelliJ)에서 프로젝트 열고 Gradle 'build'
```
`build/libs/MurimSkills-0.1.0.jar` 를 `plugins/` 에 넣으세요.
(Mythic-Dist 버전이 안 받아지면 build.gradle.kts 의 버전을 MythicMobs 저장소의 최신 5.x 로 바꾸세요.)

## 설치 순서
1. jar 를 plugins/ 에 넣고 서버 1회 실행 → `plugins/MurimSkills/` 생성
2. `mythic/murim_skills.yml` 을 `plugins/MythicMobs/Skills/` 에 복사 (예시 스킬) 후 `/mm reload`
3. `resourcepack/` 안의 내용(pack.mcmeta, assets)이 zip 최상위에 오도록 압축해서 적용
4. 게임 안: `/murim set <이름> internal 50`, `/murim set <이름> prof sword 30` 로 수치 부여
5. `/murim` → 비급 등록 → 스킬 습득 → 슬롯 배치 → F키로 스킬 모드 진입 → 1~8번 키로 사용

## 명령어
| 명령어 | 설명 |
|---|---|
| `/murim` | 비급 GUI |
| `/murim mode` | 스킬 모드 토글 (F키와 동일) |
| `/murim stat [플레이어]` | 수치 확인 |
| `/murim set <플레이어> internal\|external <값>` | 내공/외공 설정 (admin) |
| `/murim set <플레이어> prof <종류> <값>` | 숙련도 설정 (admin) |
| `/murim reload` | 설정/비급 리로드 (admin) |
| `/murim genfont` | icons.yml 기준으로 icon.json 생성 (admin) |

## 비급/스킬 추가
`plugins/MurimSkills/manuals/*.yml` 파일을 추가하고 `/murim reload`.
스킬의 `mythic_skill` 은 MythicMobs 스킬 이름, `icon` 은 아이콘 이름입니다.

## 새 아이콘 추가
1. 16x16 PNG 를 `resourcepack/assets/murim/textures/font/skill/<아이콘이름>.png` 로 저장
2. 비급 yml 의 `icon: "<아이콘이름>"` 지정 후 `/murim reload` (icons.yml 에 코드포인트 자동 할당)
3. `/murim genfont` → `plugins/MurimSkills/generated/icon.json` 을 리소스팩 `assets/murim/font/icon.json` 에 덮어쓰기
4. 리소스팩 다시 적용

## 직접 그린 텍스처로 교체할 때 (파일명 유지)
| 파일 | 크기 | 용도 |
|---|---|---|
| slot_normal.png / slot_cooldown.png / slot_empty.png | 18x18 | 슬롯 틀 (일반 / 쿨타임 빨강 / 빈칸) |
| skill/*.png | 16x16 | 스킬 아이콘 |
| digits.png | 88x8 (8x8 칸 11개: 0~9 와 '.') | 쿨타임·키번호 숫자 (흰색으로 그리기) |

- 크기를 바꾸면 `HudRenderer.java` 의 SLOT_ADV / ICON_ADV / PITCH 상수와 hud.json 의 height 도 같이 바꿔야 합니다.
- **스킬바 세로 위치**: `hud.json` 의 `ascent` 값 (-23 등). 숫자를 줄이면(더 음수) 아래로, 키우면 위로 이동합니다. 숫자/아이콘 ascent 는 슬롯 ascent 기준 상대값이라 함께 움직여야 합니다. `tools/gen_assets.py` 의 `SLOT_ASCENT` 를 바꾸고 재실행하면 한 번에 갱신됩니다.

## 구조
```
MurimPlugin        진입점, 서비스 연결
data/              PlayerData, PlayerDataStore(YAML), StatService(기력)
manual/            ManualRegistry(YAML 로더), ManualDef, SkillDef, Requirements(조건 판정)
skill/             SkillService(등록/습득/쿨타임/Mythic 발동), SkillModeManager(F키·1~8키)
hud/               HudRenderer(액션바 스킬바), IconRegistry(아이콘 글자 매핑)
gui/               MenuManager(비급 → 스킬 → 슬롯 GUI)
event/             ManualRegisterEvent, SkillCastEvent (다른 플러그인 연동용)
```
