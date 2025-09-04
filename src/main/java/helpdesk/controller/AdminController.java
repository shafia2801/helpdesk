package helpdesk.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import helpdesk.entity.Ticket;
import helpdesk.entity.User;
import helpdesk.repository.TicketRepository;
import helpdesk.repository.UserRepository;
import helpdesk.service.EmailService;
import helpdesk.service.TicketService;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired private TicketRepository ticketRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private TicketService ticketService;
    @Autowired private EmailService emailService;
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
    	model.addAttribute("totalTickets", ticketRepo.count());
    	model.addAttribute("escalatedTickets", ticketService.getEscalatedTickets());
model.addAttribute("resolvedTickets", ticketRepo.countByStatus("Resolved"));
        model.addAttribute("openTickets", ticketRepo.countByStatus("Open"));
        model.addAttribute("inProgressTickets", ticketRepo.countByStatus("In Progress"));
        model.addAttribute("closedTickets", ticketRepo.countByStatus("Closed"));
        return "admin/dashboard";    }

    @GetMapping("/tickets")
    public String viewAllTickets(Model model) {
        model.addAttribute("tickets", ticketRepo.findAll());
        model.addAttribute("supportUsers", userRepo.findAllByRole("SUPPORT"));
        return "admin/view-tickets";
    }

    @PostMapping("/assign")
    public String assignTicket(@RequestParam Long ticketId, @RequestParam Long userId) {
        Ticket ticket = ticketRepo.findById(ticketId).orElseThrow();
        User support = userRepo.findById(userId).orElseThrow();
        ticket.setAssignedTo(support);
        ticketRepo.save(ticket);
        emailService.sendAssignmentEmail(ticket);
        return "redirect:/admin/tickets";
    }

    @GetMapping("/manage-users")
    public String manageUsers(Model model) {
        model.addAttribute("users", userRepo.findAll());
        model.addAttribute("user", new User());
        return "admin/manage-users";
    }
    
    @PostMapping("/users/update")
    public String updateUser(@RequestParam Long id,
                             @RequestParam String name,
                             @RequestParam String email,
                             @RequestParam String role) {

        User existingUser = userRepo.findById(id).orElseThrow();

        existingUser.setName(name);
        existingUser.setEmail(email);
        existingUser.setRole(role);

        userRepo.save(existingUser); // Now it's an update, not insert

        return "redirect:/admin/manage-users";
    }


    @GetMapping("/users/delete/{id}")
    public String deleteUser(@PathVariable Long id) {
    	User user = userRepo.findById(id).orElseThrow();
        
        // Unassign tickets
        List<Ticket> tickets = ticketRepo.findByAssignedTo(user);
        for (Ticket t : tickets) {
            t.setAssignedTo(null);
        }
        ticketRepo.saveAll(tickets);
        
        // Delete user
    	userRepo.deleteById(id);
        return "redirect:/admin/manage-users";
    }
    @GetMapping("/analytics")
    public String viewAnalytics(
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,

            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,

            Model model) {

        LocalDate today = LocalDate.now();
        if (startDate == null) startDate = today.minusMonths(1);
        if (endDate == null) endDate = today;

        List<Ticket> ticketsInRange = ticketRepo.findAllByCreatedAtBetween(startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());

     // *** ADD THIS SECTION ***
        // Calculate the current SLA status for each ticket in the range
        // This is crucial for getting up-to-the-minute analytics
        for (Ticket ticket : ticketsInRange) {
            ticketService.updateSlaStatus(ticket); // We will add this new, efficient method below
        }
        
        long open = ticketsInRange.stream().filter(t -> t.getStatus().equalsIgnoreCase("Open")).count();
        long inProgress = ticketsInRange.stream().filter(t -> t.getStatus().equalsIgnoreCase("In Progress")).count();
        long escalated = ticketsInRange.stream().filter(t -> t.getStatus().equalsIgnoreCase("Escalated")).count();
        long resolved = ticketsInRange.stream().filter(t -> t.getStatus().equalsIgnoreCase("Resolved")).count();
        long closed = ticketsInRange.stream().filter(t -> t.getStatus().equalsIgnoreCase("Closed")).count();

     // *** ADD THIS SECTION ***
        // Now, count the SLA statuses from the updated list
        long withinSlaCount = ticketsInRange.stream()
                .filter(t -> "Within SLA".equalsIgnoreCase(t.getSlaStatus())).count();
        long breachedSlaCount = ticketsInRange.stream()
                .filter(t -> "Breached".equalsIgnoreCase(t.getSlaStatus())).count();

        
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("openTickets", open);
        model.addAttribute("inProgressTickets", inProgress);
        model.addAttribute("escalatedTickets", escalated);
        model.addAttribute("resolvedTickets", resolved);
        model.addAttribute("closedTickets", closed);

     // *** ADD THIS SECTION ***
        // Add the SLA counts to the model for the frontend chart
        model.addAttribute("withinSlaCount", withinSlaCount);
        model.addAttribute("breachedSlaCount", breachedSlaCount);

        
        return "admin/analytics";
    }

}
