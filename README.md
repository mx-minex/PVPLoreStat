# PVPLoreStat

아이템 로어 기반 스탯 시스템을 제공하는 마인크래프트 PVP 플러그인입니다.

## 개요

PVPLoreStat은 아이템의 로어(설명)에 작성된 스탯을 자동으로 인식하여 전투에 적용하는 플러그인입니다. 공격력, 방어력, 체력, 피흡수, 치명타, 회피율 등 다양한 스탯을 지원하며, GUI 에디터를 통해 쉽게 아이템 스탯을 편집할 수 있습니다.

### 주요 기능

- **로어 기반 스탯 시스템**: 아이템 로어에서 스탯을 자동 파싱
- **다양한 스탯 지원**: 공격력, 방어력, 체력, 피흡수, 치명타 확률/데미지, 회피율
- **커스터마이징 가능한 로어 형식**: config.yml에서 로어 형식을 자유롭게 설정
- **GUI 스탯 에디터**: 손에 든 아이템의 스탯을 GUI로 편집
- **실시간 스탯 업데이트**: 장비 변경 시 즉시 스탯 반영
- **PVP/PVE 모드 선택**: PVP 전용 또는 PVE 포함 설정 가능

## 스탯 시스템

### 지원 스탯

| 스탯 | 설명 | 기본 형식 |
|------|------|-----------|
| 공격력 (Damage) | 추가 공격 데미지 | `⚔ 공격력 +{value}` |
| 방어력 (Defense) | 받는 데미지 감소 | `🛡 방어력 +{value}` |
| 체력 (Health) | 최대 체력 증가 | `❤ 체력 +{value}` |
| 피흡수 (Lifesteal) | 공격 시 체력 회복 (%) | `🩸 피흡수 {value}%` |
| 치명타 확률 (Crit Chance) | 치명타 발생 확률 (%) | `⚡ 치명타 확률 {value}%` |
| 치명타 데미지 (Crit Damage) | 치명타 추가 데미지 | `💥 치명타 데미지 +{value}` |
| 회피율 (Dodge) | 공격 회피 확률 (%) | `💨 회피율 {value}%` |

### 스탯 계산 공식

```
최종 데미지 = (기본 데미지 + 공격력 / divisor - 방어력 / divisor) × 치명타 배수
피흡수량 = 최종 데미지 × (피흡수 / 100)
최대 체력 = 기본 체력(20) + 체력 스탯
```

## 요구사항

- **Minecraft 버전:** 1.20 이상
- **서버:** Spigot / Paper
- **Java:** 17 이상

## 설치 방법

