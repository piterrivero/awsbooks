package books;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;

import lombok.extern.slf4j.Slf4j;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@Slf4j
public class ScheduledBackupHandler implements RequestHandler<ScheduledEvent, Void> {

    private final DynamoDbEnhancedClient enhancedClient;
    private final DynamoDbTable<Book> bookTable;
    private final AmazonS3 s3Client;
    private final String tableName;
    private final String bucketName;

    public ScheduledBackupHandler() {
        this.tableName = System.getenv("TABLE_NAME");
        this.bucketName = System.getenv("BACKUP_BUCKET_NAME");
        
        DynamoDbClient ddbClient = DynamoDbClient.builder().build();
        this.enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(ddbClient)
                .build();
        this.bookTable = enhancedClient.table(tableName, TableSchema.fromBean(Book.class));
        this.s3Client = AmazonS3ClientBuilder.standard()
                .withRegion(Regions.EU_CENTRAL_1)
                .build();
    }

    @Override
    public Void handleRequest(ScheduledEvent event, Context context) {
        log.info("ScheduledBackup function started - Request ID: {}", context.getAwsRequestId());
        
        try {
            // Scan all books ordered by ID
            List<Book> books = bookTable.scan()
                    .items()
                    .stream()
                    .sorted((b1, b2) -> Integer.compare(b1.getId(), b2.getId()))
                    .collect(Collectors.toList());
            
            log.info("Found {} books to backup", books.size());
            
            // Create backup content in CSV format
            StringBuilder backupContent = new StringBuilder();
            
            // Header row
            backupContent.append("id;title;author;publicationYear;language;format;finishDate;readYear;readingTimeInDays\n");
            
            // Data rows
            for (Book book : books) {
                backupContent.append(book.getId()).append(";")
                            .append(book.getTitle() != null ? book.getTitle() : "").append(";")
                            .append(book.getAuthor() != null ? book.getAuthor() : "").append(";")
                            .append(book.getPublicationYear()).append(";")
                            .append(book.getLanguage() != null ? book.getLanguage() : "").append(";")
                            .append(book.getFormat() != null ? book.getFormat() : "").append(";")
                            .append(book.getFinishDate() != null ? book.getFinishDate() : "").append(";")
                            .append(book.getReadYear()).append(";")
                            .append(book.getReadingTimeInDays()).append("\n");
            }
            
            // Generate filename with scheduled backup format
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            String fileName = "backup_scheduled_" + timestamp + ".txt";
            
            // Upload to S3
            byte[] contentBytes = backupContent.toString().getBytes("UTF-8");
            InputStream contentStream = new ByteArrayInputStream(contentBytes);
            
            PutObjectRequest putRequest = new PutObjectRequest(bucketName, fileName, contentStream, null);
            
            s3Client.putObject(putRequest);
            
            log.info("Scheduled backup successfully uploaded to S3: s3://{}/{}", bucketName, fileName);
            
        } catch (Exception e) {
            log.error("Error creating scheduled backup", e);
            throw new RuntimeException("Failed to create scheduled backup", e);
        }
        
        return null;
    }
}