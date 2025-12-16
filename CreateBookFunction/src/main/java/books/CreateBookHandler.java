package books;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

@Slf4j
public class CreateBookHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final DynamoDbEnhancedClient enhancedClient;
    private final DynamoDbTable<Book> bookTable;
    private final ObjectMapper objectMapper;
    private final String tableName;
    private final SnsClient snsClient;
    private final String topicArn;

    public CreateBookHandler() {
        this.tableName = System.getenv("TABLE_NAME");
        this.topicArn = System.getenv("BOOK_NOTIFICATIONS_TOPIC_ARN");
        DynamoDbClient ddbClient = DynamoDbClient.builder().build();
        this.enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(ddbClient)
                .build();
        this.bookTable = enhancedClient.table(tableName, TableSchema.fromBean(Book.class));
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
        this.objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.snsClient = SnsClient.builder().build();
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        log.info("CreateBook function started - Request ID: {}", context != null ? context.getAwsRequestId() : "test");
        
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Access-Control-Allow-Origin", "*");

        try {
            // Parse request body
            BookRequest bookRequest = objectMapper.readValue(input.getBody(), BookRequest.class);
            
            // Create Book object
            Book book = new Book();
            
            // Auto-generate ID (next available ID)
            Integer nextId = getNextId();
            book.setId(nextId);
            
            // Set basic fields from request
            book.setTitle(bookRequest.getTitle());
            book.setAuthor(bookRequest.getAuthor());
            book.setPublicationYear(bookRequest.getPublicationYear());
            book.setLanguage(bookRequest.getLanguage());
            book.setFormat(bookRequest.getFormat());
            
            // Auto-set finish date to today
            LocalDate currentDate = LocalDate.now();
            book.setFinishDate(currentDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
            book.setReadYear(currentDate.getYear());
            
            // Calculate reading time based on last book's finish date
            int readingTimeInDays = calculateReadingTime(currentDate);
            book.setReadingTimeInDays(readingTimeInDays);
            
            log.info("Creating book: {} by {} with ID: {}", book.getTitle(), book.getAuthor(), book.getId());
            
            // Save to DynamoDB
            bookTable.putItem(book);
            
            log.info("Book created successfully with ID: {}", book.getId());
            
            // Convert to BookResponse
            BookResponse response = new BookResponse();
            response.setId(book.getId());
            response.setTitle(book.getTitle());
            response.setAuthor(book.getAuthor());
            response.setPublicationYear(book.getPublicationYear());
            response.setLanguage(book.getLanguage());
            response.setFormat(book.getFormat());
            response.setFinishDate(LocalDate.parse(book.getFinishDate()));
            response.setReadYear(book.getReadYear());
            response.setReadingTimeInDays(book.getReadingTimeInDays());
            
            // Send notification to SNS topic
            sendBookNotification(response);
            
            String jsonResponse = objectMapper.writeValueAsString(response);
            
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(201)
                    .withHeaders(headers)
                    .withBody(jsonResponse);
                    
        } catch (Exception e) {
            log.error("Error creating book", e);
            
            String errorResponse = "{\"error\": \"Failed to create book\", \"message\": \"" + e.getMessage() + "\"}";
            
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withHeaders(headers)
                    .withBody(errorResponse);
        }
    }
    
    private Integer getNextId() {
        List<Book> allBooks = bookTable.scan().items().stream().collect(Collectors.toList());
        return allBooks.stream()
                .mapToInt(Book::getId)
                .max()
                .orElse(0) + 1;
    }
    
    private int calculateReadingTime(LocalDate currentDate) {
        List<Book> allBooks = bookTable.scan().items().stream().collect(Collectors.toList());
        
        Optional<Book> lastBook = allBooks.stream()
                .max((b1, b2) -> Integer.compare(b1.getId(), b2.getId()));
        
        if (lastBook.isPresent() && lastBook.get().getFinishDate() != null) {
            try {
                LocalDate lastFinishDate = LocalDate.parse(lastBook.get().getFinishDate());
                long daysBetween = ChronoUnit.DAYS.between(lastFinishDate, currentDate);
                return (int) daysBetween;
            } catch (Exception e) {
                log.warn("Error parsing last book finish date, defaulting to 0: {}", e.getMessage());
                return 0;
            }
        }
        
        return 0; // Default for first book
    }
    
    private void sendBookNotification(BookResponse book) {
        try {
            String message = objectMapper.writeValueAsString(book);
            
            PublishRequest publishRequest = PublishRequest.builder()
                    .topicArn(topicArn)
                    .message(message)
                    .subject("New Book Created: " + book.getTitle())
                    .build();
            
            snsClient.publish(publishRequest);
            log.info("Notification sent to SNS topic for book: {}", book.getTitle());
        } catch (Exception e) {
            log.error("Failed to send SNS notification for book: {}", book.getTitle(), e);
        }
    }
}