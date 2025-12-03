package books;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@Data
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class Book {
    private String id;
    private String title;
    private String author;
    private String genre;
    private Integer year;
    private Boolean read;

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }
}