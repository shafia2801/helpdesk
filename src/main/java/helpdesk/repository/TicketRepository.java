package helpdesk.repository;

import helpdesk.entity.Ticket;
import helpdesk.entity.User;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    long countByStatus(String status);
    long countBySlaStatus(String slaStatus);
    List<Ticket> findAllByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    List<Ticket> findByAssignedTo(User user);
    List<Ticket> findByCreatedBy(User user);

    @Query("SELECT t FROM Ticket t WHERE t.status = 'Resolved' AND t.assignedTo.id = :userId")
    List<Ticket> findResolvedTicketsByUser(Long userId);
    @Query("SELECT t FROM Ticket t LEFT JOIN FETCH t.comments")
    List<Ticket> findAllWithComments();

}
