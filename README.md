## **비즈니스 요구사항**

1. **재고 관리**:
    - 각 상품의 재고를 실시간으로 확인할 수 있어야 한다.
    - 재고가 부족할 경우, 주문이 거절되거나 대기열에 추가되어야 한다.
    - 상품의 입고 및 출고 이력을 기록해야 한다.
2. **주문 처리**:
    - 사용자당 동시 다중 주문이 가능해야 한다.
    - 주문 상태(예: 생성, 처리 중, 배송 완료)를 실시간으로 업데이트한다.
    - 대량 주문을 안정적으로 처리할 수 있는 시스템이 필요하다.
3. **시스템 안정성 및 확장성**:
    - 예상치 못한 주문 폭주에도 시스템이 안정적으로 작동해야 한다.
    - 로드 밸런싱, 캐싱, 분산 처리를 통해 확장성을 확보해야 한다.
4. **데이터 무결성**:
    - 재고 감소 및 주문 생성이 트랜잭션 단위로 처리되어 데이터 무결성을 보장해야 한다.


## **시스템 설계도**
![Image](https://github.com/user-attachments/assets/b233a356-4e9f-47c8-b213-13be63e7fc24)

기술적 구현 세부사항

🔁 트랜잭션 처리 전략
- 재고 차감 + 주문 생성은 단일 트랜잭션으로 처리
- Spring의 @Transactional 어노테이션 사용
- 예외 발생 시 전체 트랜잭션이 자동 롤백되어 불완전한 주문 상태 방지
- 트랜잭션 내 Event 전송 타이밍, 롤백 영향 범위 등을 고려해 명확한 트랜잭션 경계 설정

🔀 Saga 패턴 (Choreography 방식)
- 서비스 간 분산 트랜잭션 보장을 위해 Kafka 기반 Saga 기반 코레오 그래피 방식 도입
- 각 서비스는 Kafka 이벤트를 비동기적으로 소비하고 자신의 로직을 수행
- 실패 시 보상 트랜잭션(Compensation Logic) 을 통해 상태 복구
- 이벤트 설계 시 정합성 보장을 위해 TransactionEventListener도 함께 고려하여 실험

📦 Outbox 패턴
- Kafka 메시지 전송 실패 가능성을 고려해 Outbox 테이블을 별도로 두어 신뢰성 확보
- 주문 생성 후 Kafka 메시지를 바로 전송하지 않고, Outbox 테이블에 저장
- @Scheduled 작업에서 미전송 메시지를 조회하여 Kafka 전송 시도
- 전송 성공 시 published=true로 상태 변경
- Kafka 전송의 멱등성 보장을 위해 aggregateId + published 조건 중복 방지 쿼리 도입
- 향후 장애 대응을 위해 DLQ(Dead Letter Queue) 설계 고려

🔒 동시성 제어 (분산 락)
- Redisson 기반 분산 락 @RedissonLock 커스텀 어노테이션 적용
- 상품 재고 차감 시 동시에 여러 사용자가 접근하더라도 정확한 수량 감소 보장
- 성능 측정을 위해 1만 명 동시 요청 시나리오 테스트 작성
- 실제 테스트 결과, Redisson 락 적용 시 중복 주문 및 오버 셀링 현상 없이 정상 동작 확인

⚡ 계층적 캐싱 전략
- 1차 캐시 (Local - Caffeine): 빠른 응답을 위한 메모리 캐시
- 2차 캐시 (Distributed - Redis): 장애 대응 및 서버 간 공유
- READ, WRITE, DELETE 시나리오에 따른 캐시 정책 정의 
- @Cached 커스텀 AOP 어노테이션 구현 → 서비스 내 중복 캐시 로직 제거 및 유지보수성 향상
- @Cacheable의 한계(복합 키, Null 캐싱 등)를 극복하고, CompositeCache (Local+Redis) 조합을 직접 구현하여 성능과 일관성 확보

☎️ Feign Client 통신 안정성
- 외부 서비스 호출 시 Feign Client 사용
- 장애 대응을 위해 Retry 정책과 Fallback 메커니즘 구현

🧪 테스트 전략
- 단위 테스트: 각 도메인 서비스 단위로 @MockBean, Mockito, Stub 등을 활용한 테스트
- 통합 테스트: 실제 DB 및 Kafka 환경 포함하여 @SpringBootTest 기반 E2E 흐름 검증
- 동시성 테스트: ExecutorService 및 CountDownLatch 활용하여 멀티스레드 테스트 진행

🛠️ 예외 처리 및 에러 핸들링
- 전역 예외 처리기(@ControllerAdvice)를 통해 일관된 에러 응답 구조 제공
- 서비스 내 커스텀 예외(OrderCreationException, StockNotFoundException 등)를 정의하여 의도 명확화
- Kafka 메시지 전송 실패 시 로그 기록 및 재시도 또는 DLQ 처리 방식 설계 중

🧩 멀티 모듈 아키텍처
- 도메인 책임을 명확히 분리하여 유지보수성 향상
- order, payment, store, kafka, gateway 등 모듈별 역할 분리
- 각 모듈은 독립 배포 및 테스트 가능 → 확장성과 CI/CD 최적화 용이