package viettel.dac.identityservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import viettel.dac.identityservice.entity.VerificationToken;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, String> {

    Optional<VerificationToken> findByToken(String token);

    @Query("SELECT vt FROM VerificationToken vt WHERE vt.userId = :userId AND vt.tokenType = :tokenType AND vt.used = false AND vt.expiresAt > :now")
    List<VerificationToken> findValidTokensByUserIdAndType(
            @Param("userId") String userId,
            @Param("tokenType") VerificationToken.TokenType tokenType,
            @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE VerificationToken vt SET vt.used = true, vt.usedAt = :now WHERE vt.token = :token")
    void markTokenAsUsed(@Param("token") String token, @Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM VerificationToken vt WHERE vt.expiresAt < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE VerificationToken vt SET vt.used = true, vt.usedAt = :now " +
            "WHERE vt.userId = :userId AND vt.tokenType = :tokenType AND vt.used = false")
    void invalidateAllUserTokensOfType(
            @Param("userId") String userId,
            @Param("tokenType") VerificationToken.TokenType tokenType,
            @Param("now") LocalDateTime now);
}