package in.indra.cloudshareapi.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Document(collection = "profiles")
public class ProfileDocument {

    @Id
    private String id;

    private String clerkId;
    private String firstName;
    private String lastName;

    @Indexed(unique = true)
    private String email;
    private Integer credits;
    private String photoUrl;

    @CreatedDate
    private Instant createdAt;
}