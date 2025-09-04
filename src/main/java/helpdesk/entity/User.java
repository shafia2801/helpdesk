package helpdesk.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true, nullable = true)
    private String email;

    private String role; // Admin, Support, User

    private String password;

    private boolean active = true;

    private boolean emailVerified = false;

    private String verificationToken;

    private LocalDateTime tokenExpiry;
    

    // Field for the profile picture URL
    private String profilePictureUrl;
    
    @OneToMany(mappedBy = "assignedTo", cascade = CascadeType.PERSIST)
    private List<Ticket> assignedTickets;

    public User() {}

    public User(String name, String email, String role, String password) {
        this.name = name;
        this.email = email;
        this.role = role;
        this.password = password;
    }

    public Long getId() { return id; }

    public String getName() { return this.name; }

    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; } // REQUIRED METHOD

    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }

    public void setRole(String role) { this.role = role; }

    public String getPassword() { return password; }

    public void setPassword(String password) { this.password = password; }

    public boolean isActive() { return active; }

    public void setActive(boolean active) { this.active = active; }

    public List<Ticket> getAssignedTickets() { return assignedTickets; }

    public void setAssignedTickets(List<Ticket> assignedTickets) { this.assignedTickets = assignedTickets; }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public String getVerificationToken() {
        return verificationToken;
    }

    public void setVerificationToken(String verificationToken) {
        this.verificationToken = verificationToken;
    }

    public LocalDateTime getTokenExpiry() {
        return tokenExpiry;
    }

    public void setTokenExpiry(LocalDateTime tokenExpiry) {
        this.tokenExpiry = tokenExpiry;
    }
    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }

    
}
