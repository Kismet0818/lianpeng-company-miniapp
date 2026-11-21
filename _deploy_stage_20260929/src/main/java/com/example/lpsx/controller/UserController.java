package com.example.lpsx.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.lpsx.common.result.Result;
import com.example.lpsx.entity.CustomerLead;
import com.example.lpsx.entity.User;
import com.example.lpsx.service.UserService;
import com.example.lpsx.service.AuthService;

import jakarta.annotation.Resource;

/**
 * 用户接口 - 需 JWT 鉴权
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Resource
    private UserService userService;
    @Resource
    private AuthService authService;
    @GetMapping("/lead-history")
    public Result<List<CustomerLead>> leadHistory(@RequestAttribute("userId") Integer userId) {
        return Result.success(userService.getLeadHistory(userId));
    }

    @GetMapping("/profile")
    public Result<User> profile(@RequestAttribute("userId") Integer userId) {
        return Result.success(userService.getProfile(userId));
    }

    @PutMapping("/profile")
    public Result<Void> updateProfile(@RequestBody Map<String, String> body,
                                      @RequestAttribute("userId") Integer userId) {
        userService.updateProfile(userId, body.get("nickname"), body.get("avatarUrl"));
        return Result.success();
    }

    /** 微信手机号快捷验证：前端传 getPhoneNumber 回调中的一次性 code。 */
    @PostMapping("/phone")
    public Result<Map<String, String>> bindPhone(@RequestBody Map<String, String> body,
                                                  @RequestAttribute("userId") Integer userId) {
        String code = body == null ? null : body.get("code");
        String phone = authService.bindPhoneByCode(code, userId);
        return Result.success(Map.of("phone", phone));
    }
}
