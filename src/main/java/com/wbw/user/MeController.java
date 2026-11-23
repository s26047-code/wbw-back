package com.wbw.user;

import com.wbw.auth.LoginUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class MeController {

    private final UserService userService;

    public MeController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public MeResponse me(@LoginUser Long userId) {
        User user = userService.get(userId);
        return new MeResponse(user.getId(), user.getEmail(), user.getName(), user.getPictureUrl());
    }

    public record MeResponse(Long id, String email, String name, String pictureUrl) {
    }
}
