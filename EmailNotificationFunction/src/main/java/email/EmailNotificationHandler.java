package email;

import java.util.List;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;

@Slf4j
public class EmailNotificationHandler implements RequestHandler<SNSEvent, Void> {

    private final SesClient sesClient;
    private final ObjectMapper objectMapper;
    private final String fromEmail;
    private final String toEmail;

    public EmailNotificationHandler() {
        this.sesClient = SesClient.builder().build();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
        this.fromEmail = System.getenv("FROM_EMAIL");
        this.toEmail = System.getenv("TO_EMAIL");
    }

    @Override
    public Void handleRequest(SNSEvent event, Context context) {
        log.info("EmailNotification function started - Request ID: {}", context.getAwsRequestId());

        for (SNSEvent.SNSRecord record : event.getRecords()) {
            try {
                String message = record.getSNS().getMessage();
                log.info("Processing SNS message: {}", message);

                BookResponse book = objectMapper.readValue(message, BookResponse.class);
                sendBookCreatedEmail(book);

            } catch (Exception e) {
                log.error("Error processing SNS message", e);
            }
        }

        return null;
    }

    private void sendBookCreatedEmail(BookResponse book) {
        try {
            String subject = "New Book Added: " + book.getTitle();
            String htmlBody = buildEmailBody(book);

            SendEmailRequest emailRequest = SendEmailRequest.builder()
                    .source(fromEmail)
                    .destination(Destination.builder()
                            .toAddresses(toEmail)
                            .build())
                    .message(Message.builder()
                            .subject(Content.builder()
                                    .data(subject)
                                    .build())
                            .body(Body.builder()
                                    .html(Content.builder()
                                            .data(htmlBody)
                                            .build())
                                    .build())
                            .build())
                    .build();

            sesClient.sendEmail(emailRequest);
            log.info("Email sent successfully for book: {}", book.getTitle());

        } catch (Exception e) {
            log.error("Failed to send email for book: {}", book.getTitle(), e);
        }
    }

    private String buildEmailBody(BookResponse book) {
        return "<html><body>" +
                "<h2>New Book Successfully Added!</h2>" +
                "<p><strong>Title:</strong> " + book.getTitle() + "</p>" +
                "<p><strong>Author:</strong> " + book.getAuthor() + "</p>" +
                "<p><strong>Publication Year:</strong> " + book.getPublicationYear() + "</p>" +
                "<p><strong>Language:</strong> " + book.getLanguage() + "</p>" +
                "<p><strong>Format:</strong> " + book.getFormat() + "</p>" +
                "<p><strong>Finish Date:</strong> " + book.getFinishDate() + "</p>" +
                "<p><strong>Read Year:</strong> " + book.getReadYear() + "</p>" +
                "<p><strong>Reading Time:</strong> " + book.getReadingTimeInDays() + " days</p>" +
                "<p><strong>Book ID:</strong> " + book.getId() + "</p>" +
                "</body></html>";
    }
}