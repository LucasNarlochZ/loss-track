package com.lucasnarloch.loss_track.user;

import com.lucasnarloch.loss_track.domain.user.dtos.UserResponseDTO;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;

class UserResponseDTOSecurityTest {

    @Test
    void responseShouldNotExposePasswordHash() {
        boolean exposesPasswordHash = Arrays.stream(UserResponseDTO.class.getRecordComponents())
                .anyMatch(component -> component.getName().equals("passwordHash"));

        assertFalse(exposesPasswordHash);
    }
}
