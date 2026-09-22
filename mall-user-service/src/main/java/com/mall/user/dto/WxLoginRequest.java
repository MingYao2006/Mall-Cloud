package com.mall.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 微信小程序登录请求
 */
@Data
@Schema(description = "微信小程序登录请求")
public class WxLoginRequest {

    /**
     * 临时登录凭证，由小程序端 uni.login() 获取，5 分钟内有效且只能用一次
     */
    @Schema(description = "uni.login 获取的临时登录凭证 code")
    @NotBlank(message = "code 不能为空")
    private String code;

    /**
     * 微信昵称（可选，用于首次注册时填充资料）
     */
    @Schema(description = "微信昵称")
    private String nickname;

    /**
     * 微信头像地址（可选）
     */
    @Schema(description = "微信头像地址")
    private String avatarUrl;
}
