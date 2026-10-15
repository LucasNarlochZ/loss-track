package com.lucasnarloch.loss_track.domain.auth.exceptions;

public class InvalidCredentials extends RuntimeException {
    public InvalidCredentials() {
        super("Invalid credentials");
    }
}
