package main.services;

import main.modeles.User;
import main.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ✅ Méthode : crée un utilisateur avec rôle USER par défaut
    public User createUser(String username, String email, String password, String photoUrl) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username déjà utilisé: " + username);
        }
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email déjà utilisé: " + email);
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole("USER"); // rôle par défaut
        user.setPhotoUrl(photoUrl); // ✅ on ajoute la photo

        return userRepository.save(user);
    }

    // ✅ Méthode : crée un utilisateur avec un rôle spécifique
    public User createUserWithRole(String username, String email, String password, String role, String photoUrl) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username déjà utilisé: " + username);
        }
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email déjà utilisé: " + email);
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role.toUpperCase()); // force en majuscules
        user.setPhotoUrl(photoUrl); // ✅ on ajoute la photo

        return userRepository.save(user);
    }

    // Liste tous les utilisateurs
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Recherche par username
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }


    // Vérifie un mot de passe
    public boolean validatePassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    public Optional<User> findById(Long userId) {
        return userRepository.findById(userId);
    }

    // Dans UserService.java - Ajouter cette méthode
    public User updateUser(User user) {
        // Validation basique
        if (user == null) {
            throw new RuntimeException("L'utilisateur ne peut pas être null");
        }

        if (user.getId() == null) {
            throw new RuntimeException("Impossible de mettre à jour un utilisateur sans ID");
        }

        // Vérifier que l'utilisateur existe
        if (!userRepository.existsById(user.getId())) {
            throw new RuntimeException("Utilisateur non trouvé avec l'ID: " + user.getId());
        }

        // Sauvegarder et retourner l'utilisateur mis à jour
        return userRepository.save(user);
    }
}
