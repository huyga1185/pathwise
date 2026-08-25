package uk.huy.pathwise.auth.dto.response;

import uk.huy.pathwise.auth.token.opaque.refreshtoken.dto.RefreshTokenDetail;

import java.util.List;

public record GetListSessionResponse(List<RefreshTokenDetail> listSession) {}
