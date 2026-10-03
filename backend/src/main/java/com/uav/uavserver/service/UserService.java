package com.uav.uavserver.service;

import com.uav.uavserver.domain.User;
import com.uav.uavserver.domain.UserRole;
import com.uav.uavserver.domain.UserStatus;
import com.uav.uavserver.dto.UserDto;
import com.uav.uavserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户服务。
 * 返回前统一转换成 UserDto.Response，避免把密码哈希暴露出去。
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public List<UserDto.Response> getAllUsers() {
        return userRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<UserDto.Response> getUsersByRole(UserRole role) {
        return userRepository.findByRole(role).stream().map(this::toResponse).collect(Collectors.toList());
    }

    public UserDto.Response getUser(Long id) {
        return toResponse(findUser(id));
    }

    public UserDto.Response getUserByUsername(String username) {
        return toResponse(userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + username)));
    }

    @Transactional
    public UserDto.Response createUser(UserDto.CreateRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("用户名已存在: " + request.getUsername());
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()
                && userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("邮箱已被注册: " + request.getEmail());
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setRole(request.getRole());
        user.setStatus(request.getStatus() != null ? request.getStatus() : UserStatus.ACTIVE);
        user.setOrganization(request.getOrganization());

        // 人员档案字段
        user.setIdCard(request.getIdCard());
        user.setCertNo(request.getCertNo());
        user.setCertExpiry(request.getCertExpiry());
        user.setJoinDate(request.getJoinDate() != null ? request.getJoinDate() : LocalDate.now());
        user.setFlightHours(request.getFlightHours() != null ? request.getFlightHours() : 0d);
        user.setTodayTasks(request.getTodayTasks() != null ? request.getTodayTasks() : 0);
        user.setTotalTasks(request.getTotalTasks() != null ? request.getTotalTasks() : 0);
        user.setTraining(request.getTraining());
        user.setRemark(request.getRemark());

        return toResponse(userRepository.save(user));
    }

    @Transactional
    public UserDto.Response updateUser(Long id, UserDto.UpdateRequest request) {
        User user = findUser(id);

        if (request.getRealName() != null) user.setRealName(request.getRealName());
        if (request.getEmail() != null) user.setEmail(request.getEmail());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getRole() != null) user.setRole(request.getRole());
        if (request.getStatus() != null) user.setStatus(request.getStatus());
        if (request.getOrganization() != null) user.setOrganization(request.getOrganization());

        // 人员档案字段
        if (request.getIdCard() != null) user.setIdCard(request.getIdCard());
        if (request.getCertNo() != null) user.setCertNo(request.getCertNo());
        if (request.getCertExpiry() != null) user.setCertExpiry(request.getCertExpiry());
        if (request.getJoinDate() != null) user.setJoinDate(request.getJoinDate());
        if (request.getFlightHours() != null) user.setFlightHours(request.getFlightHours());
        if (request.getTodayTasks() != null) user.setTodayTasks(request.getTodayTasks());
        if (request.getTotalTasks() != null) user.setTotalTasks(request.getTotalTasks());
        if (request.getTraining() != null) user.setTraining(request.getTraining());
        if (request.getRemark() != null) user.setRemark(request.getRemark());

        return toResponse(userRepository.save(user));
    }

    @Transactional
    public void resetPassword(Long id, String newPassword) {
        User user = findUser(id);
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = findUser(id);
        if (user.getUsername().equals("admin")) {
            throw new RuntimeException("内置管理员账号不允许删除");
        }
        userRepository.delete(user);
    }

    /** 各角色用户数量（角色权限页面用） */
    public List<RoleUserCount> countUsersByRole() {
        List<User> all = userRepository.findAll();
        return List.of(UserRole.values()).stream()
                .map(role -> new RoleUserCount(
                        role,
                        all.stream().filter(u -> u.getRole() == role).count()))
                .filter(c -> c.count() > 0)
                .collect(Collectors.toList());
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在: id=" + id));
    }

    private UserDto.Response toResponse(User user) {
        UserDto.Response response = new UserDto.Response();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setRealName(user.getRealName());
        response.setRole(user.getRole());
        response.setStatus(user.getStatus());
        response.setOrganization(user.getOrganization());
        response.setCreatedAt(user.getCreatedAt());
        response.setLastLoginAt(user.getLastLoginAt());

        response.setIdCard(user.getIdCard());
        response.setCertNo(user.getCertNo());
        response.setCertExpiry(user.getCertExpiry());
        response.setJoinDate(user.getJoinDate());
        response.setFlightHours(user.getFlightHours());
        response.setTodayTasks(user.getTodayTasks());
        response.setTotalTasks(user.getTotalTasks());
        response.setTraining(user.getTraining());
        response.setRemark(user.getRemark());
        return response;
    }

    public record RoleUserCount(UserRole role, long count) {
    }
}
