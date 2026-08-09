package com.example.restaurant.entity.user;

import com.example.restaurant.entity.image.Image;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Entity
@Table(name = "employees")
@Getter
@Setter
@NoArgsConstructor
@SQLDelete(sql = "UPDATE employees SET is_deleted = TRUE WHERE id = ?")
public class Employee implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String firstName;
    @Column(nullable = false)
    private String lastName;
    @Column(nullable = false, unique = true)
    private String email;
    @Column(unique = true)
    private String pendingEmail = null;
    @Column(nullable = false, columnDefinition = "TINYINT(1)")
    private boolean emailConfirmed = false;
    private String phone;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_picture_id")
    private Image profilePicture;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    private String password;
    private LocalDateTime passwordCreatedAt = null;
    private LocalDateTime passwordUpdatedAt = null;
    @ManyToOne
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;
    @Column(nullable = false, columnDefinition = "TINYINT(1)")
    private boolean isDeleted = false;

    @Override
    @NullMarked
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(
                new SimpleGrantedAuthority(
                        role.getName().toString()));
    }

    @Override
    @NullMarked
    public String getUsername() {
        return getEmail();
    }
}
