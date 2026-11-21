package com.example.lpsx.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.lpsx.common.result.Result;
import com.example.lpsx.service.FavoriteService;
import com.example.lpsx.vo.FavoriteVO;

import jakarta.annotation.Resource;

/**
 * 收藏接口 - 需 JWT 鉴权
 */
@RestController
@RequestMapping("/api/favorite")
public class FavoriteController {

    @Resource
    private FavoriteService favoriteService;

    @PostMapping("/toggle")
    public Result<Void> toggle(@RequestBody Map<String, Object> body,
                               @RequestAttribute("userId") Integer userId) {
        Integer schoolId = number(body == null ? null :
                (body.containsKey("schoolId") ? body.get("schoolId") : body.get("school_id")));
        String schoolSource = body == null || body.get("schoolSource") == null
                ? "international" : String.valueOf(body.get("schoolSource"));
        if (schoolId == null || schoolId <= 0) {
            return Result.error("学校编号不正确");
        }
        favoriteService.toggle(userId, schoolSource, schoolId);
        return Result.success();
    }

    @GetMapping("/list")
    public Result<List<FavoriteVO>> list(@RequestAttribute("userId") Integer userId) {
        return Result.success(favoriteService.list(userId));
    }

    private Integer number(Object value) {
        try {
            return value == null ? null : Integer.valueOf(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
