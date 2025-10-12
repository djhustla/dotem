package main.modeles;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String intituleMessage;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date heureMessage = new Date(); // heure automatique


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    // =====================
    // Constructeurs
    // =====================
    public Message() {}

    public Message(String intituleMessage, User user) {
        this.intituleMessage = intituleMessage;
        this.user = user;
    }

    // =====================
    // Getters / Setters
    // =====================
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getIntituleMessage() { return intituleMessage; }
    public void setIntituleMessage(String intituleMessage) { this.intituleMessage = intituleMessage; }

    public Date getHeureMessage() { return heureMessage; }
    public void setHeureMessage(Date heureMessage) { this.heureMessage = heureMessage; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}
