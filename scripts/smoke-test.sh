#!/usr/bin/env bash
# End-to-end smoke test against a running server (default http://localhost:8080).
#
#   ./scripts/run-server.sh &        # or start it from IntelliJ IDEA
#   ./scripts/smoke-test.sh
#
# Environment: BASE_URL, MODEL_ID, PROFILE
set -euo pipefail

BASE="${BASE_URL:-http://localhost:8080}"
MODEL_ID="${MODEL_ID:-smoke-demo}"
PROFILE="${PROFILE:-h2}"

# Sends a request and prints only the HTTP status code.
status() {
  local method="$1" path="$2" body="${3:-}"
  if [[ -n "$body" ]]; then
    curl -sS -o /tmp/smoke-response.json -w '%{http_code}' -X "$method" "$BASE$path" \
      -H 'Content-Type: application/json' --data-binary "$body"
  else
    curl -sS -o /tmp/smoke-response.json -w '%{http_code}' -X "$method" "$BASE$path"
  fi
}

check() {
  local expected="$1" actual="$2" label="$3"
  if [[ "$expected" != "$actual" ]]; then
    echo "FAIL: $label (expected $expected, got $actual)"
    cat /tmp/smoke-response.json
    echo
    exit 1
  fi
  echo "ok: $label -> $actual"
}

json_load=$(printf '{"modelId":"%s","profile":"%s"}' "$MODEL_ID" "$PROFILE")
json_reason=$(printf '{"modelId":"%s","materialize":false}' "$MODEL_ID")
sparql_select='SELECT ?e ?n WHERE { ?e a :SeniorManager ; :name ?n } ORDER BY ?n'
sparql_ask='ASK { <http://example.org/company#dept/2> <http://example.org/company#partOf> <http://example.org/company#dept/5> }'
json_query=$(printf '{"modelId":"%s","sparql":"%s","limit":10}' "$MODEL_ID" "$sparql_select")
json_ask=$(printf '{"modelId":"%s","sparql":"%s"}' "$MODEL_ID" "$sparql_ask")

echo "Smoke test against $BASE (model=$MODEL_ID, profile=$PROFILE)"
check 201 "$(status POST /load "$json_load")" "POST /load"
check 200 "$(status GET "/models/$MODEL_ID")" "GET /models/{id}"
check 200 "$(status POST /reason "$json_reason")" "POST /reason"
check 200 "$(status POST /query "$json_query")" "POST /query (SELECT)"
check 200 "$(status POST /query "$json_ask")" "POST /query (ASK transitive)"
check 200 "$(status GET "/models/$MODEL_ID/explain?subject=%3Aemp%2F5&limit=5")" "GET explain"
check 204 "$(status DELETE "/models/$MODEL_ID")" "DELETE /models/{id}"
check 404 "$(status GET "/models/$MODEL_ID")" "GET deleted model -> 404"

# ---------------------------------------------------------------------------
# Document-driven ontology modeling (heuristic provider, no AI needed)
# ---------------------------------------------------------------------------
echo
echo "Modeling smoke test (heuristic extractor) ..."
create_json=$(python3 - <<'PYMODEL'
import json
text = "第一章 总则\n第一条 为了规范测试行为，制定本示例。\n第二条 “测试术语”是指用于冒烟测试的术语。"
print(json.dumps({"name": "smoke-modeling", "fileName": "smoke.txt", "content": text}, ensure_ascii=False))
PYMODEL
)
curl -sS -X POST "$BASE/modeling/projects" -H 'Content-Type: application/json' \
  --data-binary "$create_json" -o /tmp/smoke-modeling.json
project=$(python3 -c 'import json;print(json.load(open("/tmp/smoke-modeling.json"))["id"])')
model="modeling-$project"
check 200 "$(status POST "/modeling/projects/$project/extract" '{"provider":"heuristic"}')" "POST /modeling/projects/{id}/extract"
check 200 "$(status POST "/modeling/projects/$project/generate" '{}')" "POST /modeling/projects/{id}/generate"
check 200 "$(status POST "/modeling/projects/$project/load" '{"autoReason":true}')" "POST /modeling/projects/{id}/load"
check 200 "$(status GET "/modeling/projects/$project/ontology")" "GET generated T-Box"
check 200 "$(status GET "/modeling/projects/$project/instances")" "GET generated A-Box"
check 200 "$(status POST /reason "$(printf '{"modelId":"%s","materialize":false}' "$model")")" "POST /reason (generated model)"
check 200 "$(status POST /query "$(printf '{"modelId":"%s","sparql":"SELECT ?s ?p ?o WHERE { ?s ?p ?o } LIMIT 5"}' "$model")")" "POST /query (generated model)"
check 204 "$(status DELETE "/modeling/projects/$project")" "DELETE /modeling/projects/{id}"

echo "All smoke tests passed."
