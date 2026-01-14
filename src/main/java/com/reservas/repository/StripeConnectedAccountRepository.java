package com.reservas.repository;

import com.reservas.entity.StripeConnectedAccount;
import com.reservas.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StripeConnectedAccountRepository extends JpaRepository<StripeConnectedAccount, String> {

    Optional<StripeConnectedAccount> findByUsuario(Usuario usuario);

    Optional<StripeConnectedAccount> findByUsuarioId(UUID usuarioId);

    Optional<StripeConnectedAccount> findByStripeAccountId(String stripeAccountId);

    boolean existsByUsuarioId(UUID usuarioId);

    boolean existsByStripeAccountId(String stripeAccountId);
}
