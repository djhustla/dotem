package main.services;

import main.modeles.*;
import main.repository.MusicPreferenceRepository;
import main.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MusicPreferenceService {

    @Autowired
    private MusicPreferenceRepository musicPreferenceRepository;

    @Autowired
    private UserRepository userRepository;

    public MusicPreference updatePreference(Long userId, MusicType musicType, Integer level) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        MusicPreference preference = musicPreferenceRepository
                .findByUserAndMusicType(user, musicType)
                .orElse(new MusicPreference(user, musicType, level));

        preference.setPreferenceLevel(level);
        return musicPreferenceRepository.save(preference);
    }

    public List<MusicPreference> getUserPreferences(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        return musicPreferenceRepository.findByUser(user);
    }

    public void deletePreference(Long userId, MusicType musicType) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        MusicPreference preference = musicPreferenceRepository
                .findByUserAndMusicType(user, musicType)
                .orElseThrow(() -> new RuntimeException(
                        "Préférence non trouvée pour le type: " + musicType.name() +
                                " et l'utilisateur: " + user.getUsername()
                ));

        musicPreferenceRepository.delete(preference);
        System.out.println("✅ Préférence supprimée : " + musicType.name() +
                " pour l'utilisateur " + user.getUsername());
    }

    public List<User> findPerfectMatches(Long currentUserId) {
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        List<MusicPreference> currentPreferences = getUserPreferencesOrdered(currentUserId);

        if (currentPreferences.isEmpty()) {
            return new ArrayList<>();
        }

        return userRepository.findAll().stream()
                .filter(user -> !user.getId().equals(currentUserId))
                .filter(user -> hasExactSamePreferences(user, currentPreferences))
                .collect(Collectors.toList());
    }

    public List<User> findMusicSoulmates(Long currentUserId) {
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        Optional<MusicPreference> favoritePreference = musicPreferenceRepository.findByUserAndPreferenceLevel(currentUser, 3);

        if (favoritePreference.isEmpty()) {
            return new ArrayList<>();
        }

        MusicType favoriteGenre = favoritePreference.get().getMusicType();

        return musicPreferenceRepository.findUsersByMusicTypeAndLevel(favoriteGenre, 3).stream()
                .filter(user -> !user.getId().equals(currentUserId))
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> findMusicEnemies(Long currentUserId) {
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        List<Map<String, Object>> enemies = new ArrayList<>();

        Optional<MusicPreference> currentFavorite = musicPreferenceRepository.findByUserAndPreferenceLevel(currentUser, 3);
        Optional<MusicPreference> currentHated = musicPreferenceRepository.findByUserAndPreferenceLevel(currentUser, 0);

        if (currentFavorite.isPresent()) {
            MusicType favoriteGenre = currentFavorite.get().getMusicType();
            List<User> usersWhoHateMyFavorite = musicPreferenceRepository.findUsersByMusicTypeAndLevel(favoriteGenre, 0)
                    .stream()
                    .filter(user -> !user.getId().equals(currentUserId))
                    .collect(Collectors.toList());

            for (User user : usersWhoHateMyFavorite) {
                Map<String, Object> enemyInfo = new HashMap<>();
                enemyInfo.put("user", user);
                enemyInfo.put("reason", "Déteste votre musique préférée");
                enemyInfo.put("musicType", favoriteGenre);
                enemyInfo.put("conflictType", "THEY_HATE_MY_FAVORITE");
                enemies.add(enemyInfo);
            }
        }

        if (currentHated.isPresent()) {
            MusicType hatedGenre = currentHated.get().getMusicType();
            List<User> usersWhoLoveMyHated = musicPreferenceRepository.findUsersByMusicTypeAndLevel(hatedGenre, 3)
                    .stream()
                    .filter(user -> !user.getId().equals(currentUserId))
                    .collect(Collectors.toList());

            for (User user : usersWhoLoveMyHated) {
                Map<String, Object> enemyInfo = new HashMap<>();
                enemyInfo.put("user", user);
                enemyInfo.put("reason", "Adore la musique que vous détestez");
                enemyInfo.put("musicType", hatedGenre);
                enemyInfo.put("conflictType", "THEY_LOVE_MY_HATED");
                enemies.add(enemyInfo);
            }
        }

        return enemies;
    }

    public List<Map<String, Object>> findCompatibleUsers(Long currentUserId) {
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        List<Object[]> results = musicPreferenceRepository.findCompatibleUsers(currentUser, 1.5);

        return results.stream().map(result -> {
            User compatibleUser = (User) result[0];
            Double score = (Double) result[1];

            Map<String, Object> response = new HashMap<>();
            response.put("user", createUserMap(compatibleUser));
            response.put("compatibilityScore", Math.round((1 - score/3) * 100));
            response.put("sharedPreferences", getSharedPreferences(currentUser, compatibleUser));

            return response;
        }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> getSharedPreferences(User user1, User user2) {
        List<MusicPreference> prefs1 = musicPreferenceRepository.findByUser(user1);
        List<MusicPreference> prefs2 = musicPreferenceRepository.findByUser(user2);

        Map<MusicType, MusicPreference> prefsMap2 = prefs2.stream()
                .collect(Collectors.toMap(MusicPreference::getMusicType, p -> p));

        List<Map<String, Object>> sharedList = new ArrayList<>();

        for (MusicPreference p1 : prefs1) {
            if (prefsMap2.containsKey(p1.getMusicType())) {
                MusicPreference p2 = prefsMap2.get(p1.getMusicType());

                Map<String, Object> sharedPref = new HashMap<>();
                sharedPref.put("musicType", p1.getMusicType().name());
                sharedPref.put("yourLevel", p1.getPreferenceLevel());
                sharedPref.put("theirLevel", p2.getPreferenceLevel());
                sharedPref.put("compatibility",
                        Math.abs(p1.getPreferenceLevel() - p2.getPreferenceLevel()) <= 1 ? "Élevée" : "Moyenne");

                sharedList.add(sharedPref);
            }
        }

        return sharedList;
    }

    private boolean hasExactSamePreferences(User user, List<MusicPreference> targetPreferences) {
        List<MusicPreference> userPreferences = getUserPreferencesOrdered(user.getId());

        if (userPreferences.size() != targetPreferences.size()) {
            return false;
        }

        for (int i = 0; i < targetPreferences.size(); i++) {
            MusicPreference targetPref = targetPreferences.get(i);
            MusicPreference userPref = userPreferences.get(i);

            if (!targetPref.getMusicType().equals(userPref.getMusicType()) ||
                    !targetPref.getPreferenceLevel().equals(userPref.getPreferenceLevel())) {
                return false;
            }
        }

        return true;
    }

    private List<MusicPreference> getUserPreferencesOrdered(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        return musicPreferenceRepository.findByUserOrderByLevelDesc(user);
    }

    private Map<String, Object> createUserMap(User user) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", user.getId());
        userMap.put("username", user.getUsername());
        userMap.put("photoUrl", user.getPhotoUrl());
        return userMap;
    }
}