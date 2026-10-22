package uk.huy.pathwise.auth.email.infrastructure.emailcontext;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class EmailContext {
    protected final String from;
    protected final String name;
    protected final String appName;
    protected final String appUrl;
    protected final String companyName;
    protected final String companyUrl;
    protected final String companyAddress;
}
