package OneUP.main.service;

import org.apache.coyote.BadRequestException;

import java.util.Map;
import java.util.concurrent.ExecutionException;

public interface ArtifactService {
    void savePlayerArtifact(String nickname, Map<String, Map<String, String>> artifacts) throws ExecutionException, InterruptedException, BadRequestException;
    Map<String, Map<String, String>> getPlayerArtifact(String nickname) throws ExecutionException, InterruptedException, BadRequestException;
    Map<String, Map<String, Map<String, String>>> getAllGuildArtifact() throws BadRequestException, ExecutionException, InterruptedException;
}
