#!/bin/bash
# PostToolUse hook: compiles what an Edit/Write just touched and, on failure, exits 2 so the
# compiler output reaches the agent as feedback. Unrelated files exit 0 without doing anything.
set -uo pipefail

root="$CLAUDE_PROJECT_DIR"
file="$(jq -r '.tool_input.file_path // empty')"
rel="${file#"$root"/}"

report() {
  # Long compiler output buries the first error the agent needs, so only the head is passed on
  { echo "$1"; head -n 80 <<<"$2"; } >&2
  exit 2
}

case "$rel" in
  rfr-auth/src/main/*.java | rfr-api/src/main/*.java)
    module="${rel%%/*}"
    out="$(cd "$root" && ./gradlew -q --console=plain ":$module:compileJava" 2>&1)" \
      || report "Compilation of $module failed after editing $rel" "$out"
    ;;
  rfr-e2e/src/test/*.java)
    out="$(cd "$root" && ./gradlew -q --console=plain :rfr-e2e:compileTestJava 2>&1)" \
      || report "Compilation of rfr-e2e failed after editing $rel" "$out"
    ;;
  rfr-gql-client/src/*.ts | rfr-gql-client/src/*.tsx)
    client="$root/rfr-gql-client"
    out="$(cd "$client" && npx --no-install tsc --noEmit --pretty false 2>&1)" \
      || report "Type check of rfr-gql-client failed after editing $rel" "$out"
    # Same flags as `npm run lint` in CI, where a single warning fails the job
    out="$(cd "$client" && npx --no-install eslint --report-unused-disable-directives --max-warnings 0 \
      "${rel#rfr-gql-client/}" 2>&1)" \
      || report "ESLint failed on $rel" "$out"
    ;;
esac
exit 0