1. [Releases](https://github.com/mx-minex/PVPLoreStat/releases)에서 최신 버전의 JAR 파일을 다운로드합니다.
2. 서버의 `plugins` 폴더에 JAR 파일을 넣습니다.
3. 서버를 재시작합니다.
4. `plugins/PVPLoreStat/config.yml`에서 설정을 커스터마이즈합니다.

## 명령어

| 명령어 | 설명 | 권한 |
|--------|------|------|
| `/pls help` | 도움말 표시 | 없음 |
| `/pls reload` | 설정 파일 리로드 | `pvplorestat.reload` |
| `/pls stats` | 현재 스탯 확인 | `pvplorestat.stats` |
| `/pls edit` | 손에 든 아이템의 스탯 편집 (GUI) | `pvplorestat.edit` |

**명령어 별칭:** `/pvplorestat`, `/로어`

## 권한

| 권한 | 설명 | 기본값 |
|------|------|--------|
| `pvplorestat.*` | 모든 권한 | OP |
| `pvplorestat.reload` | 설정 리로드 | OP |
| `pvplorestat.stats` | 스탯 확인 | 모든 플레이어 |
| `pvplorestat.edit` | 아이템 스탯 편집 | OP |

## 설정

### config.yml

```yaml
# PVPLoreStat v1.0.0 설정

# 일반 설정
settings:
  # 스탯 업데이트 간격 (틱, 20틱 = 1초)
  update-interval: 10

  # PVP만 적용 (false = PVE도 적용)
  pvp-only: true

  # 디버그 모드
  debug: false

# 스탯 계산 설정
stats:
  damage:
    # 공격력 적용 비율 (공격력 / divisor = 추가 데미지)
    divisor: 2.0
    # 최대 공격력 (0 = 무제한)
    max: 0

  defense:
    divisor: 2.0
    max: 0

  health:
    # 체력 스탯은 그대로 최대 체력에 추가됩니다 (체력 10 = +10 체력)
    max: 0
    # 기본 최대 체력 (하트 수 * 2)
    base: 20.0

  lifesteal:
    # 최대 피흡수율 (%)
    max: 100

  critchance:
    # 최대 치명타 확률 (%)
    max: 100

  critdamage:
    divisor: 2.0
    max: 0

  dodge:
    # 최대 회피율 (%)
    max: 80

# 무기로 인식할 아이템 타입 (스탯 적용 대상)
# 와일드카드(*) 지원: *_SWORD는 모든 검을 포함
weapons:
  - "*_SWORD"
  - "*_AXE"

# 로어 디자인
lore:
  # 구분선 설정
  separator:
    enabled: true
    top: "&8&m─────&r &6✦ 스탯 &8&m─────"
    bottom: "&8&m──────────────────"

  # 스탯 표시 형식
  # {value} 플레이스홀더에 수치가 들어갑니다
  format:
    damage: "&c⚔ 공격력 &f+{value}"
    defense: "&9🛡 방어력 &f+{value}"
    health: "&6❤ 체력 &f+{value}"
    lifesteal: "&4🩸 피흡수 &f{value}%"
    critchance: "&e⚡ 치명타 확률 &f{value}%"
    critdamage: "&5💥 치명타 데미지 &f+{value}"
    dodge: "&b💨 회피율 &f{value}%"

  # 스탯 표시 순서
  order:
    - damage
    - defense
    - health
    - lifesteal
    - critchance
    - critdamage
    - dodge
```

### 로어 형식 커스터마이징

`lore.format` 섹션에서 각 스탯의 표시 형식을 자유롭게 설정할 수 있습니다. 플러그인은 설정된 형식과 **정확히 일치하는** 로어만 스탯으로 인식합니다.

**예시:**
```yaml
# 기본 형식
damage: "&c⚔ 공격력 &f+{value}"
# 결과: §c⚔ 공격력 §f+100

# 간단한 형식
damage: "&c공격력: {value}"
# 결과: §c공격력: 100

# 영어 형식
damage: "&cDamage: +{value}"
# 결과: §cDamage: +100
```

**중요:** 로어 형식을 변경하면 기존 아이템의 로어도 새 형식으로 작성되어야 인식됩니다.

### messages.yml

플러그인의 모든 메시지를 커스터마이즈할 수 있습니다:

```yaml
prefix: "&8[&6PVPLoreStat&8] "

commands:
  reload-success: "&a설정을 다시 불러왔습니다."
  no-permission: "&c권한이 없습니다."
  # ...

combat:
  critical-hit: "&e치명타!"
  dodge: "&b회피!"
  # ...
```

## GUI 에디터

`/pls edit` 명령어로 손에 든 아이템의 스탯을 GUI로 편집할 수 있습니다.

### 사용 방법

1. 편집할 아이템을 손에 들고 `/pls edit` 실행
2. GUI에서 스탯 아이콘을 클릭하여 값 조정
   - **좌클릭**: +1
   - **Shift + 좌클릭**: +10
   - **우클릭**: -1
   - **Shift + 우클릭**: -10
3. 초록색 확인 버튼을 클릭하여 저장
4. 빨간색 취소 버튼을 클릭하여 취소

### GUI 구성

```
[공격력] [방어력] [체력] [피흡수]
[치확]  [치뎀]   [회피]
        [저장]  [취소]
```

## 작동 방식

### 스탯 파싱

1. 플레이어가 장비를 변경하면 이벤트가 감지됩니다.
2. 모든 장비 슬롯(투구, 흉갑, 레깅스, 부츠, 주무기, 보조손)의 아이템을 확인합니다.
3. 각 아이템의 로어에서 `config.yml`에 설정된 형식과 일치하는 스탯을 파싱합니다.
4. 모든 장비의 스탯을 합산하여 플레이어 총 스탯을 계산합니다.
5. 체력 스탯에 따라 최대 체력이 조정됩니다.

### 전투 적용

1. 플레이어가 공격하면 데미지 이벤트가 감지됩니다.
2. PVP-only 설정에 따라 대상을 확인합니다.
3. 공격자의 공격력과 무기 스탯을 적용합니다.
4. 치명타 확률을 계산하여 치명타 데미지를 적용합니다.
5. 피해자의 회피율을 계산하여 회피 여부를 결정합니다.
6. 피해자의 방어력을 적용하여 최종 데미지를 계산합니다.
7. 피흡수 스탯에 따라 공격자의 체력을 회복합니다.

## 아키텍처

PVPLoreStat은 헥사고날(포트와 어댑터) 아키텍처를 기반으로 설계되었습니다:

```
src/main/java/kr/minex/pvplorestat/
├── domain/           # 도메인 계층 (핵심 비즈니스 로직)
│   ├── model/        # 도메인 모델 (ItemStats, PlayerStats, StatType)
│   └── service/      # 도메인 서비스 (DamageCalculator)
├── application/      # 애플리케이션 계층 (유스케이스)
│   └── service/      # 애플리케이션 서비스 (PlayerStatsService)
├── infrastructure/   # 인프라 계층 (외부 시스템 연동)
│   ├── config/       # 설정 관리 (ConfigManager)
│   └── lore/         # 로어 처리 (LoreManager, LoreTemplate)
└── presentation/     # 프레젠테이션 계층 (사용자 인터페이스)
    ├── command/      # 명령어 처리
    ├── listener/     # 이벤트 리스너
    └── gui/          # GUI 인터페이스
```

## 빌드 방법

```bash
./gradlew build
```

빌드된 JAR 파일은 `build/libs/PVPLoreStat-{version}.jar` 형태로 생성됩니다.

### 테스트 실행

```bash
./gradlew test
```

## API 사용 (개발자용)

다른 플러그인에서 PVPLoreStat의 기능을 사용할 수 있습니다:

```java
// 플러그인 인스턴스 가져오기
PVPLoreStat plugin = PVPLoreStat.getInstance();

// 플레이어 스탯 조회
PlayerStatsService statsService = plugin.getPlayerStatsService();
PlayerStats stats = statsService.getStats(player.getUniqueId());

// 총 스탯 확인
ItemStats totalStats = stats.getTotalStats();
double damage = totalStats.getDamage();
double defense = totalStats.getDefense();

// 아이템에서 스탯 파싱
LoreManager loreManager = plugin.getLoreManager();
ItemStats itemStats = loreManager.parseLore(item.getItemMeta().getLore());

// 스탯으로 로어 생성
ItemStats newStats = ItemStats.builder()
    .damage(100)
    .defense(50)
    .build();
List<String> lore = loreManager.generateLore(newStats);
```

## 호환성

### 테스트된 서버 소프트웨어

- Paper 1.20.x
- Spigot 1.20.x

### 알려진 호환 플러그인

- MMOItems
- ItemsAdder
- Oraxen
- EcoItems

**주의:** 다른 스탯 플러그인과 함께 사용 시 스탯이 중복 적용될 수 있습니다.

## FAQ

### Q: 로어 형식을 변경했는데 기존 아이템이 인식되지 않아요.

A: 로어 형식 변경 후에는 기존 아이템의 로어도 새 형식으로 수정해야 합니다. `/pls edit` 명령어로 아이템을 다시 편집하면 새 형식으로 저장됩니다.

### Q: 체력이 제대로 적용되지 않아요.

A: `config.yml`의 `stats.health.base` 값이 올바른지 확인하세요. 기본값은 20 (하트 10개)입니다.

### Q: PVE에서도 스탯을 적용하고 싶어요.

A: `config.yml`에서 `settings.pvp-only`를 `false`로 설정하세요.

### Q: 특정 무기에만 공격력을 적용하고 싶어요.

A: `config.yml`의 `weapons` 섹션에서 무기 타입을 설정하세요. 와일드카드(`*`)를 사용할 수 있습니다.

## 라이선스

이 프로젝트는 [GNU General Public License v3.0](LICENSE) 하에 배포됩니다.

## 개발자

- **Minex** (mx-minex)
- GitHub: https://github.com/mx-minex

## 기여

버그 리포트, 기능 제안, Pull Request를 환영합니다!

1. 이 저장소를 Fork합니다.
2. 새 브랜치를 생성합니다: `git checkout -b feature/AmazingFeature`
3. 변경사항을 커밋합니다: `git commit -m 'Add some AmazingFeature'`
4. 브랜치에 Push합니다: `git push origin feature/AmazingFeature`
5. Pull Request를 생성합니다.
