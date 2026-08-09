package com.fanzone.auth.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "user_interests")
@IdClass(UserInterestId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInterest {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Id
    @Column(length = 20)
    private String interest;
}
