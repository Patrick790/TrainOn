package licenta.persistence;

import jakarta.transaction.Transactional;
import licenta.model.CardPaymentMethod;
import licenta.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ICardPaymentMethodSpringRepository extends JpaRepository<CardPaymentMethod, Long> {

    /**
     * Găsește toate cardurile active ale unui utilizator
     */
    List<CardPaymentMethod> findByUserAndIsActiveTrueOrderByIsDefaultDescCreatedAtDesc(User user);

    /**
     * Găsește cardul default al unui utilizator
     */
    Optional<CardPaymentMethod> findByUserAndIsDefaultTrueAndIsActiveTrue(User user);

    /**
     * Găsește un card după Stripe Payment Method ID
     */
    Optional<CardPaymentMethod> findByStripePaymentMethodId(String stripePaymentMethodId);

    /**
     * Găsește toate cardurile unui utilizator (active și inactive)
     */
    List<CardPaymentMethod> findByUserOrderByIsDefaultDescCreatedAtDesc(User user);

    /**
     * Verifică dacă utilizatorul are deja un card salvat cu același Stripe ID
     */
    boolean existsByUserAndStripePaymentMethodId(User user, String stripePaymentMethodId);

    /**
     * Contorizează cardurile active ale unui utilizator
     */
    long countByUserAndIsActiveTrue(User user);

    /**
     * Marchează toate cardurile unui utilizator ca non-default
     */
    @Modifying
    @Transactional
    @Query("UPDATE CardPaymentMethod c SET c.isDefault = false WHERE c.user = :user")
    void unmarkAllAsDefaultForUser(@Param("user") User user);

    /**
     * Găsește cardurile expirate ale unui utilizator
     */
    @Query("SELECT c FROM CardPaymentMethod c WHERE c.user = :user AND c.isActive = true AND " +
            "(c.cardExpYear < :currentYear OR (c.cardExpYear = :currentYear AND c.cardExpMonth < :currentMonth))")
    List<CardPaymentMethod> findExpiredCardsForUser(@Param("user") User user,
                                                    @Param("currentYear") Integer currentYear,
                                                    @Param("currentMonth") Integer currentMonth);

    /**
     * Dezactivează toate cardurile expirate
     */
    @Modifying
    @Query("UPDATE CardPaymentMethod c SET c.isActive = false, c.isDefault = false WHERE " +
            "(c.cardExpYear < :currentYear OR (c.cardExpYear = :currentYear AND c.cardExpMonth < :currentMonth))")
    int deactivateExpiredCards(@Param("currentYear") Integer currentYear,
                               @Param("currentMonth") Integer currentMonth);

    /**
     * Găsește cardurile care urmează să expire în următoarele luni
     */
    @Query("SELECT c FROM CardPaymentMethod c WHERE c.isActive = true AND " +
            "((c.cardExpYear = :currentYear AND c.cardExpMonth <= :nextMonth) OR " +
            "(c.cardExpYear = :nextYear AND c.cardExpMonth <= :nextMonthNextYear))")
    List<CardPaymentMethod> findCardsExpiringInNextMonths(@Param("currentYear") Integer currentYear,
                                                          @Param("nextMonth") Integer nextMonth,
                                                          @Param("nextYear") Integer nextYear,
                                                          @Param("nextMonthNextYear") Integer nextMonthNextYear);
}