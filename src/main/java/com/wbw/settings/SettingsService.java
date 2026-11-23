package com.wbw.settings;

import com.wbw.common.ApiException;
import com.wbw.common.ErrorCode;
import com.wbw.pdf.FontCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettingsService {

    private final UserSettingsRepository repository;
    private final FontCatalog fontCatalog;

    public SettingsService(UserSettingsRepository repository, FontCatalog fontCatalog) {
        this.repository = repository;
        this.fontCatalog = fontCatalog;
    }

    @Transactional
    public UserSettings get(Long userId) {
        return repository.findById(userId).orElseGet(() -> repository.save(UserSettings.defaults(userId)));
    }

    @Transactional
    public UserSettings update(Long userId, SettingsDtos.SettingsRequest req) {
        if (!fontCatalog.isKnown(req.fontFamily())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "지원하지 않는 글꼴입니다: " + req.fontFamily());
        }
        UserSettings settings = get(userId);
        settings.update(req.theme(), normalize(req.backgroundColor()), normalize(req.textColor()), req.fontFamily(),
                req.fontSize(), req.letterSpacing(), req.lineHeight(), req.paragraphSpacing(), req.editorWidth());
        return repository.saveAndFlush(settings);
    }

    private static String normalize(String color) {
        return color == null ? null : color.toUpperCase();
    }
}
