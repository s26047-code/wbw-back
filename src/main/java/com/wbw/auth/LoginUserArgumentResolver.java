package com.wbw.auth;

import com.wbw.common.ApiException;
import com.wbw.common.ErrorCode;
import com.wbw.user.UserRepository;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class LoginUserArgumentResolver implements HandlerMethodArgumentResolver {

    private final UserRepository userRepository;

    public LoginUserArgumentResolver(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(LoginUser.class)
                && Long.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String sub = extractSubject(auth);
        if (sub == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
        return userRepository.findByGoogleSub(sub)
                .map(u -> u.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
    }

    static String extractSubject(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof OidcUser oidc) {
            return oidc.getSubject();
        }
        if (principal instanceof OAuth2User oauth) {
            Object sub = oauth.getAttribute("sub");
            return sub != null ? sub.toString() : null;
        }
        return null;
    }
}
