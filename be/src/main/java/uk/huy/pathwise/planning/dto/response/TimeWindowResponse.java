package uk.huy.pathwise.planning.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalTime;

public record TimeWindowResponse(@JsonFormat(pattern = "HH:mm") LocalTime start,
                                 @JsonFormat(pattern = "HH:mm") LocalTime end) {}
