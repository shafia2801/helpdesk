package helpdesk.service;

import helpdesk.entity.Ticket;
import helpdesk.entity.User;
import helpdesk.repository.TicketRepository;
import helpdesk.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TicketService {

    @Autowired private TicketRepository ticketRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private EmailService emailService;

    public Ticket createTicket(Ticket ticket) {
        LocalDateTime now = LocalDateTime.now();
        Duration slaDuration = getSlaDuration(ticket.getPriority());
        ticket.setCreatedAt(now);
        ticket.setDueDate(now.plus(slaDuration));
        ticket.setSlaStatus("Within SLA");
        return ticketRepo.save(ticket);
    }

    public void save(Ticket ticket) {
        ticketRepo.save(ticket);
    }

    public List<Ticket> getAllTickets() {
        return ticketRepo.findAll();
    }

    public void assignTicket(Long ticketId, Long userId) {
        Ticket t = ticketRepo.findById(ticketId).orElseThrow();
        User u = userRepo.findById(userId).orElseThrow();
        t.setAssignedTo(u);
        ticketRepo.save(t);
        emailService.sendAssignmentEmail(t);
    }

    public Duration getSlaDuration(String priority) {
        switch (priority.toLowerCase()) {
            case "high": return Duration.ofHours(4);
            case "medium": return Duration.ofHours(8);
            case "low": return Duration.ofHours(24);
            default: return Duration.ofHours(12);
        }
    }

    public void updateSlaStatus(Ticket ticket) {
        // If the ticket is already resolved or closed, its SLA status is final.
        if ("Resolved".equalsIgnoreCase(ticket.getStatus()) || "Closed".equalsIgnoreCase(ticket.getStatus())) {
            return; // Do nothing
        }

        if (ticket.getDueDate() != null) {
            if (LocalDateTime.now().isAfter(ticket.getDueDate())) {
                ticket.setSlaStatus("Breached");
            } else {
                ticket.setSlaStatus("Within SLA");
            }
        } else {
            ticket.setSlaStatus("Not Applicable");
        }
    }

    
    public void updateSlaStatusForAllTickets() {
        List<Ticket> tickets = ticketRepo.findAll();
        LocalDateTime now = LocalDateTime.now();

        for (Ticket t : tickets) {
            // Skip if already resolved or closed
            if (t.getStatus().equalsIgnoreCase("Resolved") || t.getStatus().equalsIgnoreCase("Closed")) continue;

            if (t.getDueDate() != null) {
                boolean isBreached = now.isAfter(t.getDueDate());
                String newSlaStatus = isBreached ? "Breached" : "Within SLA";

                if (!newSlaStatus.equals(t.getSlaStatus())) {
                    t.setSlaStatus(newSlaStatus);

                    // Sync status with SLA status
                    if (isBreached) {
                        t.setStatus("Escalated");  // Or "SLA Breached"
                    } else {
                        t.setStatus("In Progress"); // Or "Reopened" if previously escalated
                    }

                    ticketRepo.save(t);
                }
            }
        }
    }

    @Scheduled(cron = "0 * * * * *") 
    public void checkSLA() {
        List<Ticket> tickets = ticketRepo.findAll();
        LocalDateTime now = LocalDateTime.now();

        for (Ticket t : tickets) {
            if (t.getDueDate() == null || t.getStatus().equalsIgnoreCase("Closed") || t.getStatus().equalsIgnoreCase("Resolved")) continue;

            // SLA Breach
            if (now.isAfter(t.getDueDate()) && !"Breached".equalsIgnoreCase(t.getSlaStatus())) {
                t.setSlaStatus("Breached");
                t.setStatus("Escalated");
                ticketRepo.save(t);
                emailService.sendSLAAlert(t);
            }

            // SLA Reminder (1 hour before due)
            if (t.getDueDate().minusHours(1).isBefore(now) && !t.isReminderSent()) {
                emailService.sendSLAReminder(t);
                t.setReminderSent(true); // track reminder was sent
                ticketRepo.save(t);
            }


        }
    }
    public void updateTicketPriority(Long ticketId, String newPriority) {
        Ticket ticket = ticketRepo.findById(ticketId).orElseThrow();

        Duration newSlaDuration = getSlaDuration(newPriority);
        ticket.setPriority(newPriority);
        ticket.setDueDate(LocalDateTime.now().plus(newSlaDuration));
        ticket.setSlaStatus("Within SLA");

        // Reset status if it was breached earlier
        if ("Escalated".equalsIgnoreCase(ticket.getStatus()) || "Breached".equalsIgnoreCase(ticket.getSlaStatus())) {
            ticket.setStatus("In Progress");
        }

        ticketRepo.save(ticket);
    }
    public long countTotalTickets() {
        return ticketRepo.count();
    }

    public long countResolvedTickets() {
        return ticketRepo.countByStatus("Resolved");
    }

    public long countSlaBreachedTickets() {
        return ticketRepo.countBySlaStatus("Breached");
    }
    
    public long getEscalatedTickets() {
        return ticketRepo.countByStatus("Escalated");
    }

    public long countOpenTickets() {
        return ticketRepo.countByStatus("Open");
    }

    public long countInProgressTickets() {
        return ticketRepo.countByStatus("In Progress");
    }
    public Ticket getTicketById(Long id) {
        return ticketRepo.findById(id).orElseThrow(() -> new RuntimeException("Ticket not found"));
    }

}

