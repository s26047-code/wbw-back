package com.wbw.settings;

import com.wbw.auth.LoginUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    public SettingsDtos.SettingsResponse get(@LoginUser Long userId) {
        return SettingsDtos.SettingsResponse.from(settingsService.get(userId));
    }

    @PutMapping
    public SettingsDtos.SettingsResponse update(@LoginUser Long userId,
                                                @Valid @RequestBody SettingsDtos.SettingsRequest request) {
        return SettingsDtos.SettingsResponse.from(settingsService.update(userId, request));
    }
}
