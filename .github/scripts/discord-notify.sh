#!/usr/bin/env bash
# GitHub Actions 결과를 Discord 웹훅으로 보낸다.
# 사용법: discord-notify.sh <success|failure|cancelled> "<제목>" ["<추가 설명(선택)>"]
# 환경변수: WEBHOOK_URL(필수, 없으면 건너뜀), COMMIT_MESSAGE, PR_TITLE, PR_NUMBER, PR_URL (워크플로에서 전달)
set -euo pipefail

STATUS="${1:-unknown}"
TITLE="${2:-GitHub Actions}"
EXTRA="${3:-}"

if [ -z "${WEBHOOK_URL:-}" ]; then
  echo "::notice::Discord 웹훅이 설정되지 않아 알림을 건너뜁니다."
  exit 0
fi

case "$STATUS" in
  success)   ICON="✅"; COLOR=3066993 ;;   # 초록
  failure)   ICON="❌"; COLOR=15158332 ;;  # 빨강
  cancelled) ICON="⚪"; COLOR=9807270 ;;   # 회색
  *)         ICON="❔"; COLOR=9807270 ;;
esac

RUN_URL="${GITHUB_SERVER_URL}/${GITHUB_REPOSITORY}/actions/runs/${GITHUB_RUN_ID}"
SHA="${GITHUB_SHA:0:7}"
COMMIT_URL="${GITHUB_SERVER_URL}/${GITHUB_REPOSITORY}/commit/${GITHUB_SHA}"

if [ -n "${PR_NUMBER:-}" ]; then
  WHERE="[PR #${PR_NUMBER}](${PR_URL}) → \`${GITHUB_BASE_REF:-}\`"
  DESC="${PR_TITLE:-}"
else
  WHERE="\`${GITHUB_REF_NAME}\`"
  DESC="$(printf '%s' "${COMMIT_MESSAGE:-}" | head -n 1)"
fi
[ -n "$EXTRA" ] && DESC="${DESC}"$'\n'"${EXTRA}"

PAYLOAD=$(jq -n \
  --arg title "${ICON} ${TITLE}" \
  --arg url "$RUN_URL" \
  --arg desc "$DESC" \
  --argjson color "$COLOR" \
  --arg where "$WHERE" \
  --arg commit "[\`${SHA}\`](${COMMIT_URL})" \
  --arg actor "${GITHUB_ACTOR}" \
  --arg repo "${GITHUB_REPOSITORY}" \
  '{
     username: "wbw-back CI/CD",
     embeds: [{
       title: $title, url: $url, description: $desc, color: $color,
       fields: [
         {name: "위치", value: $where, inline: true},
         {name: "커밋", value: $commit, inline: true},
         {name: "작성자", value: $actor, inline: true}
       ],
       footer: {text: $repo},
       timestamp: (now | todate)
     }]
   }')

curl -fsS -X POST -H "Content-Type: application/json" -d "$PAYLOAD" "$WEBHOOK_URL" >/dev/null \
  || echo "::warning::Discord 알림 전송 실패"
