package org.azelabs.boxshare.controllers;

import jakarta.persistence.EntityNotFoundException;
import org.azelabs.boxshare.application.CommandInvoker;
import org.azelabs.boxshare.application.enums.CommandType;
import org.azelabs.boxshare.dtos.UserRecord;
import org.azelabs.boxshare.models.HybridUser;
import org.azelabs.boxshare.services.interfaces.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.Optional;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final CommandInvoker invoker;
    private final IUserService userService;

    @Autowired
    public UserController(CommandInvoker invoker, IUserService userService) {
        this.invoker = invoker;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserRecord> createUser(@RequestBody UserRecord user) {
        UserRecord response = this.invoker.execute(CommandType.CREATE_USER.getName(), user);
        return ResponseEntity.status(202).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserRecord> getUserById(@PathVariable Long id) {
        Optional<UserRecord> user = this.invoker.execute(CommandType.GET_USER.getName(), id);
        return user.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping
    public ResponseEntity<UserRecord> updateUser(@RequestBody UserRecord user) {
        Optional<UserRecord> updatedUser = this.invoker.execute(CommandType.UPDATE_USER.getName(), user);
        return updatedUser.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.badRequest().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Boolean> deleteUser(@PathVariable Long id) {
        Boolean deleted = this.invoker.execute(CommandType.DELETE_USER.getName(), id);
        return ResponseEntity.ok(deleted);
    }

    @PostMapping("/verify/{token}")
    public ResponseEntity<UserRecord> verifyUserEmail(@PathVariable UUID token) {
        UserRecord verifiedUser = this.invoker.execute(CommandType.VERIFY_USER_EMAIL.getName(), token);
        return ResponseEntity.ok(verifiedUser);
    }

    @PostMapping("/verify/resend")
    public ResponseEntity<Void> resendVerificationEmail(@AuthenticationPrincipal HybridUser principal) {
        userService.resendVerificationEmail(principal.getUser().getEmail());
        return ResponseEntity.ok().build();
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Void> handleNotFound() {
        return ResponseEntity.notFound().build();
    }
}
