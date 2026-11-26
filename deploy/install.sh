#!/usr/bin/env bash
# Runs on the server (called by deploy.ps1). Replaces the app and restarts the service.
set -e
cd ~/wbw
# PDF 한글용 고정 굵기 TTF (나눔명조/나눔고딕). 없으면 설치
if ! dpkg -s fonts-nanum >/dev/null 2>&1; then
  echo "Installing Korean fonts for PDF (fonts-nanum)..."
  sudo apt-get update -qq && sudo apt-get install -y -qq fonts-nanum >/dev/null
fi
# Pretendard 고정 굵기 TTF (Regular/Bold) -> ~/wbw/fonts. 실패해도 배포는 계속 (나눔 글꼴로 대체됨)
PRETENDARD_VERSION=1.3.9
mkdir -p fonts
if [ ! -f fonts/Pretendard-Regular.ttf ] || [ ! -f fonts/Pretendard-Bold.ttf ]; then
  echo "Installing Pretendard ${PRETENDARD_VERSION} (static TTF) for PDF..."
  if curl -fsSL -o /tmp/pretendard.zip \
      "https://github.com/orioncactus/pretendard/releases/download/v${PRETENDARD_VERSION}/Pretendard-${PRETENDARD_VERSION}.zip"; then
    python3 - <<'PY' || echo "WARN: Pretendard TTF extraction failed"
import zipfile, os, sys
z = zipfile.ZipFile('/tmp/pretendard.zip')
names = z.namelist()
for want in ('Pretendard-Regular.ttf', 'Pretendard-Bold.ttf'):
    cands = [n for n in names if os.path.basename(n) == want]
    # 가변 글꼴(variable)이 아닌 고정(static) 파일 우선
    cands.sort(key=lambda n: ('variable' in n.lower(), 'static' not in n.lower(), len(n)))
    if not cands:
        print('WARN: not found in archive:', want); sys.exit(1)
    with z.open(cands[0]) as src, open(os.path.join('fonts', want), 'wb') as dst:
        dst.write(src.read())
    print('  installed', cands[0])
PY
    rm -f /tmp/pretendard.zip
  else
    echo "WARN: Pretendard download failed (PDF will fall back to Nanum fonts)"
  fi
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
