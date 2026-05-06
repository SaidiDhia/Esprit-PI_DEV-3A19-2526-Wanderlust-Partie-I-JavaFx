package com.example.pi_dev;

import com.example.pi_dev.enums.RoleEnum;
import com.example.pi_dev.Entities.Users.User;
import com.example.pi_dev.Services.Users.UserService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

public class UserCrudConsole {

    private static final UserService userService = new UserService();
    private static final Scanner scanner = new Scanner(System.in);
    private static User currentUser = null;

    public static void main(String[] args) {
        System.out.println("=== User Management Console ===");

        // 1. Login
        while (currentUser == null) {
            System.out.println("\nPlease Login:");
            System.out.print("Email: ");
            String email = scanner.nextLine();
            System.out.print("Password: ");
            String password = scanner.nextLine();

            String token = userService.login(email, password);
            if (token != null) {
                // Find the user object (Login only returns token usually, but we need role)
                // In a real app, we'd parse the JWT. Here we'll just fetch from DB by email.
                currentUser = userService.getAllUsers().stream()
                        .filter(u -> u.getEmail().equals(email))
                        .findFirst()
                        .orElse(null);
                
                if (currentUser != null) {
                    System.out.println("Login Successful! Welcome, " + currentUser.getFullName() + " (" + currentUser.getRole() + ")");
                } else {
                    System.out.println("Error fetching user details.");
                }
            } else {
                System.out.println("Invalid credentials. Try again.");
            }
        }

        // 2. Menu Loop
        boolean running = true;
        while (running) {
            printMenu();
            String choice = scanner.nextLine();

            switch (choice) {
                case "1":
                    listUsers();
                    break;
                case "2":
                    addUser();
                    break;
                case "3":
                    updateUser();
                    break;
                case "4":
                    deleteUser();
                    break;
                case "5":
                    running = false;
                    System.out.println("Exiting...");
                    break;
                default:
                    System.out.println("Invalid option.");
            }
        }
        
        scanner.close();
    }

    private static void printMenu() {
        System.out.println("\n--- Menu ---");
        System.out.println("1. List All Users");
        System.out.println("2. Add New User");
        System.out.println("3. Update User");
        System.out.println("4. Delete User (Admin Only)");
        System.out.println("5. Exit");
        System.out.print("Select an option: ");
    }

    private static void listUsers() {
        System.out.println("\n--- User List ---");
        List<User> users = userService.getAllUsers();
        if (users == null || users.isEmpty()) {
            System.out.println("No users found.");
        } else {
            System.out.printf("%-36s | %-25s | %-15s | %-10s | %-8s%n", "ID", "Email", "Name", "Role", "Active");
            System.out.println("------------------------------------------------------------------------------------------------------------");
            for (User user : users) {
                System.out.printf("%-36s | %-25s | %-15s | %-10s | %-8s%n", 
                        user.getUserId(), 
                        user.getEmail(), 
                        user.getFullName(), 
                        user.getRole(),
                        user.getIsActive());
            }
        }
    }

    private static void addUser() {
        System.out.println("\n--- Add User ---");
        User newUser = new User();
        newUser.setUserId(UUID.randomUUID());
        
        System.out.print("Full Name: ");
        newUser.setFullName(scanner.nextLine());
        
        System.out.print("Email: ");
        newUser.setEmail(scanner.nextLine());
        
        System.out.print("Password: ");
        newUser.setPasswordHash(scanner.nextLine()); // Service will hash it? Wait, register hashes it.
        
        System.out.print("Phone: ");
        newUser.setPhoneNumber(scanner.nextLine());
        
        System.out.print("Role (ADMIN/HOST/PARTICIPANT): ");
        try {
            newUser.setRole(RoleEnum.valueOf(scanner.nextLine().toUpperCase()));
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid Role. Defaulting to PARTICIPANT.");
            newUser.setRole(RoleEnum.PARTICIPANT);
        }
        
        newUser.setIsActive(true);
        newUser.setCreatedAt(LocalDateTime.now());

        try {
            userService.register(newUser); // Register method hashes password
            System.out.println("User added successfully.");
        } catch (Exception e) {
            System.out.println("Error adding user: " + e.getMessage());
        }
    }

    private static void updateUser() {
        System.out.println("\n--- Update User ---");
        System.out.print("Enter User ID to update: ");
        String idStr = scanner.nextLine();
        
        try {
            UUID userId = UUID.fromString(idStr);
            User user = userService.getUserById(userId);
            
            if (user == null) {
                System.out.println("User not found.");
                return;
            }
            
            System.out.println("Updating user: " + user.getFullName());
            System.out.println("Press Enter to keep current value.");
            
            System.out.print("New Name (" + user.getFullName() + "): ");
            String name = scanner.nextLine();
            if (!name.isEmpty()) user.setFullName(name);
            
            System.out.print("New Email (" + user.getEmail() + "): ");
            String email = scanner.nextLine();
            if (!email.isEmpty()) user.setEmail(email);
            
            System.out.print("New Phone (" + user.getPhoneNumber() + "): ");
            String phone = scanner.nextLine();
            if (!phone.isEmpty()) user.setPhoneNumber(phone);
            
            System.out.print("New Role (" + user.getRole() + "): ");
            String roleStr = scanner.nextLine();
            if (!roleStr.isEmpty()) {
                try {
                    user.setRole(RoleEnum.valueOf(roleStr.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    System.out.println("Invalid Role. Keeping current.");
                }
            }

            userService.updateUser(user);
            System.out.println("User updated successfully.");

        } catch (IllegalArgumentException e) {
            System.out.println("Invalid UUID format.");
        }
    }

    private static void deleteUser() {
        System.out.println("\n--- Delete User ---");
        
        // 1. Check Permission
        if (currentUser.getRole() != RoleEnum.ADMIN) {
            System.out.println("PERMISSION DENIED: Only Admins can delete users.");
            return;
        }
        
        System.out.print("Enter User ID to delete: ");
        String idStr = scanner.nextLine();
        
        try {
            UUID userId = UUID.fromString(idStr);
            
            // Optional: Check if user exists first
            User userToDelete = userService.getUserById(userId);
            if (userToDelete == null) {
                System.out.println("User not found.");
                return;
            }
            
            System.out.print("Are you sure you want to delete " + userToDelete.getFullName() + "? (yes/no): ");
            String confirm = scanner.nextLine();
            
            if (confirm.equalsIgnoreCase("yes")) {
                userService.deleteUser(userId);
                System.out.println("User deleted successfully.");
            } else {
                System.out.println("Deletion cancelled.");
            }
            
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid UUID format.");
        }
    }
}