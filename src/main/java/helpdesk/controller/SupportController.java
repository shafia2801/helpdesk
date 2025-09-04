package helpdesk.controller;

import helpdesk.entity.Ticket;
import helpdesk.entity.User;
import helpdesk.entity.Comment;
import helpdesk.repository.TicketRepository;
import helpdesk.repository.CommentRepository;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/support")
public class SupportController {

    @Autowired private TicketRepository ticketRepo;
    @Autowired private CommentRepository commentRepo;

    // Show dashboard with assigned tickets
    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        User supportUser = (User) session.getAttribute("loggedUser");
        if (supportUser == null || !supportUser.getRole().equalsIgnoreCase("support")) {
            return "redirect:/login";
        }
        List<Ticket> tickets = ticketRepo.findByAssignedTo(supportUser);
        model.addAttribute("tickets", tickets);
        return "support/dashboard"; // templates/support/dashboard.html
    }

    // View single ticket with comments
    @GetMapping("/ticket/{id}")
    public String viewTicket(@PathVariable Long id, HttpSession session, Model model) {
        User supportUser = (User) session.getAttribute("loggedUser");
        Ticket ticket = ticketRepo.findById(id).orElseThrow();
        if (ticket.getAssignedTo() == null || supportUser == null || 
        	    !ticket.getAssignedTo().getId().equals(supportUser.getId())) {
        	    return "redirect:/support/dashboard";
        	}

        model.addAttribute("ticket", ticket);
        model.addAttribute("comments", commentRepo.findByTicketId(id));

        // SLA check
        boolean slaBreached = Duration.between(ticket.getCreatedAt(), LocalDateTime.now()).toHours() > 24;
        model.addAttribute("slaBreached", slaBreached);

        return "support/ticket-view"; // templates/support/ticket-view.html
    }

    // Add comment to ticket
    @PostMapping("/ticket/{id}/comment")
    public String addComment(@PathVariable Long id,
                             @RequestParam String message,
                             HttpSession session) {
        User user = (User) session.getAttribute("loggedUser");
        Ticket ticket = ticketRepo.findById(id).orElseThrow();

        Comment comment = new Comment();
        comment.setTicket(ticket);
        comment.setUser(user);
        comment.setMessage(message);
        comment.setCreatedAt(LocalDateTime.now());

        commentRepo.save(comment);
        return "redirect:/support/ticket/" + id;
    }

    // Update ticket status
    @PostMapping("/ticket/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam String status) {
        Ticket ticket = ticketRepo.findById(id).orElseThrow();
        ticket.setStatus(status);
        ticketRepo.save(ticket);
        return "redirect:/support/ticket/" + id;
    }
}
