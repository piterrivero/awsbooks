package books;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

@Slf4j
public class GetAllBooksHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final DynamoDbEnhancedClient enhancedClient;
    private final DynamoDbTable<Book> bookTable;
    private final ObjectMapper objectMapper;
    private final String tableName;

    public GetAllBooksHandler() {
        this.tableName = System.getenv("TABLE_NAME");
        DynamoDbClient ddbClient = DynamoDbClient.builder().build();
        this.enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(ddbClient)
                .build();
        this.bookTable = enhancedClient.table(tableName, TableSchema.fromBean(Book.class));
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
        this.objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        log.info("GetAllBooks function started - Request ID: {}", context.getAwsRequestId());
        
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Access-Control-Allow-Origin", "*");

        try {
            log.info("Scanning DynamoDB table: {}", tableName);
            
            List<BookResponse> books = bookTable.scan()
                    .items()
                    .stream()
                    .map(this::convertToBookResponse)
                    .sorted((b1, b2) -> Integer.compare(b1.getId(), b2.getId()))
                    .collect(Collectors.toList());
            
            log.info("Found {} books in database", books.size());
            
            String jsonResponse = objectMapper.writeValueAsString(books);
            
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withHeaders(headers)
                    .withBody(jsonResponse);
                    
        } catch (Exception e) {
            log.error("Error retrieving books from DynamoDB", e);
            
            String errorResponse = "{\"error\": \"Failed to retrieve books\", \"message\": \"" + e.getMessage() + "\"}";
            
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withHeaders(headers)
                    .withBody(errorResponse);
        }
    }
    
    private BookResponse convertToBookResponse(Book book) {
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
        return response;
    }
}