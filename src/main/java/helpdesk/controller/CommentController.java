package helpdesk.controller;

import helpdesk.entity.Comment;
import helpdesk.entity.Ticket;
import helpdesk.entity.User;
import helpdesk.repository.CommentRepository;
import helpdesk.repository.TicketRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/tickets")
public class CommentController {

    @Autowired
    private CommentRepository commentRepo;

    @Autowired
    private TicketRepository ticketRepo;

    @PostMapping("/{id}/comments")
    public String addComment(@PathVariable Long id, @RequestParam("message") String message, HttpSession session) {
        Ticket ticket = ticketRepo.findById(id).orElseThrow();
        User user = (User) session.getAttribute("loggedUser");

        Comment comment = new Comment();
        comment.setTicket(ticket);
        comment.setUser(user);
        comment.setMessage(message);
        comment.setCreatedAt(LocalDateTime.now());

        commentRepo.save(comment);
        return "redirect:/tickets/ticket-view";

   
   }
    
}
