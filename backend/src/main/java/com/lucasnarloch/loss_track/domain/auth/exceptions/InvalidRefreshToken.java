package com.lucasnarloch.loss_track.domain.auth.exceptions;

public class InvalidRefreshToken extends RuntimeException {
    public InvalidRefreshToken() {
        super("Invalid refresh token");
    }
}
