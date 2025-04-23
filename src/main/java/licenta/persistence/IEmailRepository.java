package licenta.persistence;

import licenta.model.Email;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IEmailRepository extends JpaRepository<Email, Long> {
    List<Email> findByCreatedByOrderByCreatedAtDesc(String createdBy);
    List<Email> findAllByOrderByCreatedAtDesc();
}
