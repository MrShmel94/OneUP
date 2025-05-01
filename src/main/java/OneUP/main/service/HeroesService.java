package OneUP.main.service;

import org.apache.coyote.BadRequestException;

import java.util.Map;
import java.util.concurrent.ExecutionException;

public interface HeroesService {
    void savePlayerHeroes(String nickname, Map<String, Map<String, String>> heroes) throws ExecutionException, InterruptedException, BadRequestException;
    Map<String, Map<String, String>> getPlayerHeroes(String nickname) throws ExecutionException, InterruptedException, BadRequestException;
    Map<String, Map<String, Map<String, String>>> getAllGuildHeroes() throws BadRequestException, ExecutionException, InterruptedException;
}
