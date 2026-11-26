# PDF용 한글 글꼴

> **리눅스 서버**는 `sudo apt install fonts-nanum` 만 하면 나눔명조·나눔고딕(일반/굵게)이 자동으로 사용됩니다. (배포 스크립트가 자동 설치)
> PDF에는 고정 굵기 TTF만 사용합니다. WOFF2·가변 글꼴·OTF/TTC(fonts-noto-cjk)는 한글이 깨질 수 있어 쓰지 않습니다.

PDF 내보내기에서 한글을 표시하려면 이 폴더에 아래 TTF 파일을 넣어야 합니다.
모두 SIL Open Font License(무료, 재배포 가능) 글꼴이며 Google Fonts에서 받을 수 있습니다.
최소 1개(권장: Noto Serif KR)만 있어도 한글이 표시됩니다.

| 파일명 | 받는 곳 |
|---|---|
| Pretendard-Regular.ttf, Pretendard-Bold.ttf | https://github.com/orioncactus/pretendard/releases (zip 안의 고정 굵기 TTF, 서버는 배포 스크립트가 자동 설치) |
| NotoSerifKR-Regular.ttf, NotoSerifKR-Bold.ttf | https://fonts.google.com/noto/specimen/Noto+Serif+KR (압축 해제 후 `static` 폴더) |
| NotoSansKR-Regular.ttf, NotoSansKR-Bold.ttf | https://fonts.google.com/noto/specimen/Noto+Sans+KR (`static` 폴더) |
| NanumMyeongjo-Regular.ttf, NanumMyeongjo-Bold.ttf | https://fonts.google.com/specimen/Nanum+Myeongjo |
| NanumGothic-Regular.ttf, NanumGothic-Bold.ttf | https://fonts.google.com/specimen/Nanum+Gothic |
| GowunBatang-Regular.ttf, GowunBatang-Bold.ttf | https://fonts.google.com/specimen/Gowun+Batang |
| GowunDodum-Regular.ttf | https://fonts.google.com/specimen/Gowun+Dodum |

파일명이 표와 정확히 같아야 합니다. 서버 시작 로그에 `PDF 글꼴 n/6개 사용 가능` 이 표시됩니다.
