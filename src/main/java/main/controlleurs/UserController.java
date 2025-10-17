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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static main.hebergeurs.infinityfree.InfinityFreeUploader.uploadFile;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;



    // ===========================
    // ENDPOINTS PUBLICS
    // ===========================


    @PostMapping("/register")
    public ResponseEntity<String> registerUser(
            @RequestParam("username") String username,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam("file") MultipartFile file) {

        String uploadedFileUrl = null;

        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("Le fichier photo est requis");
            }

            String originalName = file.getOriginalFilename();
            if (originalName == null || originalName.isEmpty()) {
                return ResponseEntity.badRequest().body("Nom de fichier invalide");
            }

            // Créer un fichier temporaire en mémoire
            Path tempFile = Files.createTempFile("upload_", "_" + originalName);

            try {
                // Sauvegarde temporaire très rapide
                file.transferTo(tempFile);

                // Générer un nom de fichier unique
                String tempFileName = System.currentTimeMillis() + "_" + originalName;
                String remoteFilePath = "photos/" + tempFileName;

                // Upload vers InfinityFree
                uploadedFileUrl = uploadFile(tempFile.toString(), remoteFilePath);

                // Vérifier si l'upload a réussi
                if (uploadedFileUrl == null) {
                    return ResponseEntity.status(500)
                            .body("Erreur lors de l'upload de la photo vers le serveur");
                }

                // Crée l'utilisateur
                User user = userService.createUser(username, email, password, uploadedFileUrl);

                String responseText = "Nom utilisateur : " + user.getUsername() +
                        "\nEmail : " + email +
                        "\nMot de passe : " + password +
                        "\nURL de la photo : " + uploadedFileUrl +
                        "\nRôle : " + user.getRole();

                return ResponseEntity.ok(responseText);

            } finally {
                // Nettoyage IMMÉDIAT du fichier temporaire
                Files.deleteIfExists(tempFile);
            }

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

        String uploadedFileUrl = null;

        try {
            // Vérifications de base
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("Le fichier photo est requis");
            }

            String originalName = file.getOriginalFilename();
            if (originalName == null || originalName.isEmpty()) {
                return ResponseEntity.badRequest().body("Nom de fichier invalide");
            }

            // Générer un nom de fichier unique
            String tempFileName = System.currentTimeMillis() + "_" + originalName;
            String remoteFilePath = "photos/" + tempFileName;

            // Upload DIRECT vers InfinityFree sans sauvegarde locale
            uploadedFileUrl = uploadFile( tempFileName, remoteFilePath);

            // Vérifier si l'upload a réussi
            if (uploadedFileUrl == null) {
                return ResponseEntity.status(500)
                        .body("Erreur lors de l'upload de la photo vers le serveur");
            }

            // Crée l'administrateur avec l'URL de la photo depuis InfinityFree
            User user = userService.createUserWithRole(username, email, password, "ADMIN", uploadedFileUrl);

            return ResponseEntity.ok("Administrateur créé avec succès : " + user.getUsername()
                    + " (Rôle: " + user.getRole() + ")"
                    + "\nURL de la photo : " + uploadedFileUrl);

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
