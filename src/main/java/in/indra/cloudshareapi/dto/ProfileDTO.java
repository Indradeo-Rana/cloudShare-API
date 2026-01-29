package in.indra.cloudshareapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProfileDTO {

    private String id;
    private String clerkId;
    private String firstName;
    private String lastName;
    private String email;
    private Integer credits;
    private String photoUrl;
    private Instant createdAt;
}
