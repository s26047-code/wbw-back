#!/usr/bin/env bash
# Runs on the server. Opens (or reuses) a public HTTPS address via Cloudflare quick tunnel,
# pins it as the OAuth base URL in ~/wbw/.env, restarts the app and prints the exact Google redirect URI.
set -e
cd ~/wbw

if ! command -v cloudflared >/dev/null; then
  ARCH=$(dpkg --print-architecture)
  curl -fsSL -o /tmp/cloudflared.deb "https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-${ARCH}.deb"
  sudo dpkg -i /tmp/cloudflared.deb >/dev/null
fi
BIN=$(command -v cloudflared)

if [ ! -f /etc/systemd/system/wbw-tunnel.service ]; then
  sudo tee /etc/systemd/system/wbw-tunnel.service >/dev/null <<UNIT
[Unit]
Description=wbw public tunnel (Cloudflare quick tunnel)
After=network-online.target wbw.service
Wants=network-online.target

[Service]
User=ubuntu
ExecStart=${BIN} tunnel --no-autoupdate --url http://localhost:8080
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
UNIT
  sudo systemctl daemon-reload
  sudo systemctl enable wbw-tunnel >/dev/null 2>&1
fi

# 1) 지금 터널이 살아 있으면 주소를 그대로 재사용 (주소가 바뀌지 않도록)
URL=""
if systemctl is-active --quiet wbw-tunnel && [ -s public-url.txt ]; then
  CANDIDATE=$(cat public-url.txt)
  if curl -sf -m 15 "$CANDIDATE/actuator/health" >/dev/null; then URL="$CANDIDATE"; echo "Reusing tunnel: $URL"; fi
fi

# 2) 아니면 터널을 새로 시작하고 새 주소를 받는다
if [ -z "$URL" ]; then
  sudo systemctl restart wbw-tunnel
  echo "Waiting for public URL..."
  for i in $(seq 1 40); do
    URL=$(sudo journalctl -u wbw-tunnel --since "-3 min" --no-pager | grep -o 'https://[a-z0-9-]*\.trycloudflare\.com' | tail -1)
    [ -n "$URL" ] && break
    sleep 2
  done
  [ -z "$URL" ] && { echo "Tunnel did not start:"; sudo journalctl -u wbw-tunnel -n 30 --no-pager; exit 1; }
  echo "$URL" > public-url.txt
fi

# 3) OAuth 콜백 주소를 공개 주소로 고정
if grep -q '^OAUTH_BASE_URL=' .env; then
  sed -i "s#^OAUTH_BASE_URL=.*#OAUTH_BASE_URL=${URL}#" .env
else
  printf '\nOAUTH_BASE_URL=%s\n' "$URL" >> .env
fi
sudo systemctl restart wbw
echo "Restarting app..."
for i in $(seq 1 40); do curl -sf localhost:8080/actuator/health >/dev/null && break; sleep 3; done

# 4) 백엔드가 실제로 구글에 보내는 redirect_uri 확인
SENT=$(curl -s -o /dev/null -w '%{redirect_url}' localhost:8080/oauth2/authorization/google \
  | grep -o 'redirect_uri=[^&]*' | cut -d= -f2 \
  | python3 -c 'import sys,urllib.parse;print(urllib.parse.unquote(sys.stdin.read().strip()))')
EXPECTED="${URL}/login/oauth2/code/google"

echo
echo "PUBLIC URL      : $URL"
echo "SWAGGER         : $URL/swagger-ui.html"
echo "BACKEND SENDS   : $SENT"
echo "REGISTER IN GCP : $EXPECTED"
if [ "$SENT" = "$EXPECTED" ]; then echo "CHECK           : OK (backend redirect_uri matches the public URL)"; else echo "CHECK           : MISMATCH"; exit 1; fi
