package com.wbw.user;

import com.wbw.common.ApiException;
import com.wbw.common.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** 구글 로그인 시 사용자를 생성하거나 프로필을 최신으로 갱신한다. */
    @Transactional
    public User upsertGoogleUser(String sub, String email, String name, String pictureUrl) {
        return userRepository.findByGoogleSub(sub)
                .map(user -> {
                    user.updateProfile(email, name, pictureUrl);
                    return user;
                })
                .orElseGet(() -> userRepository.save(new User(sub, email, name, pictureUrl)));
    }

    @Transactional(readOnly = true)
    public User get(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
    }
}
