package com.thoughtworks.rslist.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "vote")
public class VoteDto {
    @Id
    @GeneratedValue
    private int id;

    private LocalDateTime localDateTime;

    private int num;
    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserDto user;
    @ManyToOne
    @JoinColumn(name = "rs_event_id")
    private RsEventDto rsEvent;
}
