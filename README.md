| 패키지 | 역할 | 의존 가능한 대상 |
| --- | --- | --- |
| `domain.model` | 비즈니스 객체 (Sentence, Difficulty) | **없음** (순수 자바만) |
| `domain.port.in` | 외부→도메인 인터페이스 | domain.model만 |
| `domain.port.out` | 도메인→외부 인터페이스 | domain.model만 |
| `application.service` | 포트 구현, 비즈니스 로직 조합 | domain 전체 |
| `adapter.in.web` | REST Controller | domain.port.in, domain.model |
| `adapter.out.persistence` | JPA Entity, Repository | domain.port.out, domain.model |