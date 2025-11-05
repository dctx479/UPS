package com.userprofile.user.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * 用户创建/更新DTO
 */
@Data
public class UserDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50个字符之间")
    private String username;

    @NotBlank(message = "密码不能为空", groups = Create.class)
    @Size(min = 8, max = 50, message = "密码长度必须在8-50个字符之间")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
        message = "密码必须包含至少1个大写字母、1个小写字母、1个数字和1个特殊字符(@$!%*?&)",
        groups = Create.class
    )
    private String password;

    @Size(max = 50, message = "姓名长度不能超过50个字符")
    private String name;

    @Min(value = 0, message = "年龄不能小于0")
    @Max(value = 150, message = "年龄不能大于150")
    private Integer age;

    @Min(value = 0, message = "性别值无效")
    @Max(value = 2, message = "性别值无效")
    private Integer gender;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号码格式不正确")
    private String phone;

    @Email(message = "邮箱格式不正确")
    private String email;

    private String region;

    private String zodiacSign;

    private String occupation;

    private String incomeLevel;

    @DecimalMin(value = "0.0", message = "资产负债比不能为负数")
    @DecimalMax(value = "100.0", message = "资产负债比不能超过100")
    private Double debtToAssetRatio;

    /**
     * 验证组：创建时
     */
    public interface Create {}

    /**
     * 验证组：更新时
     */
    public interface Update {}
}
