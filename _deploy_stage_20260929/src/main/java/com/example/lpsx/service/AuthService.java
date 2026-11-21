package com.example.lpsx.service;

/**
 * 认证服务 — 微信登录 & 手机号绑定
 */
public interface AuthService {

    /**
     * 微信登录
     *
     * @param code 微信临时 code
     * @return JWT Token
     */
    String wxLogin(String code);

    /**
     * 解密并绑定手机号
     *
     * @param encryptedData 加密数据
     * @param iv            初始向量
     * @param userId        当前用户 ID
     */
    void decryptPhone(String encryptedData, String iv, Integer userId);

    /** 使用微信手机号授权的一次性 code 绑定当前用户手机号。 */
    String bindPhoneByCode(String code, Integer userId);
}
