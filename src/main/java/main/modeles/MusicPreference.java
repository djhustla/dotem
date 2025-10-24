// MusicPreference.java
package main.modeles;

import jakarta.persistence.*;

@Entity
@Table(name = "music_preferences")
public class MusicPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MusicType musicType;

    @Column(nullable = false)
    private Integer preferenceLevel; // 0=déteste, 1=neutre, 2=apprécie, 3=adore

    // Constructeurs
    public MusicPreference() {}

    public MusicPreference(User user, MusicType musicType, Integer preferenceLevel) {
        this.user = user;
        this.musicType = musicType;
        this.preferenceLevel = preferenceLevel;
    }

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public MusicType getMusicType() { return musicType; }
    public void setMusicType(MusicType musicType) { this.musicType = musicType; }

    public Integer getPreferenceLevel() { return preferenceLevel; }
    public void setPreferenceLevel(Integer preferenceLevel) {
        this.preferenceLevel = preferenceLevel;
    }

    // Méthode utilitaire pour le libellé
    public String getPreferenceLabel() {
        switch (preferenceLevel) {
            case 3: return "J'adore";
            case 2: return "J'apprécie";
            case 1: return "Neutre";
            case 0: return "Je déteste";
            default: return "Inconnu";
        }
    }
}