package com.lucasnarloch.loss_track.domain.auth.exceptions;

public class CnpjAlreadyRegistered extends RuntimeException {
    public CnpjAlreadyRegistered() {
        super("CNPJ is already registered");
    }
}
