package helpdesk.controller;

import helpdesk.entity.Comment;
import helpdesk.entity.Ticket;
import helpdesk.entity.User;
import helpdesk.repository.CommentRepository;
import helpdesk.repository.TicketRepository;
import helpdesk.service.TicketService;
import jakarta.servlet.http.HttpSession;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class UserController {
@Autowired private TicketService ticketService;
	@Autowired private TicketRepository ticketRepo;
	@Autowired private CommentRepository commentRepo;

	@GetMapping("/user/dashboard")
	public String userDashboard(HttpSession session, Model model) {
	    User user = (User) session.getAttribute("loggedUser");
	    if (user == null || !user.getRole().equalsIgnoreCase("user")) {
	        return "redirect:/login";
	    }
	    List<Ticket> myTickets = ticketRepo.findByCreatedBy(user);
	    model.addAttribute("user", user);
	    model.addAttribute("tickets", myTickets);  // ✅ This line is important
	    return "user/dashboard";
	}

    @GetMapping("/user/create-ticket")
    public String createTicketForm(Model model) {
    	 Ticket ticket = new Ticket();
    	    ticket.setPriority("High");
    	model.addAttribute("ticket", ticket);
        return "user/create-ticket"; // make sure ticket object is passed
    }
    @GetMapping("/user/tickets")
    public String viewMyTickets(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedUser");
        List<Ticket> myTickets = ticketRepo.findByCreatedBy(user);
        model.addAttribute("tickets", myTickets);
        return "user/tickets";  // Create user/tickets.html
    }
    @GetMapping("/user/tickets/{id}")
    public String viewTicket(@PathVariable Long id, Model model) {
        Ticket ticket = ticketRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        // Use existing repository method
        List<Comment> comments = commentRepo.findByTicketId(id);

        model.addAttribute("ticket", ticket);
        model.addAttribute("comments", comments);

        return "user/ticket-view";
    }



    @PostMapping("/user/tickets/{id}/close")
    public String closeTicket(@PathVariable Long id, HttpSession session) {
        Ticket ticket = ticketRepo.findById(id).orElseThrow();
        User user = (User) session.getAttribute("loggedUser");

        if (ticket.getCreatedBy().getId().equals(user.getId())) {
            ticket.setStatus("Closed");
            ticketRepo.save(ticket);
        }

        return "redirect:/user/tickets";
    }
    @PostMapping("/user/ticket/{id}/comment")
    public String addComment(@PathVariable Long id,
                              @RequestParam String message,
                              HttpSession session) {
        User loggedUser = (User) session.getAttribute("loggedUser");
        if (loggedUser == null) {
            return "redirect:/login"; // safety check
        }

        Ticket ticket = ticketRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        Comment comment = new Comment();
        comment.setTicket(ticket);
        comment.setUser(loggedUser); // ✅ This is what gives the comment an author
        comment.setMessage(message);
        comment.setCreatedAt(LocalDateTime.now());

        commentRepo.save(comment);

        return "redirect:/user/tickets/" + id;
    }


    
    @PostMapping("/user/tickets/create")
    public String createTicket(Ticket ticket, HttpSession session) {
        User user = (User) session.getAttribute("loggedUser");
        if (user == null || !user.getRole().equalsIgnoreCase("user")) {
            return "redirect:/login";
        }

        ticket.setCreatedBy(user);
        ticket.setStatus("Open");

        // Set SLA due date based on priority
        LocalDateTime now = LocalDateTime.now();
        switch (ticket.getPriority()) {
            case "High":
                ticket.setDueDate(now.plusHours(24));
                break;
            case "Medium":
                ticket.setDueDate(now.plusHours(48));
                break;
            case "Low":
                ticket.setDueDate(now.plusHours(72));
                break;
            default:
                ticket.setDueDate(now.plusHours(48)); // default to Medium
        }

        ticket.setSlaStatus("Within SLA");
        ticketRepo.save(ticket);

        return "redirect:/user/dashboard";
    }

}
