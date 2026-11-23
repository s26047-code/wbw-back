package com.wbw.auth;

import com.wbw.user.UserService;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

/** 구글 로그인 성공 시 사용자 정보를 DB에 저장/갱신한다. */
@Service
public class GoogleOidcUserService extends OidcUserService {

    private final UserService userService;

    public GoogleOidcUserService(UserService userService) {
        this.userService = userService;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);
        String sub = oidcUser.getSubject();
        String email = oidcUser.getEmail();
        if (sub == null || email == null) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_user_info"), "구글 계정 정보가 부족합니다.");
        }
        userService.upsertGoogleUser(sub, email, oidcUser.getFullName(), oidcUser.getPicture());
        return oidcUser;
    }
}
