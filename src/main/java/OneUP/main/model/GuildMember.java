package OneUP.main.model;

import OneUP.main.security.Role;
import OneUP.main.security.VisibleForRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuildMember {

    private String nickname;

    @VisibleForRole(Role.VIEWER)
    private String fullName;
    @VisibleForRole(Role.VIEWER)
    private String location;

    private String timezone;

    @VisibleForRole(Role.MODERATOR)
    private String about;
    @VisibleForRole(Role.VIEWER)
    private String role;
    @VisibleForRole(Role.VIEWER)
    private boolean isBanned;

    @VisibleForRole(Role.MODERATOR)
    private String confirmCode;
}
