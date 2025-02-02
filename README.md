## 이동 윈도우 카운터(Sliding Window Counter)  

---  

가상 면접 사례로 배우는 대규모 시스템 설계 기초 1권 4장 '처리율 제한 장치 설계'의 이동 윈도우 카운터를 구현  

<br>

### 구현 방식  

---  

Spring Cloud Gateway의 GlobalFilter + Redis + lua script  
- 1분당 100회의 처리율 제한 설정  
- 목적지 instance-id를 Redis Hash의 키로 활용
- Redis Hash
  - previous(직전 1분), current(현재 1분) 2개의 Hash
  - 각 Hash는 window, request count를 기록  
- 아래 계산 결과가 처리율 제한을 넘지 않는다면 current의 요청 횟수 증가
  - 현재 1분의 요청 수 + (직전 1분 요청 수 * 현재 윈도우 중 직전 1분의 비율) + 1  

<br>

### 폴더 구조  

---  

```shell
.gateway
├── build.gradle
├── gradle
├── gradlew
├── gradlew.bat
├── settings.gradle
└── src
    ├── main
    │   ├── java
    │   │   └── org
    │   │       └── example
    │   │           └── gateway
    │   │               ├── GatewayApplication.java
    │   │               ├── config
    │   │               │   └── RedisConfig.java
    │   │               ├── filter
    │   │               │   └── RateLimitFilter.java
    │   │               └── support
    │   │                   └── RateLimiter.java
    │   └── resources
    │       ├── application.yml
    │       ├── script
    │       │   └── incrRequestCount.lua
    │       ├── static
    │       └── templates
    └── test
```