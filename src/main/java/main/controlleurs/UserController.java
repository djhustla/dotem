package main.controlleurs;

import main.modeles.MusicPreference;
import main.modeles.MusicType;
import main.modeles.User;
import main.repository.UserRepository;
import main.security.JwtUtil;
import main.services.MusicPreferenceService;
import main.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static main.hebergeurs.github.GitHubFileUploader.uploadFileGitHub;
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

    @Autowired
    private MusicPreferenceService musicPreferenceService;

    ///////////////////////////////////////////////////////////////////
    ///// NOUVEL ENDPOINT PUT POUR LE SON D'ENTRÉE
    ///////////////////////////////////////////////////////////////////
    @PutMapping("/me/son-entree")
    public ResponseEntity<Map<String, Object>> updateSonEntree(@RequestParam("audio") MultipartFile audioFile) {
        try {
            String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            User currentUser = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            // Validation du fichier audio
            if (audioFile.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Le fichier audio est requis"));
            }

            if (!audioFile.getContentType().startsWith("audio/")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Le fichier doit être un fichier audio"));
            }

            if (audioFile.getSize() > 10 * 1024 * 1024) { // 10MB max
                return ResponseEntity.badRequest().body(Map.of("error", "Le fichier est trop volumineux (max 10MB)"));
            }

            // Upload vers FTP
            String originalName = audioFile.getOriginalFilename();
            if (originalName == null || originalName.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Nom de fichier invalide"));
            }

            Path tempFile = Files.createTempFile("audio_upload_", "_" + originalName);

            try {
                audioFile.transferTo(tempFile);

                String tempFileName = System.currentTimeMillis() + "_" + originalName;
                String remoteFilePath = "sons/" + tempFileName;

                //String uploadedFileUrl = uploadFile(tempFile.toString(), remoteFilePath);



                String uploadedFileUrl = uploadFileGitHub(tempFile.toString(), "sons");




                if (uploadedFileUrl == null) {
                    return ResponseEntity.status(500)
                            .body(Map.of("error", "Erreur lors de l'upload du son vers le serveur"));
                }

                // Mettre à jour l'utilisateur avec l'URL du son
                currentUser.setSonEntreeURL(uploadedFileUrl);
                userService.updateUser(currentUser);

                Map<String, Object> response = new HashMap<>();
                response.put("message", "Son d'entrée mis à jour avec succès");
                response.put("sonEntreeURL", uploadedFileUrl);
                response.put("username", username);

                return ResponseEntity.ok(response);

            } finally {
                Files.deleteIfExists(tempFile);
            }

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Erreur lors de la mise à jour du son: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    ///////////////////////////////////////////////////////////////////
    ///// FIN DU NOUVEL ENDPOINT
    ///////////////////////////////////////////////////////////////////

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

            Path tempFile = Files.createTempFile("upload_", "_" + originalName);

            try {
                file.transferTo(tempFile);

                String tempFileName = System.currentTimeMillis() + "_" + originalName;
                String remoteFilePath = "photos/" + tempFileName;

                uploadedFileUrl = uploadFile(tempFile.toString(), remoteFilePath);

                if (uploadedFileUrl == null) {
                    return ResponseEntity.status(500)
                            .body("Erreur lors de l'upload de la photo vers le serveur");
                }

                User user = userService.createUser(username, email, password, uploadedFileUrl);

                String responseText = "Nom utilisateur : " + user.getUsername() +
                        "\nEmail : " + email +
                        "\nMot de passe : " + password +
                        "\nURL de la photo : " + uploadedFileUrl +
                        "\nRôle : " + user.getRole();

                return ResponseEntity.ok(responseText);

            } finally {
                Files.deleteIfExists(tempFile);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body("Erreur lors de l'inscription : " + e.getMessage());
        }
    }

    @PostMapping("/register-admin")
    public ResponseEntity<String> registerAdmin(
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

            String tempFileName = System.currentTimeMillis() + "_" + originalName;
            String remoteFilePath = "photos/" + tempFileName;

            uploadedFileUrl = uploadFile(tempFileName, remoteFilePath);

            if (uploadedFileUrl == null) {
                return ResponseEntity.status(500)
                        .body("Erreur lors de l'upload de la photo vers le serveur");
            }

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

    @GetMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> getUserById(@PathVariable Long userId) {
        try {
            User user = userService.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + userId));

            List<MusicPreference> preferences = musicPreferenceService.getUserPreferences(userId);
            List<Map<String, Object>> musicPreferences = preferences.stream()
                    .map(pref -> {
                        Map<String, Object> prefMap = new HashMap<>();
                        prefMap.put("musicType", pref.getMusicType().name());
                        prefMap.put("level", pref.getPreferenceLevel());
                        prefMap.put("label", pref.getPreferenceLabel());
                        return prefMap;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("id", user.getId());
            response.put("username", user.getUsername());
            response.put("email", user.getEmail());
            response.put("role", user.getRole());
            response.put("createdAt", user.getCreatedAt().toString());
            response.put("photoUrl", user.getPhotoUrl());
            response.put("sonEntreeURL", user.getSonEntreeURL()); // ✅ AJOUT DU SON D'ENTRÉE
            response.put("musicPreferences", musicPreferences);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(404).body(errorResponse);
        }
    }

    @GetMapping("/all")
    public ResponseEntity<List<Map<String, Object>>> getAllUsers() {
        try {
            List<User> users = userService.getAllUsers();

            List<Map<String, Object>> response = users.stream()
                    .map(user -> {
                        Map<String, Object> userMap = new HashMap<>();
                        userMap.put("id", user.getId());
                        userMap.put("username", user.getUsername());
                        userMap.put("email", user.getEmail());
                        userMap.put("role", user.getRole());
                        userMap.put("photoUrl", user.getPhotoUrl());
                        userMap.put("sonEntreeURL", user.getSonEntreeURL()); // ✅ AJOUT DU SON D'ENTRÉE
                        userMap.put("createdAt", user.getCreatedAt());
                        return userMap;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(List.of());
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<Map<String, String>> getProfile(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String username = jwtUtil.extractUsername(token);

        if (jwtUtil.validateToken(token, username)) {
            Map<String, String> response = new HashMap<>();
            response.put("username", username);
            response.put("message", "Bienvenue dans ton profil sécurisé !");
            return ResponseEntity.ok(response);
        } else {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Token invalide ou expiré");
            return ResponseEntity.status(401).body(response);
        }
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser() {
        String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (username == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Utilisateur non authentifié");
            return ResponseEntity.status(401).body(response);
        }

        User user = userService.findByUsername(username).orElse(null);
        if (user == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Utilisateur non trouvé");
            return ResponseEntity.status(404).body(response);
        }

        List<MusicPreference> preferences = musicPreferenceService.getUserPreferences(user.getId());
        List<Map<String, Object>> musicPreferences = preferences.stream()
                .map(pref -> {
                    Map<String, Object> prefMap = new HashMap<>();
                    prefMap.put("musicType", pref.getMusicType().name());
                    prefMap.put("level", pref.getPreferenceLevel());
                    prefMap.put("label", pref.getPreferenceLabel());
                    return prefMap;
                })
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());
        response.put("role", user.getRole());
        response.put("createdAt", user.getCreatedAt().toString());
        response.put("photoUrl", user.getPhotoUrl());
        response.put("sonEntreeURL", user.getSonEntreeURL()); // ✅ AJOUT DU SON D'ENTRÉE
        response.put("musicPreferences", musicPreferences);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}/music-preferences")
    public ResponseEntity<List<Map<String, Object>>> getUserMusicPreferences(@PathVariable Long userId) {
        try {
            List<MusicPreference> preferences = musicPreferenceService.getUserPreferences(userId);

            List<Map<String, Object>> response = preferences.stream()
                    .map(pref -> {
                        Map<String, Object> prefMap = new HashMap<>();
                        prefMap.put("musicType", pref.getMusicType().name());
                        prefMap.put("level", pref.getPreferenceLevel());
                        prefMap.put("label", pref.getPreferenceLabel());
                        return prefMap;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            List<Map<String, Object>> errorList = new ArrayList<>();
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", e.getMessage());
            errorList.add(errorMap);
            return ResponseEntity.status(404).body(errorList);
        }
    }

    @GetMapping("/me/music-preferences")
    public ResponseEntity<List<Map<String, Object>>> getMyMusicPreferences() {
        String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        return getUserMusicPreferences(user.getId());
    }

    @PostMapping("/me/music-preferences")
    public ResponseEntity<Map<String, Object>> createOrUpdateMusicPreference(
            @RequestBody Map<String, Object> request) {

        String musicTypeStr = (String) request.get("musicType");
        Integer level = (Integer) request.get("level");

        if (level == null || level < 0 || level > 3) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Le niveau doit être entre 0 et 3");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        try {
            String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            MusicType musicType = MusicType.valueOf(musicTypeStr.toUpperCase());
            MusicPreference preference = musicPreferenceService.updatePreference(user.getId(), musicType, level);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Préférence musicale " + (request.containsKey("id") ? "mise à jour" : "créée"));
            response.put("id", preference.getId());
            response.put("musicType", preference.getMusicType().name());
            response.put("level", preference.getPreferenceLevel());
            response.put("label", preference.getPreferenceLabel());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Type de musique invalide: " + musicTypeStr);
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/me/music-preferences/batch")
    public ResponseEntity<Map<String, Object>> createMultipleMusicPreferences(
            @RequestBody List<Map<String, Object>> requests) {

        String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        List<Map<String, Object>> results = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (Map<String, Object> request : requests) {
            try {
                String musicTypeStr = (String) request.get("musicType");
                Integer level = (Integer) request.get("level");

                if (level == null || level < 0 || level > 3) {
                    errors.add("Niveau invalide pour " + musicTypeStr + ": " + level);
                    continue;
                }

                MusicType musicType = MusicType.valueOf(musicTypeStr.toUpperCase());
                MusicPreference preference = musicPreferenceService.updatePreference(user.getId(), musicType, level);

                Map<String, Object> result = new HashMap<>();
                result.put("musicType", preference.getMusicType().name());
                result.put("level", preference.getPreferenceLevel());
                result.put("label", preference.getPreferenceLabel());
                results.add(result);
            } catch (Exception e) {
                errors.add("Erreur avec " + request.get("musicType") + ": " + e.getMessage());
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("created", results);
        if (!errors.isEmpty()) {
            response.put("errors", errors);
        }

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/me/music-preferences/{musicType}")
    public ResponseEntity<Map<String, Object>> deleteMusicPreference(@PathVariable String musicType) {
        try {
            String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            musicPreferenceService.deletePreference(user.getId(), MusicType.valueOf(musicType.toUpperCase()));

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Préférence supprimée");
            response.put("musicType", musicType);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Type de musique invalide: " + musicType);
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(404).body(errorResponse);
        }
    }

    @GetMapping("/perfect-music-matches")
    public ResponseEntity<List<Map<String, Object>>> getPerfectMusicMatches() {
        try {
            String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            User currentUser = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            List<User> matches = musicPreferenceService.findPerfectMatches(currentUser.getId());

            List<Map<String, Object>> response = matches.stream()
                    .map(user -> {
                        Map<String, Object> userMap = new HashMap<>();
                        userMap.put("id", user.getId());
                        userMap.put("username", user.getUsername());
                        userMap.put("email", user.getEmail());
                        userMap.put("photoUrl", user.getPhotoUrl());
                        userMap.put("sonEntreeURL", user.getSonEntreeURL()); // ✅ AJOUT DU SON D'ENTRÉE
                        userMap.put("createdAt", user.getCreatedAt());
                        return userMap;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(404).body(List.of(error));
        }
    }

    @GetMapping("/music-soulmates")
    public ResponseEntity<List<Map<String, Object>>> getMusicSoulmates() {
        try {
            String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            User currentUser = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            List<User> soulmates = musicPreferenceService.findMusicSoulmates(currentUser.getId());

            List<Map<String, Object>> response = soulmates.stream()
                    .map(user -> {
                        Map<String, Object> userMap = new HashMap<>();
                        userMap.put("id", user.getId());
                        userMap.put("username", user.getUsername());
                        userMap.put("email", user.getEmail());
                        userMap.put("photoUrl", user.getPhotoUrl());
                        userMap.put("sonEntreeURL", user.getSonEntreeURL()); // ✅ AJOUT DU SON D'ENTRÉE
                        userMap.put("createdAt", user.getCreatedAt());
                        return userMap;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(404).body(List.of(error));
        }
    }

    @GetMapping("/music-enemies")
    public ResponseEntity<List<Map<String, Object>>> getMusicEnemies() {
        try {
            String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            User currentUser = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            List<Map<String, Object>> enemies = musicPreferenceService.findMusicEnemies(currentUser.getId());

            List<Map<String, Object>> response = enemies.stream()
                    .map(enemyInfo -> {
                        User user = (User) enemyInfo.get("user");
                        Map<String, Object> userMap = new HashMap<>();
                        userMap.put("id", user.getId());
                        userMap.put("username", user.getUsername());
                        userMap.put("email", user.getEmail());
                        userMap.put("photoUrl", user.getPhotoUrl());
                        userMap.put("sonEntreeURL", user.getSonEntreeURL()); // ✅ AJOUT DU SON D'ENTRÉE
                        userMap.put("createdAt", user.getCreatedAt());
                        userMap.put("reason", enemyInfo.get("reason"));
                        userMap.put("musicType", enemyInfo.get("musicType"));
                        userMap.put("conflictType", enemyInfo.get("conflictType"));
                        return userMap;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(404).body(List.of(error));
        }
    }

    @GetMapping("/admin/dashboard")
    public ResponseEntity<Map<String, Object>> adminDashboard() {
        String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        long totalUsers = userService.getAllUsers().size();
        long adminUsers = userService.getAllUsers().stream()
                .filter(user -> "ADMIN".equals(user.getRole()))
                .count();

        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("message", "Bienvenue dans le tableau de bord administrateur");
        dashboard.put("connectedAdmin", username);
        dashboard.put("totalUsers", totalUsers);
        dashboard.put("adminUsers", adminUsers);

        return ResponseEntity.ok(dashboard);
    }

    @DeleteMapping("/admin/delete/{userId}")
    public ResponseEntity<String> deleteUser(@PathVariable Long userId) {
        String adminUsername = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        return ResponseEntity.ok("Utilisateur avec ID " + userId + " supprimé par l'admin " + adminUsername);
    }

    @GetMapping("/admin/users-details")
    public ResponseEntity<List<User>> getAllUsersWithDetails() {
        List<User> users = userService.getAllUsers();
        users.forEach(user -> user.setPassword("*****"));
        return ResponseEntity.ok(users);
    }


    }









