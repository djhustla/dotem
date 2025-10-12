package main.controlleurs;

import main.modeles.User;
import main.repository.UserRepository;
import main.security.JwtUtil;
import main.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Value("${file.upload-dir}")
    private String uploadDir; // dossier configurable depuis properties

    // ===========================
    // ENDPOINTS PUBLICS
    // ===========================



    @PostMapping("/register")
    public ResponseEntity<String> registerUser(
            @RequestParam("username") String username,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam("file") MultipartFile file) {

        try {
            // Utilise le chemin configuré dans application.properties
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                if (!created) {
                    return ResponseEntity.status(500)
                            .body("Impossible de créer le dossier: " + uploadDir);
                }
            }

            // Nom unique du fichier
            String originalName = file.getOriginalFilename();
            if (originalName == null || originalName.isEmpty()) {
                return ResponseEntity.badRequest().body("Nom de fichier invalide");
            }

            String fileName = System.currentTimeMillis() + "_" + originalName;
            String filePath = uploadDir + "/" + fileName;

            // Sauvegarde du fichier
            file.transferTo(new File(filePath));

            // Crée l'utilisateur - attention a l adresse de la photo

            filePath = "/photos/" + fileName;
            User user = userService.createUser(username, email, password, filePath);

            String responseText = "Nom utilisateur : " + user.getUsername() +
                    "\nEmail : " + email +
                    "\nMot de passe : " + password +
                    "\nChemin fichier serveur : " + filePath +
                    "\nRôle : " + user.getRole();

            return ResponseEntity.ok(responseText);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body("Erreur lors de l'inscription : " + e.getMessage());
        }
    }


    // Endpoint pour créer un administrateur avec photo
    @PostMapping("/register-admin")
    public ResponseEntity<String> registerAdmin(
            @RequestParam("username") String username,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam("file") MultipartFile file) {

        try {
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                if (!created) {
                    return ResponseEntity.status(500)
                            .body("Impossible de créer le dossier: " + uploadDir);
                }
            }

            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            String filePath = uploadDir + "/" + fileName;

            file.transferTo(new File(filePath));

            filePath = "/photos/" + fileName;
            User user = userService.createUserWithRole(username, email, password, "ADMIN", filePath);

            return ResponseEntity.ok("Administrateur créé avec succès : " + user.getUsername()
                    + " (Rôle: " + user.getRole() + ")");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body("Erreur : " + e.getMessage());
        }
    }





    @GetMapping("/all")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        users.forEach(user -> user.setPassword("*****"));
        return ResponseEntity.ok(users);
    }

    // ===========================
    // ENDPOINTS AUTHENTIFIÉS
    // ===========================

    @GetMapping("/profile")
    public ResponseEntity<Map<String, String>> getProfile(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String username = jwtUtil.extractUsername(token);

        if (jwtUtil.validateToken(token, username)) {
            return ResponseEntity.ok(Map.of(
                    "username", username,
                    "message", "Bienvenue dans ton profil sécurisé !"
            ));
        } else {
            return ResponseEntity.status(401).body(Map.of(
                    "error", "Token invalide ou expiré"
            ));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser() {
        String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (username == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Utilisateur non authentifié"));
        }

        User user = userService.findByUsername(username).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Utilisateur non trouvé"));
        }

        Map<String, Object> response = Map.of(

                "id",user.getId(),

                "username", user.getUsername(),
                "email", user.getEmail(),
                "role", user.getRole(),
                "createdAt", user.getCreatedAt().toString(),
                "photoUrl", user.getPhotoUrl() // 👈 affichage de la photo
        );

        return ResponseEntity.ok(response);
    }

    // ✅ Upload photo (fichier physique)
    @PostMapping("/{id}/upload-photo")
    public ResponseEntity<String> uploadPhoto(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        try {
            User user = userRepository.findById(id).orElse(null);
            if (user == null) {
                return ResponseEntity.badRequest().body("Utilisateur non trouvé");
            }

            // Crée un dossier "uploads" si inexistant
            File uploadDir = new File("uploads");
            if (!uploadDir.exists()) uploadDir.mkdirs();

            // Nom unique du fichier
            String filePath = "uploads/" + id + "_" + file.getOriginalFilename();
            file.transferTo(new File(filePath));

            // Sauvegarde chemin relatif dans la BDD
            user.setPhotoUrl(filePath);
            userRepository.save(user);

            return ResponseEntity.ok("Photo uploadée avec succès: " + filePath);
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Erreur lors de l'upload: " + e.getMessage());
        }
    }

    // ===========================
    // ENDPOINTS RÉSERVÉS AUX ADMIN
    // ===========================

    @GetMapping("/admin/dashboard")
    public ResponseEntity<Map<String, Object>> adminDashboard() {
        String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        long totalUsers = userService.getAllUsers().size();
        long adminUsers = userService.getAllUsers().stream()
                .filter(user -> "ADMIN".equals(user.getRole()))
                .count();

        Map<String, Object> dashboard = Map.of(
                "message", "Bienvenue dans le tableau de bord administrateur",
                "connectedAdmin", username,
                "totalUsers", totalUsers,
                "adminUsers", adminUsers
        );

        return ResponseEntity.ok(dashboard);
    }

    @DeleteMapping("/admin/delete/{userId}")
    public ResponseEntity<String> deleteUser(@PathVariable Long userId) {
        String adminUsername = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // Logique de suppression (à implémenter)
        return ResponseEntity.ok("Utilisateur avec ID " + userId + " supprimé par l'admin " + adminUsername);
    }

    @GetMapping("/admin/users-details")
    public ResponseEntity<List<User>> getAllUsersWithDetails() {
        List<User> users = userService.getAllUsers();
        users.forEach(user -> user.setPassword("*****"));
        return ResponseEntity.ok(users);
    }
}
