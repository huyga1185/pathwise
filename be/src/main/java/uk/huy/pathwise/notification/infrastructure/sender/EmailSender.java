package uk.huy.pathwise.notification.infrastructure.sender;

import java.util.List;

public interface EmailSender {
    /**
     * @param inlineImages images the HTML shows by {@code cid:}; empty for none
     */
    void send(String from, String name, String to, String subject, String content, boolean isHtml,
              List<InlineImage> inlineImages);
}
