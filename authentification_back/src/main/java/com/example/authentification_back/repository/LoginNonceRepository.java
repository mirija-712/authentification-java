package com.example.authentification_back.repository;

import com.example.authentification_back.entity.LoginNonce;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoginNonceRepository extends JpaRepository<LoginNonce, String> {

	Optional<LoginNonce> findByNonce(String nonce);
}
