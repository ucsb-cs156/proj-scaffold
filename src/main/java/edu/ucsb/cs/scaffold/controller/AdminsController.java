package edu.ucsb.cs.scaffold.controller;

import edu.ucsb.cs.scaffold.entity.Admin;
import edu.ucsb.cs.scaffold.entity.Instructor;
import edu.ucsb.cs.scaffold.entity.User;
import edu.ucsb.cs.scaffold.errors.EntityNotFoundException;
import edu.ucsb.cs.scaffold.repository.AdminRepository;
import edu.ucsb.cs.scaffold.repository.InstructorRepository;
import edu.ucsb.cs.scaffold.repository.UserRepository;
import edu.ucsb.cs.scaffold.utilities.CanonicalFormConverter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.StreamSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin")
@RequestMapping("/api/admin")
@RestController
@Slf4j
public class AdminsController extends ApiController {

  @Autowired AdminRepository adminRepository;
  @Autowired InstructorRepository instructorRepository;
  @Autowired UserRepository userRepository;

  @Value("#{'${app.admin.emails}'.split(',')}")
  List<String> adminEmails;

  public static record AdminDTO(String email, boolean isInAdminEmails) {
    public AdminDTO(Admin admin, List<String> adminEmails) {
      this(admin.getEmail(), adminEmails.contains(admin.getEmail()));
    }
  }

  public static record UserDTO(
      long id,
      String givenName,
      String familyName,
      String email,
      boolean admin,
      boolean instructor) {
    public UserDTO(User user, Set<String> adminEmails, Set<String> instructorEmails) {
      this(
          user.getId(),
          user.getGivenName(),
          user.getFamilyName(),
          user.getEmail(),
          adminEmails.contains(user.getEmail()),
          instructorEmails.contains(user.getEmail()));
    }
  }

  @Operation(summary = "Create a new admin")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  @PostMapping("/post")
  public Admin postAdmin(@Parameter(name = "email") @RequestParam String email) {
    String convertedEmail = CanonicalFormConverter.convertToValidEmail(email).strip();
    Admin admin = new Admin(convertedEmail);
    return adminRepository.save(admin);
  }

  @Operation(summary = "List all admins")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  @GetMapping("/all")
  public Iterable<AdminDTO> allAdmins() {
    Iterable<Admin> admins = adminRepository.findAll();
    return StreamSupport.stream(admins.spliterator(), false)
        .map(admin -> new AdminDTO(admin, adminEmails))
        .toList();
  }

  @Operation(summary = "List all users")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  @GetMapping("/users")
  public Iterable<UserDTO> allUsers() {
    Set<String> adminEmailSet = new HashSet<>(adminEmails);
    StreamSupport.stream(adminRepository.findAll().spliterator(), false)
        .map(Admin::getEmail)
        .forEach(adminEmailSet::add);

    Set<String> instructorEmailSet =
        StreamSupport.stream(instructorRepository.findAll().spliterator(), false)
            .map(Instructor::getEmail)
            .collect(java.util.stream.Collectors.toSet());

    return userRepository.findAll().stream()
        .sorted(Comparator.comparingLong(User::getId))
        .map(user -> new UserDTO(user, adminEmailSet, instructorEmailSet))
        .toList();
  }

  @Operation(summary = "Delete an Admin")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  @DeleteMapping("/delete")
  public Object deleteAdmin(@Parameter(name = "email") @RequestParam String email) {
    Admin admin =
        adminRepository
            .findByEmail(email)
            .orElseThrow(() -> new EntityNotFoundException(Admin.class, email));
    if (adminEmails.contains(email)) {
      throw new UnsupportedOperationException(
          "Forbidden to delete an admin from ADMIN_EMAILS list");
    }
    adminRepository.delete(admin);
    return genericMessage("Admin with id %s deleted".formatted(email));
  }
}
