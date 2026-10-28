package uk.huy.pathwise.planning.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import uk.huy.pathwise.core.phonenumber.ValidPhoneNumber;
import uk.huy.pathwise.core.validation.FieldErrors;
import uk.huy.pathwise.core.validation.MergePatch;
import uk.huy.pathwise.fleet.model.CapacityUnit;

import java.util.Map;

/**
 * Partial change of one stop ({@code PATCH /plans/{id}/stops/{stopId}}), as a merge patch ({@link MergePatch}): an
 * absent field keeps its value, {@code null} clears it (no time window, no demand, service time 0, no coordinates and
 * the stop is geocoded again from its address). {@code label} cannot be cleared; {@code lat} and {@code lon} are sent
 * together.
 */
@Getter
public class PatchStopRequest extends MergePatch {
    @NotNull(message = "VERSION_REQUIRED")
    private Long version;

    @Size(max = 150, message = "INVALID_STOP_LABEL_LENGTH")
    private String label;

    @Size(max = 100, message = "INVALID_RECIPIENT_NAME_LENGTH")
    private String recipientName;

    @ValidPhoneNumber
    private String recipientPhone;

    @Size(max = 500, message = "INVALID_ADDRESS_LENGTH")
    private String address;

    @DecimalMin(value = "-90", message = "INVALID_LATITUDE")
    @DecimalMax(value = "90", message = "INVALID_LATITUDE")
    private Double lat;

    @DecimalMin(value = "-180", message = "INVALID_LONGITUDE")
    @DecimalMax(value = "180", message = "INVALID_LONGITUDE")
    private Double lon;

    private Map<CapacityUnit, @NotNull(message = "INVALID_DEMAND") @PositiveOrZero(message = "INVALID_DEMAND") Integer> demands;

    @PositiveOrZero(message = "INVALID_SERVICE_TIME")
    @Max(value = 86_400, message = "INVALID_SERVICE_TIME")
    private Integer serviceTimeSeconds;

    @Valid
    private TimeWindowRequest timeWindow;

    @Size(max = 500, message = "INVALID_NOTE_LENGTH")
    private String note;

    @Override
    public void validate(FieldErrors errors) {
        rejectNull(errors, "label", label);
        if (label != null && label.isBlank()) errors.reject("label", "STOP_LABEL_REQUIRED");
        if (has("lat") != has("lon") || (lat == null) != (lon == null))
            errors.reject(has("lat") && lat != null ? "lon" : "lat", "COORDINATES_INCOMPLETE");
        if (demands != null && demands.containsKey(CapacityUnit.ORDER)) errors.reject("demands", "ORDER_DEMAND_NOT_ALLOWED");
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public void setLabel(String label) {
        this.label = label;
        present("label");
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
        present("recipientName");
    }

    public void setRecipientPhone(String recipientPhone) {
        this.recipientPhone = recipientPhone;
        present("recipientPhone");
    }

    public void setAddress(String address) {
        this.address = address;
        present("address");
    }

    public void setLat(Double lat) {
        this.lat = lat;
        present("lat");
    }

    public void setLon(Double lon) {
        this.lon = lon;
        present("lon");
    }

    public void setDemands(Map<CapacityUnit, Integer> demands) {
        this.demands = demands;
        present("demands");
    }

    public void setServiceTimeSeconds(Integer serviceTimeSeconds) {
        this.serviceTimeSeconds = serviceTimeSeconds;
        present("serviceTimeSeconds");
    }

    public void setTimeWindow(TimeWindowRequest timeWindow) {
        this.timeWindow = timeWindow;
        present("timeWindow");
    }

    public void setNote(String note) {
        this.note = note;
        present("note");
    }
}
