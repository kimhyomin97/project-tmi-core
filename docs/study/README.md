# 학습 노트

이 프로젝트를 작업하면서 나온 개념을 **이 코드에서 실제로 일어난 일** 기준으로 정리한 노트다.
개념 설명은 짧게 쓰고, 왜 중요한지는 이 저장소의 사례로 보여 준다.

> 이 폴더는 커밋된다. 로컬에서는 `git pull` 로 받는다.

## 읽는 순서

| 순서 | 노트 | 한 줄 요약 | 관련 작업 |
|---|---|---|---|
| 1 | [spring/transactional-proxy.md](spring/transactional-proxy.md) | `@Transactional` 은 프록시가 처리한다 → 같은 클래스 안에서 부르면 무시된다 | `FIX-F4` |
| 2 | [jpa/osiv.md](jpa/osiv.md) | OSIV 는 요청이 끝날 때까지 DB 커넥션을 잡아 둘 수 있다 | `FIX-F4`, D4 |
| 3 | [spring/exception-handling.md](spring/exception-handling.md) | 예외가 HTTP 응답이 되기까지의 경로, 그리고 500 이 잘못 나오는 이유 | `FIX-F3`, `FIX-F1T`, D2 |
| 4 | [spring/http-interface.md](spring/http-interface.md) | 인터페이스만 선언하면 HTTP 클라이언트가 생긴다. 단 파라미터 어노테이션은 필수 | `FIX-F1` |
| 5 | [be-standards/README.md](be-standards/README.md) | 국내 IT 기업 BE 관례(근거 등급 표시, 국내 기업 원문 미확인)와 이 프로젝트의 연습 로드맵 | 전체 (D1~D7 참고) |

1·2 는 함께 읽으면 좋다. 같은 결함(F4)의 두 원인이다.

## 노트 형식

모든 노트는 같은 순서로 쓴다.

1. **한 줄 요약**
2. **이 프로젝트에서 일어난 일** — 왜 알아야 하는지
3. **동작 원리** — 그림이나 흐름 하나
4. **코드로 보기** — 잘못된 예 / 맞는 예
5. **기억할 것** — 3개 이내
6. **더 읽을거리** — 공식 문서 링크
