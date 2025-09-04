package helpdesk.controller;

import helpdesk.entity.User;
import helpdesk.repository.UserRepository;
import helpdesk.service.StorageService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.UUID;
@Controller
public class AuthController {

    @Autowired private UserRepository userRepo;
    @Autowired private JavaMailSender mailSender;
@Autowired private StorageService storageService; 
    // Helper method to create and save a verification token
    private void createVerificationToken(User user) {
        String token = UUID.randomUUID().toString();
        user.setVerificationToken(token);
        user.setTokenExpiry(LocalDateTime.now().plusHours(24));  // token valid for 24 hours
        userRepo.save(user);
    }

    // Helper to send verification email
    private void sendVerificationEmail(String toEmail, String token) {
        String subject = "Please Verify Your Email";
        String verificationUrl = "http://localhost:8080/verify-email?token=" + token;

        String message = "Click this link to verify your email:\n\n" + verificationUrl;

        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setTo(toEmail);
        mail.setSubject(subject);
        mail.setText(message);
        mailSender.send(mail);
    }
    @GetMapping("/")
    public String showRegistrationAsHomePage(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }
    
    @GetMapping("/login")
    public String showLogin() {
        return "login";
    }

    @PostMapping("/login")
    public String loginUser(@RequestParam String email,
                            @RequestParam String password,
                            HttpSession session,
                            Model model) {

        User user = userRepo.findByEmail(email);

        if (user == null || !user.getPassword().equals(password)) {
            model.addAttribute("error", "Invalid email or password");
            return "login";
        }

        // Check if email verified
        if (!user.isEmailVerified()) {
            // Generate new verification token and email
            createVerificationToken(user);
            sendVerificationEmail(user.getEmail(), user.getVerificationToken());

            model.addAttribute("error", "Your email is not verified. We sent a verification link to your email.");
            return "login";
        }

        session.setAttribute("loggedUser", user);

        // Redirect based on role
        String role = user.getRole();
        switch (role.toUpperCase()) {
            case "ADMIN":
                return "redirect:/admin/dashboard";
            case "SUPPORT":
                return "redirect:/support/dashboard";
            case "USER":
                return "redirect:/user/dashboard";
            default:
                model.addAttribute("error", "Unknown role assigned");
                return "login";
        }
    }

    @GetMapping("/verify-email")
    public String verifyEmail(@RequestParam("token") String token, Model model) {
        User user = userRepo.findByVerificationToken(token);

        if (user == null || user.getTokenExpiry().isBefore(LocalDateTime.now())) {
            model.addAttribute("message", "Verification link is invalid or expired.");
            return "verification-failed"; // Create this Thymeleaf template
        }

        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setTokenExpiry(null);
        userRepo.save(user);

        model.addAttribute("message", "Email verified successfully! You can now login.");
        return "verification-success"; // Create this Thymeleaf template
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User());
        return "register"; // Refers to templates/register.html
    }

    @PostMapping("/register")
    public String processRegistration(@ModelAttribute("user") User user, Model model) {
        if (userRepo.findByEmail(user.getEmail()) != null) {
            model.addAttribute("error", "Email already registered.");
            return "register";
        }
        // Initially, email not verified
        user.setEmailVerified(false);
        userRepo.save(user);

        // Send verification email upon registration
        createVerificationToken(user);
        sendVerificationEmail(user.getEmail(), user.getVerificationToken());

        model.addAttribute("success", "Registration successful! Please verify your email before login.");
        return "redirect:/login";
    }

    @GetMapping("/profile")
    public String viewProfile(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedUser");
        if (user == null) return "redirect:/login";
        model.addAttribute("user", user);
        // Determine dashboard URL based on role
        String role = user.getRole().toUpperCase();
        String dashboardUrl = switch (role) {
            case "ADMIN" -> "/admin/dashboard";
            case "SUPPORT" -> "/support/dashboard";
            case "USER" -> "/user/dashboard";
            default -> "/login";
        };
        model.addAttribute("dashboardUrl", dashboardUrl);
        return "profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@RequestParam String name,
                                @RequestParam String email,
                                HttpSession session) {
        
        User sessionUser = (User) session.getAttribute("loggedUser");
        if (sessionUser == null) {
            return "redirect:/login";
        }

        // 1. Fetch the fresh user from the database
        User userToUpdate = userRepo.findById(sessionUser.getId()).orElse(null);
        if (userToUpdate == null) {
            session.invalidate();
            return "redirect:/login?error=userNotFound";
        }

        // 2. Apply changes to the fresh object
        userToUpdate.setName(name);
        userToUpdate.setEmail(email);

        // 3. Save the updated object
        User updatedUser = userRepo.save(userToUpdate);

        // 4. Update the session with the saved object
        session.setAttribute("loggedUser", updatedUser);

        return "redirect:/profile?success";
    }

    @PostMapping("/profile/picture/update")
    public String updateProfilePicture(@RequestParam("file") MultipartFile file,
                                       HttpSession session,
                                       Model model) {
                                           
        User sessionUser = (User) session.getAttribute("loggedUser");
        if (sessionUser == null) {
            return "redirect:/login";
        }

        // 1. Fetch the fresh user from the database
        User userToUpdate = userRepo.findById(sessionUser.getId()).orElse(null);
        if (userToUpdate == null) {
            session.invalidate();
            return "redirect:/login?error=userNotFound";
        }

        // 2. Check if the file is valid
        if (!file.isEmpty() && file.getContentType().startsWith("image")) {
            // 3. Store the file and get the path
            String filePath = storageService.store(file);
            
            // 4. Update the profile picture URL on the fresh object
            userToUpdate.setProfilePictureUrl(filePath);
            
            // 5. Save the updated object
            User updatedUser = userRepo.save(userToUpdate);
            
            // 6. Update the session with the saved object
            session.setAttribute("loggedUser", updatedUser);
        } else {
            model.addAttribute("error", "Please select a valid image file.");
            // Since we are redirecting, a better way to show errors is with RedirectAttributes
            // But for simplicity, this will work if you show the error on the profile page
        }

        return "redirect:/profile";
    }


}
