package kh.edu.istad.iam.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "permissions")
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String permission;

    private String description;

    @ManyToMany(mappedBy = "permissions")
    private List<User> users;

    @ManyToMany(mappedBy = "permissions")
    private List<Role> roles;

}