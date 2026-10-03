package com.uav.uavserver.config;

import com.uav.uavserver.domain.User;
import com.uav.uavserver.domain.UserRole;
import com.uav.uavserver.domain.UserStatus;
import com.uav.uavserver.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // 创建默认管理员账户
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setEmail("admin@uavserver.com");
            admin.setRealName("系统管理员");
            admin.setRole(UserRole.ADMIN);
            admin.setStatus(UserStatus.ACTIVE);
            admin.setOrganization("系统");
            userRepository.save(admin);
            System.out.println("默认管理员账户已创建: admin / admin123");
        }

        // 创建默认监管员账户
        if (!userRepository.existsByUsername("supervisor")) {
            User supervisor = new User();
            supervisor.setUsername("supervisor");
            supervisor.setPassword(passwordEncoder.encode("supervisor123"));
            supervisor.setEmail("supervisor@uavserver.com");
            supervisor.setRealName("监管人员");
            supervisor.setRole(UserRole.SUPERVISOR);
            supervisor.setStatus(UserStatus.ACTIVE);
            supervisor.setOrganization("监管部门");
            userRepository.save(supervisor);
            System.out.println("默认监管员账户已创建: supervisor / supervisor123");
        }

        // 创建默认飞行员账户
        if (!userRepository.existsByUsername("pilot")) {
            User pilot = new User();
            pilot.setUsername("pilot");
            pilot.setPassword(passwordEncoder.encode("pilot123"));
            pilot.setEmail("pilot@uavserver.com");
            pilot.setRealName("飞行员");
            pilot.setRole(UserRole.PILOT);
            pilot.setStatus(UserStatus.ACTIVE);
            pilot.setOrganization("飞行部门");
            userRepository.save(pilot);
            System.out.println("默认飞行员账户已创建: pilot / pilot123");
        }
    }
}
