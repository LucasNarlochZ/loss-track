package com.lucasnarloch.loss_track.domain.auth.exceptions;

public class InactiveAccount extends RuntimeException {
    public InactiveAccount() {
        super("User or tenant is inactive");
    }
}
