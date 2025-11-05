package com.userprofile.user.controller;

import com.userprofile.common.response.PageResult;
import com.userprofile.common.response.Result;
import com.userprofile.user.dto.UserDTO;
import com.userprofile.user.entity.User;
import com.userprofile.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户控制器
 */
@Tag(name = "用户管理", description = "用户管理相关接口")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "创建用户")
    @PostMapping
    public Result<User> createUser(@Valid @RequestBody UserDTO userDTO) {
        User user = userService.createUser(userDTO);
        return Result.success("用户创建成功", user);
    }

    @Operation(summary = "更新用户")
    @PutMapping("/{id}")
    public Result<User> updateUser(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @Valid @RequestBody UserDTO userDTO) {
        User user = userService.updateUser(id, userDTO);
        return Result.success("用户更新成功", user);
    }

    @Operation(summary = "根据ID查询用户")
    @GetMapping("/{id}")
    public Result<User> getUserById(@Parameter(description = "用户ID") @PathVariable Long id) {
        User user = userService.getUserById(id);
        return Result.success(user);
    }

    @Operation(summary = "根据用户名查询用户")
    @GetMapping("/username/{username}")
    public Result<User> getUserByUsername(@Parameter(description = "用户名") @PathVariable String username) {
        User user = userService.getUserByUsername(username);
        return Result.success(user);
    }

    @Operation(summary = "分页查询用户列表")
    @GetMapping
    public Result<PageResult<User>> getUserList(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size) {
        PageResult<User> pageResult = userService.getUserList(page, size);
        return Result.success(pageResult);
    }

    @Operation(summary = "获取所有用户")
    @GetMapping("/all")
    public Result<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return Result.success(users);
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@Parameter(description = "用户ID") @PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success("用户删除成功", null);
    }
}
