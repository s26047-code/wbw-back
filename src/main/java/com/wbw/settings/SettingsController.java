package com.wbw.settings;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.wbw.auth.LoginUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
@Tag(name = "개인 설정", description = "테마·글꼴·간격 등 글쓰기 환경")
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @Operation(summary = "설정 조회", description = "처음 호출 시 기본값으로 생성됩니다.")
    @GetMapping
    public SettingsDtos.SettingsResponse get(@LoginUser Long userId) {
        return SettingsDtos.SettingsResponse.from(settingsService.get(userId));
    }

    @Operation(summary = "설정 저장", description = "theme=CUSTOM 이면 backgroundColor, textColor 필수. fontFamily는 /api/fonts 목록 중 하나.")
    @PutMapping
    public SettingsDtos.SettingsResponse update(@LoginUser Long userId,
                                                @Valid @RequestBody SettingsDtos.SettingsRequest request) {
        return SettingsDtos.SettingsResponse.from(settingsService.update(userId, request));
    }
}
