#!/usr/bin/env bash
# 세션 시작 시 1회 (docs/ai/proposals/ai-workflow.md §4.3): 환경 확인 + 기본 신호. 실패해도 끝까지 출력하고 마지막에 요약.
set -uo pipefail
cd "$(dirname "$0")/.."
git log --oneline -10
java -version 2>&1 | grep -m1 version
summary=()
if docker info >/dev/null 2>&1; then summary+=("docker: OK"); else summary+=("docker: 없음 — requires=docker 항목 건너뜀"); fi
if jq -e '.items[] | select(.id=="SIG-1") | .passes' docs/ai/feature_list.json >/dev/null; then
  # SIG-1 이후: DB 를 막고도 통과해야 함 (DB 필요한 테스트가 integration 태그 없이 들어오면 여기서 잡힘)
  SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:1/none bash ./gradlew test --console=plain \
    && summary+=("test: OK") || summary+=("test: FAIL")
else
  bash ./gradlew compileTestJava --console=plain \
    && summary+=("compile: OK (SIG-1 전 — 전체 test 는 DB 필요)") || summary+=("compile: FAIL")
fi
if jq -e '.items[] | select(.id=="SIG-2") | .passes' docs/ai/feature_list.json >/dev/null; then
  bash ./gradlew spotlessCheck --console=plain >/dev/null 2>&1 && summary+=("spotless: OK") || summary+=("spotless: FAIL")
else
  bash ./gradlew spotlessCheck --console=plain >/dev/null 2>&1 && summary+=("spotless: OK") || summary+=("spotless: 위반 있음 (SIG-2 전 — 정보용)")
fi
# 스모크 (SIG-1 이후, DB 가 있는 환경에서만): bootRun 후 출제 API 1회 호출 — 원문의 "기본 end-to-end 확인" [F]
printf '%s\n' "${summary[@]}"
printf '%s\n' "${summary[@]}" | grep -q 'FAIL' && exit 1 || exit 0
