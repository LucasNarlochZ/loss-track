package com.lucasnarloch.loss_track.exceptions;

import com.lucasnarloch.loss_track.domain.auth.exceptions.CnpjAlreadyRegistered;
import com.lucasnarloch.loss_track.domain.auth.exceptions.InactiveAccount;
import com.lucasnarloch.loss_track.domain.auth.exceptions.InvalidCredentials;
import com.lucasnarloch.loss_track.domain.auth.exceptions.InvalidRefreshToken;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationException(
            MethodArgumentNotValidException ex
    ) {
        Map<String, String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(
                        java.util.stream.Collectors.toMap(
                                FieldError::getField,
                                DefaultMessageSourceResolvable::getDefaultMessage
                        )
                );

        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFoundException(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler({InvalidCredentials.class, InvalidRefreshToken.class})
    public ResponseEntity<Map<String, String>> handleAuthenticationException(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(InactiveAccount.class)
    public ResponseEntity<Map<String, String>> handleInactiveAccount(InactiveAccount ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(CnpjAlreadyRegistered.class)
    public ResponseEntity<Map<String, String>> handleConflict(CnpjAlreadyRegistered ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", ex.getMessage()));
    }
}
