package com.chatai.config;

import com.chatai.entity.Role;
import com.chatai.entity.User;
import com.chatai.entity.ModelManagement;
import com.chatai.repository.RoleRepository;
import com.chatai.repository.UserRepository;
import com.chatai.repository.ModelManagementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {
    
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final ModelManagementRepository modelManagementRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Value("${app.default-admin.username}")
    private String adminUsername;
    
    @Value("${app.default-admin.password}")
    private String adminPassword;
    
    @Value("${app.default-admin.email}")
    private String adminEmail;
    
    @Override
    public void run(String... args) throws Exception {
        initializeRoles();
        initializeAdminUser();
        initializeDefaultModels();
    }
    
    private void initializeRoles() {
        // Create USER role if not exists
        if (!roleRepository.findByName(Role.RoleName.USER).isPresent()) {
            Role userRole = new Role();
            userRole.setName(Role.RoleName.USER);
            roleRepository.save(userRole);
            log.info("Created USER role");
        }
        
        // Create ADMIN role if not exists
        if (!roleRepository.findByName(Role.RoleName.ADMIN).isPresent()) {
            Role adminRole = new Role();
            adminRole.setName(Role.RoleName.ADMIN);
            roleRepository.save(adminRole);
            log.info("Created ADMIN role");
        }
    }
    
    private void initializeAdminUser() {
        // Check if admin user exists
        Optional<User> existingAdmin = userRepository.findByUsername(adminUsername);
        
        if (!existingAdmin.isPresent()) {
            // Create new admin user
            Role adminRole = roleRepository.findByName(Role.RoleName.ADMIN)
                    .orElseThrow(() -> new RuntimeException("Admin role not found"));
            Role userRole = roleRepository.findByName(Role.RoleName.USER)
                    .orElseThrow(() -> new RuntimeException("User role not found"));
            
            Set<Role> roles = new HashSet<>();
            roles.add(adminRole);
            roles.add(userRole);
            
            User admin = User.builder()
                    .username(adminUsername)
                    .email(adminEmail)
                    .fullName("System Administrator")
                    .password(passwordEncoder.encode(adminPassword))
                    .isActive(true)
                    .requestsUsedToday(0)
                    .dailyRequestLimit(-1) // Unlimited for admin
                    .lastRequestReset(LocalDateTime.now())
                    .roles(roles)
                    .build();
            
            userRepository.save(admin);
            log.info("Created default admin user: {}", adminUsername);
        } else {
            // Check if existing admin user has required roles
            User admin = existingAdmin.get();
            Role adminRole = roleRepository.findByName(Role.RoleName.ADMIN)
                    .orElseThrow(() -> new RuntimeException("Admin role not found"));
            Role userRole = roleRepository.findByName(Role.RoleName.USER)
                    .orElseThrow(() -> new RuntimeException("User role not found"));
            
            boolean hasAdminRole = admin.getRoles().stream()
                    .anyMatch(role -> role.getName() == Role.RoleName.ADMIN);
            boolean hasUserRole = admin.getRoles().stream()
                    .anyMatch(role -> role.getName() == Role.RoleName.USER);
            
            if (!hasAdminRole || !hasUserRole) {
                Set<Role> roles = new HashSet<>(admin.getRoles());
                if (!hasAdminRole) {
                    roles.add(adminRole);
                }
                if (!hasUserRole) {
                    roles.add(userRole);
                }
                admin.setRoles(roles);
                userRepository.save(admin);
                log.info("Updated admin user roles: {}", adminUsername);
            } else {
                log.info("Admin user already exists with correct roles: {}", adminUsername);
            }
        }
    }
    
    private void initializeDefaultModels() {
        log.info("Checking and initializing models...");
        
        // Create comprehensive list of all models (đồng bộ với database migration)
        String[][] modelData = {
            {"llama-3.1-8b-instant", "Llama 3.1 8B Instant", "Fastest model, good for quick responses", "Llama", "true", "100"},
            {"llama-3.3-70b-versatile", "Llama 3.3 70B Versatile", "Latest Llama model, most capable", "Llama", "true", "90"},
            {"gemma2-9b-it", "Gemma2 9B", "Google's Gemma2 model, 9B parameters", "Google", "true", "80"},
            {"deepseek-r1-distill-llama-70b", "DeepSeek R1 Distill", "DeepSeek's distilled model based on Llama 70B", "DeepSeek", "true", "70"},
            {"meta-llama/llama-4-maverick-17b-128e-instruct", "Llama 4 Maverick 17B", "Meta's latest Llama 4 model", "Llama", "true", "60"},
            {"meta-llama/llama-4-scout-17b-16e-instruct", "Llama 4 Scout 17B", "Meta's Scout model for instruction following", "Llama", "true", "50"},
            {"qwen/qwen3-32b", "Qwen 3 32B", "Alibaba's Qwen model, 32B parameters", "Qwen", "true", "40"},
            {"moonshotai/kimi-k2-instruct", "Kimi K2 Instruct", "Moonshot AI's Kimi model", "Kimi", "true", "30"},
            {"groq/compound", "Compound Beta", "Compound AI's beta model", "Compound", "true", "20"},
            {"groq/compound-mini", "Compound Beta Mini", "Compound AI's smaller beta model", "Compound", "true", "10"},
            {"openai/gpt-oss-20b", "GPT-OSS 20B", "OpenAI's open source model, 20B parameters", "OpenAI", "true", "5"},
            {"openai/gpt-oss-120b", "GPT-OSS 120B", "OpenAI's open source model, 120B parameters", "OpenAI", "true", "1"}
        };
        
        int created = 0;
        int existing = 0;
        
        for (String[] data : modelData) {
            String modelId = data[0];
            
            // Check if model already exists
            if (!modelManagementRepository.findByModelId(modelId).isPresent()) {
                ModelManagement model = new ModelManagement();
                model.setModelId(modelId);
                model.setModelName(data[1]);
                model.setDescription(data[2]);
                model.setCategory(data[3]);
                model.setIsEnabled(Boolean.parseBoolean(data[4]));
                model.setIsDefault(modelId.equals("llama-3.1-8b-instant")); // Set first as default
                model.setPriority(Integer.parseInt(data[5]));
                model.setGroqModelId(modelId);
                
                modelManagementRepository.save(model);
                log.info("Created model: {} ({})", model.getModelName(), model.getModelId());
                created++;
            } else {
                existing++;
            }
        }
        
        log.info("Model initialization completed: {} created, {} already existed", created, existing);
    }
}
