package OneUP.main.service;

import OneUP.main.model.GuildMember;
import OneUP.main.request.PlayerDataRequest;

import java.util.List;
import java.util.Map;

public interface GuildMembersService {

    List<GuildMember> getAllMembers();
    PlayerDataRequest getPlayerData(String nickname);
    PlayerDataRequest getPlayerData();
    Map<String, PlayerDataRequest> getAllPlayersData();
    void savePlayerData(PlayerDataRequest request);
}
