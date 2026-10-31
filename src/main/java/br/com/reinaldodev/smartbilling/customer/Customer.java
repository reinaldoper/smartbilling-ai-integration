package br.com.reinaldodev.smartbilling.customer;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "customers")
public class Customer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String name;
    @Column(nullable = false, unique = true) private String document;
    @Column(nullable = false, unique = true) private String email;
    @Column(nullable = false) private Instant createdAt = Instant.now();

    protected Customer() {}
    public Customer(String name, String document, String email) {
        this.name = name; this.document = document; this.email = email;
    }
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDocument() { return document; }
    public String getEmail() { return email; }
    public Instant getCreatedAt() { return createdAt; }
}
