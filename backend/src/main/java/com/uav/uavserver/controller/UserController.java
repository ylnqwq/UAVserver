package com.uav.uavserver.controller;

import com.uav.uavserver.domain.UserRole;
import com.uav.uavserver.dto.UserDto;
import com.uav.uavserver.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户管理接口。
 *   GET    /api/users                      用户列表（仅管理员）
 *   GET    /api/users/role/{role}          按角色查询，例如查飞行员：/api/users/role/PILOT
 *   GET    /api/users/me                   当前登录用户
 *   GET    /api/users/{id}                 用户详情
 *   POST   /api/users                      新增用户（仅管理员）
 *   PUT    /api/users/{id}                 修改用户（仅管理员）
 *   DELETE /api/users/{id}                 删除用户（仅管理员）
 *   POST   /api/users/{id}/reset-password  重置密码（仅管理员）
 *
 * 所有响应都不含密码字段。
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDto.Response>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/role/{role}")
    public ResponseEntity<List<UserDto.Response>> getUsersByRole(@PathVariable String role) {
        return ResponseEntity.ok(userService.getUsersByRole(parseRole(role)));
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto.Response> getCurrentUser(Authentication authentication) {
        return ResponseEntity.ok(userService.getUserByUsername(authentication.getName()));
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<UserDto.Response> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUser(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto.Response> createUser(@Valid @RequestBody UserDto.CreateRequest request) {
        return ResponseEntity.ok(userService.createUser(request));
    }

    @PutMapping("/{id:\\d+}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto.Response> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserDto.UpdateRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @DeleteMapping("/{id:\\d+}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id:\\d+}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> resetPassword(
            @PathVariable Long id,
            @Valid @RequestBody UserDto.ResetPasswordRequest request) {
        userService.resetPassword(id, request.getNewPassword());
        return ResponseEntity.ok().build();
    }

    private UserRole parseRole(String role) {
        try {
            return UserRole.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            String valid = Arrays.stream(UserRole.values()).map(Enum::name).collect(Collectors.joining(", "));
            throw new IllegalArgumentException("无效的用户角色: " + role + "，可选值: " + valid);
        }
    }
}
