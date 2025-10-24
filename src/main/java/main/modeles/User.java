package main.modeles;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role = "USER"; // rôle par défaut

    @Column(nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt = new Date(); // date de création automatique

    // ✅ COLONNE pour stocker l'adresse/URL de la photo
    @Column(name = "photo_url")
    private String photoUrl; // ex: "/uploads/users/lucie.png"

    // ✅ NOUVELLE COLONNE pour stocker l'URL du son d'entrée
    @Column(name = "son_entree_url", nullable = true)
    private String sonEntreeURL; // ex: "/uploads/sons/user123_son.wav"

    // ✅ Liste des préférences musicales
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<MusicPreference> musicPreferences;

    // =====================
    // Getters et setters
    // =====================
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    // ✅ NOUVEAU Getter et Setter pour sonEntreeURL
    public String getSonEntreeURL() { return sonEntreeURL; }
    public void setSonEntreeURL(String sonEntreeURL) { this.sonEntreeURL = sonEntreeURL; }

    // Constructeurs
    public User() {}

    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    // Getters et Setters pour les préférences musicales
    public List<MusicPreference> getMusicPreferences() {
        return musicPreferences;
    }

    public void setMusicPreferences(List<MusicPreference> musicPreferences) {
        this.musicPreferences = musicPreferences;
    }

    // Méthode utilitaire pour ajouter une préférence
    public void addMusicPreference(MusicPreference preference) {
        if (this.musicPreferences == null) {
            this.musicPreferences = new ArrayList<>();
        }
        preference.setUser(this);
        this.musicPreferences.add(preference);
    }
}