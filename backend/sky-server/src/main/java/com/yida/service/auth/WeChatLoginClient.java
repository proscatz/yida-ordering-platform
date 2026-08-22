package com.yida.service.auth;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.yida.properties.WeChatProperties;
import com.yida.utils.HttpClientUtil;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class WeChatLoginClient {
    private static final String WX_LOGIN = "https://api.weixin.qq.com/sns/jscode2session";
    private final WeChatProperties properties;

    public WeChatLoginClient(WeChatProperties properties) { this.properties = properties; }

    public String exchangeCode(String code) {
        Map<String, String> params = new HashMap<>();
        params.put("appid", properties.getAppid());
        params.put("secret", properties.getSecret());
        params.put("js_code", code);
        params.put("grant_type", "authorization_code");
        JSONObject response = JSON.parseObject(HttpClientUtil.doGet(WX_LOGIN, params));
        return response == null ? null : response.getString("openid");
    }
}