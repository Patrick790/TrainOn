package licenta.persistence;

import licenta.model.EmailRecipient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IEmailRecipientRepository extends JpaRepository<EmailRecipient, Long> {
    List<EmailRecipient> findByEmailId(Long emailId);

    @Query("SELECT COUNT(er) FROM EmailRecipient er WHERE er.email.id = ?1")
    Long countByEmailId(Long emailId);
}