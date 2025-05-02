package OneUP.main.serviceImpl;

import OneUP.main.model.GuildMember;
import OneUP.main.service.GuildMembersService;
import OneUP.main.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuildMembersServiceImpl implements GuildMembersService {

    private final Firestore firestore = FirestoreClient.getFirestore();
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String REDIS_KEY = "guild:members";

    @Override
    public List<GuildMember> getAllMembers() {
        return List.of();
    }
}
