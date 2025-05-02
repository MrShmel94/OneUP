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
public class GuildMember {

    private String nickname;
    private String fullName;
    private String location;
    private String timezone;
    private String about;
    private String role;
    private boolean isBanned;

    private String confirmCode;
}
