# 학교 서버 배포 스크립트
# 사용법 (backend 폴더에서):  powershell -ExecutionPolicy Bypass -File deploy\deploy.ps1
# 처음 배포 전에 서버에 ~/wbw/.env 가 있어야 합니다. (.env.server 를 만들어 -WithEnv 옵션으로 올릴 수 있음)
param([switch]$WithEnv)

$Server = "ubuntu@ssh.gsmsv.site"
$Port = 24109
$Jar = "build\libs\wbw-backend-0.1.0.jar"

function Step($msg) { Write-Host "`n==> $msg" -ForegroundColor Cyan }
function Check($what) { if ($LASTEXITCODE -ne 0) { Write-Host "실패: $what" -ForegroundColor Red; exit 1 } }

Step "1/3 실행 파일 빌드"
.\gradlew.bat bootJar
Check "빌드"

Step "2/3 서버로 업로드 (서버 비밀번호 입력)"
$files = @($Jar, "deploy\wbw.service", "deploy\install.sh")
if ($WithEnv) { $files += ".env.server" }
ssh -p $Port $Server "mkdir -p ~/wbw/upload"
Check "서버 접속"
scp -P $Port $files "${Server}:~/wbw/upload/"
Check "업로드"

Step "3/3 서버에서 교체 후 재시작 (약 1분, 서버 비밀번호 입력)"
ssh -t -p $Port $Server "bash ~/wbw/upload/install.sh"
Check "서버 시작"
Write-Host "`n배포 완료!" -ForegroundColor Green
Write-Host "공개 주소 확인: ssh -p $Port $Server `"cat ~/wbw/public-url.txt`"  (뒤에 /swagger-ui.html)" -ForegroundColor Green
