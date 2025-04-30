package OneUP.main.serviceImpl;

import OneUP.main.exception.InvalidCredentialsException;
import OneUP.main.exception.UserNotValidatedException;
import OneUP.main.model.AppUser;
import OneUP.main.request.ConfirmRequest;
import OneUP.main.request.LoginRequest;
import OneUP.main.request.RegisterRequest;
import OneUP.main.service.JwtService;
import OneUP.main.service.UserService;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final Firestore firestore = FirestoreClient.getFirestore();
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public void registerUser(RegisterRequest dto) {
        String nickname = dto.nickname().toLowerCase().trim();

        DocumentReference userDoc = firestore.collection("users").document(nickname);
        try {
            if (userDoc.get().get().exists()) {
                throw new IllegalStateException("Nickname already in use");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to check existing user", e);
        }

        String code = String.format("%06d", new SecureRandom().nextInt(1_000_000));

        AppUser user = AppUser.builder()
                .nickname(nickname)
                .fullName(dto.fullName())
                .location(dto.location())
                .timezone(dto.timezone())
                .password(passwordEncoder.encode(dto.password()))
                .about(dto.about())
                .validated(false)
                .confirmCode(code)
                .createdAt(Instant.now())
                .build();

        try {
            userDoc.set(user).get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to save user", e);
        }
    }

    public String login(LoginRequest dto) {
        AppUser user = getUser(dto.nickname());

        if (!user.isValidated()) {
            throw new UserNotValidatedException();
        }

        if (!passwordEncoder.matches(dto.password(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        return jwtService.generateToken(user.getNickname());
    }

    public AppUser getUser(String nickname) {
        try {
            DocumentSnapshot snapshot = firestore.collection("users").document(nickname.toLowerCase()).get().get();
            if (!snapshot.exists()) {
                throw new NoSuchElementException("User not found");
            }
            return snapshot.toObject(AppUser.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch user", e);
        }
    }

    public boolean passwordMatches(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    public String confirmUser(ConfirmRequest requestModel) {
        DocumentReference doc = firestore.collection("users").document(requestModel.nickname().toLowerCase());
        try {
            AppUser user = getUser(requestModel.nickname());
            if (user.isValidated()) {
                throw new IllegalStateException("User already validated");
            }

            if (!user.getConfirmCode().equals(requestModel.code())) {
                throw new IllegalArgumentException("Invalid confirmation code");
            }

            doc.update("validated", true).get();

            return jwtService.generateToken(user.getNickname());

        } catch (Exception e) {
            throw new RuntimeException("Failed to confirm user", e);
        }
    }
}