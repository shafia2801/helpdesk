package helpdesk.controller;

import helpdesk.entity.Comment;
import helpdesk.entity.Ticket;
import helpdesk.repository.CommentRepository;
import helpdesk.repository.TicketRepository;
import helpdesk.repository.UserRepository;
import helpdesk.service.EmailService;
import helpdesk.service.TicketService;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/tickets")
public class TicketController {
@Autowired private UserRepository userRepo;
    @Autowired private TicketService ticketService;
    @Autowired private TicketRepository ticketRepo;
    @Autowired private EmailService emailService;
@Autowired private CommentRepository commentRepo;
    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("ticket", new Ticket());
        return "create_ticket";
    }

    @PostMapping("/create")
    public String createTicket(@ModelAttribute Ticket ticket) {
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

        ticket.setSlaStatus("Within SLA"); // Initial SLA status
        ticketService.save(ticket);
        return "redirect:/admin/tickets";
    }
    @PostMapping("/status")
    @ResponseBody
    public String updateTicketStatus(@RequestParam Long ticketId,
                                     @RequestParam String status) {
        Ticket ticket = ticketRepo.findById(ticketId).orElseThrow();
        ticket.setStatus(status);
        ticketRepo.save(ticket);
        if ("Resolved".equalsIgnoreCase(status) || "Closed".equalsIgnoreCase(status)) {
            emailService.sendTicketClosureMail(ticket.getAssignedTo(), ticket);
        }
        return "OK";
    }



    @GetMapping("/list")
    public String listTickets(Model model) {
    	ticketService.updateSlaStatusForAllTickets(); 
    	model.addAttribute("tickets", ticketService.getAllTickets());
        model.addAttribute("supportUsers", userRepo.findAllByRole("Support"));

    	return "ticket_list";
    }
    @PostMapping("/updatePriority")
    public String updatePriority(@RequestParam Long ticketId, @RequestParam String priority) {
        ticketService.updateTicketPriority(ticketId, priority);
        return "redirect:/tickets/list";
    }

    @GetMapping("/dashboard")
    public String showDashboard(Model model) {
    	ticketService.updateSlaStatusForAllTickets(); 
    	model.addAttribute("total", ticketRepo.count());
        model.addAttribute("open", ticketRepo.countByStatus("Open"));
        model.addAttribute("slaBreached", ticketRepo.countBySlaStatus("Breached"));
        return "dashboard";
    }
    @GetMapping("/")
    public String home() {
        return "redirect:/tickets/dashboard";
    }
    @PostMapping("/assign")
    public String assignTicket(@RequestParam Long ticketId, @RequestParam Long userId) {
        ticketService.assignTicket(ticketId, userId);
        return "redirect:/tickets/list"; // or to wherever you want
    }
    @GetMapping("/{id}")
    public String viewTicket(@PathVariable Long id, Model model) {
        Ticket ticket = ticketRepo.findById(id).orElseThrow();
        List<Comment> comments = commentRepo.findByTicketId(id);

        model.addAttribute("ticket", ticket);
        model.addAttribute("comments", comments);
        return "ticket-view";
    }

    @GetMapping("/ticket-view")
    public String showAllTickets(Model model) {
        List<Ticket> tickets = ticketRepo.findAll();
        model.addAttribute("tickets", tickets);
        return "ticket-view";
    }

}
