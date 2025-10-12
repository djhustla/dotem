package main.modeles;


import main.modeles.Message;

import java.util.Date;

public class MessageDTO {
    private Long id;
    private String intituleMessage;
    private Date heureMessage;
    private String username; // on expose juste le username de l'utilisateur
    private String photoUrl; // on expose juste le username de l'utilisateur


    public MessageDTO() {}

    // Constructeur qui convertit un Message en DTO
    public MessageDTO(Message message) {
        this.id = message.getId();
        this.intituleMessage = message.getIntituleMessage();
        this.heureMessage = message.getHeureMessage();
        this.username = message.getUser().getUsername();
        this.photoUrl =  message.getUser().getPhotoUrl();
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getIntituleMessage() { return intituleMessage; }
    public void setIntituleMessage(String intituleMessage) { this.intituleMessage = intituleMessage; }

    public Date getHeureMessage() { return heureMessage; }
    public void setHeureMessage(Date heureMessage) { this.heureMessage = heureMessage; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
}

