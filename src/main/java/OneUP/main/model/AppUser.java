package OneUP.main.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppUser {
    private String nickname;
    private String fullName;
    private String location;
    private String timezone;
    private String password;
    private String about;

    private boolean validated;
    private String confirmCode;
    private Instant createdAt;
}
