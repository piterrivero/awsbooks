package books;

import lombok.Data;
import java.time.LocalDate;

@Data
public class BookResponse {
    private Integer id;
    private String title;
    private int publicationYear;
    private int readYear;
    private String author;
    private String language;
    private String format;
    private LocalDate finishDate;
    private int readingTimeInDays;
}