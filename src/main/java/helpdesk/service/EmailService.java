package helpdesk.service;

import helpdesk.entity.Ticket;
import helpdesk.entity.User;
import helpdesk.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private UserRepository userRepo;

    // ✅ Generic method to send an email to a single user
    public void notifyUser(User user, String subject, String body) {
        if (user != null && user.getEmail() != null) {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(user.getEmail());
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        }
    }

    // ✅ Send to all admins
    public void notifyAdmins(String subject, String body) {
        List<User> admins = userRepo.findAll().stream()
            .filter(u -> "ADMIN".equalsIgnoreCase(u.getRole()))
            .toList();
        for (User admin : admins) {
            notifyUser(admin, subject, body);
        }
    }

    // ✅ Send to all support users
    public void notifySupportUsers(String subject, String body) {
        List<User> supportUsers = userRepo.findAll().stream()
            .filter(u -> "SUPPORT".equalsIgnoreCase(u.getRole()))
            .toList();
        for (User support : supportUsers) {
            notifyUser(support, subject, body);
        }
    }

    // ✅ Ticket assignment mail
    public void sendAssignmentEmail(Ticket ticket) {
        String subject = "New Ticket Assigned: " + ticket.getTitle();
        String body = "Hi " + ticket.getAssignedTo().getName() + ",\n\n"
                + "You have been assigned a new ticket:\n"
                + "Title: " + ticket.getTitle() + "\n"
                + "Priority: " + ticket.getPriority() + "\n"
                + "Status: " + ticket.getStatus() + "\n"
                + "Due Date: " + ticket.getDueDate() + "\n\n"
                + "Please login to the Helpdesk System to take action.\n\n"
                + "-- Helpdesk Team";

        notifyUser(ticket.getAssignedTo(), subject, body);
    }

    // ✅ SLA breach mail
    public void sendSLAAlert(Ticket ticket) {
        String subject = "⚠ SLA Breach - Ticket #" + ticket.getId();
        String body = "The following ticket has breached SLA:\n\n"
                + "Title: " + ticket.getTitle() + "\n"
                + "Priority: " + ticket.getPriority() + "\n"
                + "Current Status: " + ticket.getStatus() + "\n"
                + "Due Date: " + ticket.getDueDate() + "\n\n"
                + "Please review and take necessary action.";

        notifyUser(ticket.getAssignedTo(), subject, body);
        notifyAdmins(subject, body); // also notify all admins
    }

    // ✅ SLA reminder mail
    public void sendSLAReminder(Ticket ticket) {
        String subject = "⏰ SLA Reminder - Ticket #" + ticket.getId();
        String body = "Reminder: Ticket is about to breach SLA in 1 hour.\n\n"
                + "Title: " + ticket.getTitle() + "\n"
                + "Priority: " + ticket.getPriority() + "\n"
                + "Current Status: " + ticket.getStatus() + "\n"
                + "Due Date: " + ticket.getDueDate() + "\n\n"
                + "Please resolve or escalate appropriately.";

        notifyUser(ticket.getAssignedTo(), subject, body);
    }

    // ✅ Ticket created mail to all support users
    public void sendTicketCreationAlert(Ticket ticket) {
        String subject = "📩 New Ticket Created: " + ticket.getTitle();
        String body = "A new ticket has been created:\n\n"
                + "Title: " + ticket.getTitle() + "\n"
                + "Priority: " + ticket.getPriority() + "\n"
                + "Current Status: " + ticket.getStatus() + "\n"
                + "Created At: " + ticket.getCreatedAt() + "\n"
                + "Due Date: " + ticket.getDueDate() + "\n\n"
                + "Please login to assign or resolve.";

        notifySupportUsers(subject, body);
    }

    // ✅ Ticket closed/resolved email to user (only if you add createdBy field)
    public void sendTicketClosureMail(User user, Ticket ticket) {
        String subject = "✅ Your Ticket is Now " + ticket.getStatus();
        String body = "Dear " + user.getName() + ",\n\n"
                + "Your ticket has been marked as " + ticket.getStatus() + ".\n\n"
                + "Ticket Details:\n"
                + "Title: " + ticket.getTitle() + "\n"
                + "Priority: " + ticket.getPriority() + "\n"
                + "Created At: " + ticket.getCreatedAt() + "\n"
                + "Due Date: " + ticket.getDueDate() + "\n\n"
                + "Thank you for using the Helpdesk System.";

        notifyUser(user, subject, body);
    }
}
