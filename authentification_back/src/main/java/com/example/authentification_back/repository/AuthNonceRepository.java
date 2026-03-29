package com.example.authentification_back.repository;

import com.example.authentification_back.entity.AuthNonce;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthNonceRepository extends JpaRepository<AuthNonce, Long> {

	boolean existsByUserIdAndNonce(Long userId, String nonce);
}
