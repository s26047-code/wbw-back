#!/usr/bin/env bash
# Runs on the server (called by deploy.ps1). Replaces the app and restarts the service.
set -e
cd ~/wbw
# PDF 한글용 고정 굵기 TTF (나눔명조/나눔고딕). 없으면 설치
if ! dpkg -s fonts-nanum >/dev/null 2>&1; then
  echo "Installing Korean fonts for PDF (fonts-nanum)..."
  sudo apt-get update -qq && sudo apt-get install -y -qq fonts-nanum >/dev/null
fi
mv upload/wbw-backend-0.1.0.jar app.jar
if [ -f upload/.env.server ]; then mv upload/.env.server .env; chmod 600 .env; fi
sudo cp upload/wbw.service /etc/systemd/system/wbw.service
sudo systemctl daemon-reload
sudo systemctl enable wbw >/dev/null 2>&1
sudo systemctl restart wbw
echo "Waiting for the server to start..."
for i in $(seq 1 40); do
  if curl -sf localhost:8080/actuator/health >/dev/null; then
    echo "DEPLOY OK: $(curl -s localhost:8080/actuator/health)"
    exit 0
  fi
  sleep 3
done
echo "START FAILED - recent logs:"
sudo journalctl -u wbw -n 40 --no-pager
exit 1
