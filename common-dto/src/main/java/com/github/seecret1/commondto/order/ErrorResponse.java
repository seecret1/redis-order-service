package com.github.seecret1.commondto.order;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(

    Integer status,

    String message,

    Map<String, String> errors,

    Instant timestamp

) { }
