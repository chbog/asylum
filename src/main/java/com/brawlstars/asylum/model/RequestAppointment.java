package com.brawlstars.asylum.model;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "request_appointment")
public class RequestAppointment {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @Column(name = "doctor_position")
    private String doctorPosition;

    @Column(name = "status", columnDefinition = "varchar(10) default 'REQUESTED'")
    @Enumerated(EnumType.STRING)
    private AppointmentStatus status = AppointmentStatus.REQUESTED;

    @ManyToOne(cascade = CascadeType.MERGE)
    @JoinColumn(name = "user_id")
    private User user;
}
