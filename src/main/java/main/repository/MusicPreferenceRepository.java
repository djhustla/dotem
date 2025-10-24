package main.repository;

import main.modeles.MusicPreference;
import main.modeles.MusicType;
import main.modeles.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MusicPreferenceRepository extends JpaRepository<MusicPreference, Long> {

    // Méthodes existantes
    List<MusicPreference> findByUser(User user);
    Optional<MusicPreference> findByUserAndMusicType(User user, MusicType musicType);

    // Trouver tous les utilisateurs qui aiment un genre spécifique (niveau 2 ou 3)
    @Query("SELECT mp.user FROM MusicPreference mp WHERE mp.musicType = :musicType AND mp.preferenceLevel >= 2")
    List<User> findUsersWhoLikeMusicType(@Param("musicType") MusicType musicType);

    // Trouver la compatibilité entre deux utilisateurs
    @Query("SELECT AVG(ABS(mp1.preferenceLevel - mp2.preferenceLevel)) " +
            "FROM MusicPreference mp1, MusicPreference mp2 " +
            "WHERE mp1.user = :user1 AND mp2.user = :user2 AND mp1.musicType = mp2.musicType")
    Double calculateCompatibilityScore(@Param("user1") User user1, @Param("user2") User user2);

    // Trouver les utilisateurs avec une bonne compatibilité musicale
    @Query("SELECT u2, AVG(ABS(mp1.preferenceLevel - mp2.preferenceLevel)) as score " +
            "FROM User u1, MusicPreference mp1, User u2, MusicPreference mp2 " +
            "WHERE u1 = :currentUser AND mp1.user = u1 AND mp2.user = u2 AND u1 <> u2 " +
            "AND mp1.musicType = mp2.musicType " +
            "GROUP BY u2 " +
            "HAVING AVG(ABS(mp1.preferenceLevel - mp2.preferenceLevel)) <= :maxDifference " +
            "ORDER BY score ASC")
    List<Object[]> findCompatibleUsers(@Param("currentUser") User currentUser,
                                       @Param("maxDifference") Double maxDifference);

    // ============ NOUVELLES METHODES POUR LE MATCHING ============

    // Trouver une préférence spécifique par niveau
    Optional<MusicPreference> findByUserAndPreferenceLevel(User user, Integer level);

    // Trouver tous les utilisateurs qui ont un genre spécifique à un niveau spécifique
    @Query("SELECT DISTINCT mp.user FROM MusicPreference mp WHERE mp.musicType = :musicType AND mp.preferenceLevel = :level")
    List<User> findUsersByMusicTypeAndLevel(@Param("musicType") MusicType musicType, @Param("level") Integer level);

    // Trouver les préférences d'un utilisateur triées par niveau (pour matching parfait)
    @Query("SELECT mp FROM MusicPreference mp WHERE mp.user = :user ORDER BY mp.preferenceLevel DESC")
    List<MusicPreference> findByUserOrderByLevelDesc(@Param("user") User user);

    // Méthode utilitaire pour compter les préférences communes
    @Query("SELECT COUNT(mp) FROM MusicPreference mp WHERE mp.user = :user AND mp.musicType IN :musicTypes AND mp.preferenceLevel IN :levels")
    Long countMatchingPreferences(@Param("user") User user,
                                  @Param("musicTypes") List<MusicType> musicTypes,
                                  @Param("levels") List<Integer> levels);
}