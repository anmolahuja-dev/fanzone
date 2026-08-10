package com.fanzone.post.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "poll_options")
@Getter
@Setter
public class PollOptionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private PostEntity post;

    @Column(name = "option_text", nullable = false, length = 100)
    private String optionText;

    @Column(name = "vote_count")
    private int voteCount = 0;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}
