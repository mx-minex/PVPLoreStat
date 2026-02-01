# Spigot Plugin Template

Minex 조직의 Spigot 플러그인 개발용 템플릿 레포지토리입니다.

## 사용 방법

1. 이 템플릿으로 새 레포지토리 생성
   - "Use this template" 버튼 클릭
   - 레포지토리 이름 입력 (예: `MyPlugin`)

2. 자동 초기화
   - 첫 push 시 GitHub Actions가 자동으로 실행됨
   - 레포지토리 이름을 기반으로 패키지명, 클래스명 자동 변경
   - 예: `MyPlugin` → `kr.minex.myplugin.MyPlugin`

3. 개발 시작
   - `src/main/java/kr/minex/{plugin}/` 에서 개발
   - 빌드: `./gradlew build`
   - 테스트 서버 실행: `./gradlew runServer`

## 프로젝트 구조

```
{plugin-name}/
├── .github/workflows/      # CI/CD 워크플로우
│   ├── ci.yml              # 빌드 및 테스트
│   └── release.yml         # 태그 기반 릴리즈
├── src/
│   ├── main/
│   │   ├── java/kr/minex/{plugin}/
│   │   │   └── {PluginName}.java
│   │   └── resources/
│   │       └── plugin.yml
│   └── test/java/kr/minex/{plugin}/
├── build.gradle
└── settings.gradle
```

## 릴리즈 방법

```bash
git tag v1.0.0
git push origin v1.0.0
```

GitHub Actions가 자동으로:
- 빌드 실행
- GitHub Release 생성
- JAR + SHA256 체크섬 업로드

## 기술 스택

- Java 17
- Spigot API 1.20.1
- Gradle 8.x
- JUnit 5 + Mockito

## 라이선스

이 프로젝트는 [GNU General Public License v3.0](LICENSE) 하에 배포됩니다.
