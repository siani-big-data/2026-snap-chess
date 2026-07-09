package com.chessdigitizer.backend.domain.port.out;

import java.util.UUID;

public interface CurrentUserPort {
    UUID getCurrentUserId();
}