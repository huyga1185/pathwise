package uk.huy.pathwise.planning.dto.response;

import org.jspecify.annotations.Nullable;
import uk.huy.pathwise.fleet.model.CapacityUnit;

import java.util.Map;

/**
 * A stop read from an import file, shaped like a stop of {@code PlanDraftRequest} so that it can be sent back as is.
 *
 * @param recipientPhone E.164 when the number is valid, otherwise as written in the file
 * @param demands        only the units with a quantity in the file
 */
public record ImportedStopResponse(@Nullable String label,
                                   @Nullable String recipientName,
                                   @Nullable String recipientPhone,
                                   @Nullable String address,
                                   @Nullable Double lat,
                                   @Nullable Double lon,
                                   Map<CapacityUnit, Integer> demands,
                                   @Nullable Integer serviceTimeSeconds,
                                   @Nullable TimeWindowResponse timeWindow,
                                   @Nullable String note) {}
