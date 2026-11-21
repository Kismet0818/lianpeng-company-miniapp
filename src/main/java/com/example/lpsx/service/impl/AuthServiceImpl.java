package com.example.lpsx.service.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.lpsx.common.exception.BusinessException;
import com.example.lpsx.entity.User;
import com.example.lpsx.mapper.UserMapper;
import com.example.lpsx.service.AuthService;
import com.example.lpsx.utils.JwtUtils;
import com.example.lpsx.utils.WxDecryptUtils;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

/**
 * 认证服务实现
 */
@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private static final String ACCESS_TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token";
    private static final String PHONE_NUMBER_URL = "https://api.weixin.qq.com/wxa/business/getuserphonenumber";

    @Resource
    private UserMapper userMapper;

    @Resource
    private JwtUtils jwtUtils;

    @Value("${wechat.appid}")
    private String appId;

    @Value("${wechat.secret}")
    private String appSecret;

    /** 服务端缓存 sessionKey，key=userId, value=sessionKey */
    private final Map<Integer, String> sessionKeyCache = new ConcurrentHashMap<>();
    private volatile String accessToken;
    private volatile long accessTokenExpiresAt;

    @Override
    public String wxLogin(String code) {
        try {
        // 1. 调用微信 jscode2session 接口
        String url = "https://api.weixin.qq.com/sns/jscode2session";
        Map<String, Object> params = new HashMap<>();
        params.put("appid", appId);
        params.put("secret", appSecret);
        params.put("js_code", code);
        params.put("grant_type", "authorization_code");

        String response = HttpUtil.get(url, params);
        JSONObject result = JSONUtil.parseObj(response);
        log.info("微信登录接口调用完成，errcode={}", result.getInt("errcode", 0));

        // 2. 检查微信返回结果
        if (result.containsKey("errcode") && result.getInt("errcode") != 0) {
            throw new BusinessException("微信登录失败: " + result.getStr("errmsg"));
        }

        String openid = result.getStr("openid");
        String sessionKey = result.getStr("session_key");
        String unionid = result.getStr("unionid");

        // 3. 查库，若无则注册
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getOpenid, openid));
        if (user == null) {
            user = new User();
            user.setOpenid(openid);
            user.setNickname("微信用户" + ThreadLocalRandom.current().nextInt(1000, 10000));
            user.setUnionid(unionid);
            user.setLastLoginTime(LocalDateTime.now());
            user.setLastActiveTime(LocalDateTime.now());
            user.setLoginCount(1);
            user.setStatus(1);
            userMapper.insert(user);
            log.info("新用户注册: userId={}, openid={}", user.getId(), openid);
        }
        else {
            if (Integer.valueOf(0).equals(user.getStatus())) {
                throw new BusinessException(403, "账号已被停用");
            }
            LambdaUpdateWrapper<User> update = new LambdaUpdateWrapper<User>()
                    .eq(User::getId, user.getId())
                    .set(User::getLastLoginTime, LocalDateTime.now())
                    .set(User::getLastActiveTime, LocalDateTime.now())
                    .setSql("login_count = COALESCE(login_count, 0) + 1");
            if (unionid != null && !unionid.isBlank()) update.set(User::getUnionid, unionid);
            userMapper.update(null, update);
        }

        // 4. 缓存 sessionKey（用于后续手机号解密）
        sessionKeyCache.put(user.getId(), sessionKey);

        // 5. 生成 JWT
        return jwtUtils.createToken(user.getId());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("微信登录异常", e);
            throw new BusinessException("登录失败: " + e.getMessage());
        }
    }

    @Override
    public void decryptPhone(String encryptedData, String iv, Integer userId) {
        // 从缓存取 sessionKey
        String sessionKey = sessionKeyCache.get(userId);
        if (sessionKey == null) {
            throw new RuntimeException("sessionKey 不存在，请重新登录");
        }

        // 解密获取手机号
        String decryptedJson = WxDecryptUtils.decryptPhone(encryptedData, iv, sessionKey);
        String phone = WxDecryptUtils.extractPurePhone(decryptedJson);

        // 更新 user 表
        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setPhone(phone);
            userMapper.updateById(user);
        }
    }

    @Override
    public String bindPhoneByCode(String code, Integer userId) {
        if (code == null || code.isBlank()) throw new BusinessException("手机号授权凭证不能为空");
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(401, "用户不存在，请重新登录");

        String response = HttpUtil.post(PHONE_NUMBER_URL + "?access_token=" + getAccessToken(),
                JSONUtil.createObj().set("code", code.trim()).toString());
        JSONObject result = JSONUtil.parseObj(response);
        int errcode = result.getInt("errcode", -1);
        if (errcode != 0) {
            log.warn("微信手机号接口失败: errcode={}, errmsg={}", errcode, result.getStr("errmsg"));
            throw new BusinessException("手机号授权失败，请重新点击授权");
        }
        String phone = result.getByPath("phone_info.purePhoneNumber", String.class);
        if (phone == null || !phone.matches("^1\\d{10}$")) {
            throw new BusinessException("微信未返回有效的大陆手机号");
        }
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getPhone, phone)
                .set(User::getUpdateTime, LocalDateTime.now()));
        return phone;
    }

    private String getAccessToken() {
        long now = System.currentTimeMillis();
        if (accessToken != null && now < accessTokenExpiresAt) return accessToken;
        synchronized (this) {
            now = System.currentTimeMillis();
            if (accessToken != null && now < accessTokenExpiresAt) return accessToken;
            Map<String, Object> params = new HashMap<>();
            params.put("grant_type", "client_credential");
            params.put("appid", appId);
            params.put("secret", appSecret);
            JSONObject result = JSONUtil.parseObj(HttpUtil.get(ACCESS_TOKEN_URL, params));
            String token = result.getStr("access_token");
            if (token == null || token.isBlank()) {
                log.warn("微信 access_token 获取失败: errcode={}, errmsg={}", result.getInt("errcode"), result.getStr("errmsg"));
                throw new BusinessException("微信服务暂时不可用，请稍后重试");
            }
            int expiresIn = result.getInt("expires_in", 7200);
            accessToken = token;
            accessTokenExpiresAt = now + Math.max(60, expiresIn - 300) * 1000L;
            return token;
        }
    }
}
