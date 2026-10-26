package uk.huy.pathwise.store.dto.response;

public record StoreResponse(long id,
                            String name,
                            String depotAddress,
                            Double depotLat,
                            Double depotLon,
                            String timezone) {}
