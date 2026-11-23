package com.wbw.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.wbw.auth.LoginUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
@Tag(name = "사용자", description = "로그인한 사용자 정보")
public class MeController {

    private final UserService userService;

    public MeController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "내 정보 조회", description = "로그인하지 않았으면 401")
    @GetMapping
    public MeResponse me(@LoginUser Long userId) {
        User user = userService.get(userId);
        return new MeResponse(user.getId(), user.getEmail(), user.getName(), user.getPictureUrl());
    }

    public record MeResponse(Long id, String email, String name, String pictureUrl) {
    }
}
