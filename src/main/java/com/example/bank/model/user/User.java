package com.example.bank.model.user;

import com.example.bank.model.card.debitCard.DebitCard;
import com.example.bank.Enums.Role;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private Long userId;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    @JsonIgnore
    private String password;
    @Column(unique = true)
    private String email;
    private String firstName;
    private String lastName;
    @Column(unique = true)
    private String phoneNumber;



    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @Column(nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    private Boolean blocked;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<DebitCard> cards = new ArrayList<>();


    @OneToOne
    @JoinColumn(name = "main_card_id")
    private DebitCard mainCard;



}


