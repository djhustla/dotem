// MusicPreferenceController.java - VERSION CORRIGÉE
package main.controlleurs;

import main.modeles.*;
import main.services.MusicPreferenceService;
import main.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/music-preferences")
public class MusicPreferenceController {

    @Autowired
    private MusicPreferenceService musicPreferenceService;

    @Autowired
    private UserRepository userRepository;

    // ✅ CORRIGÉ : Récupérer mes préférences
    @GetMapping("/me")
    public ResponseEntity<List<Map<String, Object>>> getMyPreferences() {
        User user = getCurrentUser();

        List<MusicPreference> preferences = musicPreferenceService.getUserPreferences(user.getId());

        // ✅ CORRECTION : Utiliser HashMap au lieu de Map.of()
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
    }

    // ✅ CORRIGÉ : Mettre à jour une préférence
    @PutMapping("/me/{musicType}")
    public ResponseEntity<Map<String, Object>> updatePreference(
            @PathVariable String musicType,
            @RequestBody Map<String, Integer> request) {

        Integer level = request.get("level");
        if (level < 0 || level > 3) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Le niveau doit être entre 0 et 3");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        User user = getCurrentUser();
        MusicType type = MusicType.valueOf(musicType.toUpperCase());

        MusicPreference preference = musicPreferenceService.updatePreference(user.getId(), type, level);

        // ✅ CORRECTION : Utiliser HashMap
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Préférence mise à jour");
        response.put("musicType", preference.getMusicType().name());
        response.put("level", preference.getPreferenceLevel());
        response.put("label", preference.getPreferenceLabel());

        return ResponseEntity.ok(response);
    }

    // ✅ CORRIGÉ : Récupérer les utilisateurs compatibles
    @GetMapping("/me/compatible-users")
    public ResponseEntity<List<Map<String, Object>>> getCompatibleUsers() {
        User user = getCurrentUser();

        List<Map<String, Object>> compatibleUsers = musicPreferenceService.findCompatibleUsers(user.getId());

        return ResponseEntity.ok(compatibleUsers);
    }

    // ✅ CORRIGÉ : Récupérer tous les types de musique disponibles
    @GetMapping("/music-types")
    public ResponseEntity<List<Map<String, String>>> getMusicTypes() {
        // ✅ CORRECTION : Utiliser HashMap
        List<Map<String, String>> types = Arrays.stream(MusicType.values())
                .map(type -> {
                    Map<String, String> typeMap = new HashMap<>();
                    typeMap.put("code", type.name());
                    typeMap.put("label", type.name().toLowerCase().replace("_", " "));
                    return typeMap;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(types);
    }

    private User getCurrentUser() {
        String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }
}