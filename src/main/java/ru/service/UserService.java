package ru.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.entity.User;
import ru.entity.UserRole;
import ru.entity.dto.UserForm;
import ru.repository.UserRepository;

@Service
@Transactional
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User findByUsername(String username) {
        return userRepository.findByUsernameIgnoreCase(username).orElse(null);
    }

    public boolean existsByUsernameAndPassword(String username, String password) {
        User user = userRepository.findByUsernameIgnoreCase(username).orElse(null);
        return user != null && passwordEncoder.matches(password, user.getPassword());
    }

    public void save(UserForm form) {
        User user = new User(form.username(), passwordEncoder.encode(form.password()), UserRole.USER);
        userRepository.save(user);
    }
}
