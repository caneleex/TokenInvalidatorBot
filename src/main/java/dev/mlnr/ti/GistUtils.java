package dev.mlnr.ti;

import net.dv8tion.jda.api.utils.data.DataArray;
import net.dv8tion.jda.api.utils.data.DataObject;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public class GistUtils {
	private static final OkHttpClient OK_HTTP_CLIENT = new OkHttpClient();
	private static final String GIST_API_URL = "https://api.github.com/gists";

	private static final List<String> ALREADY_INVALIDATED_TOKENS = new ArrayList<>();

	private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor();
	private static final Logger logger = LoggerFactory.getLogger(GistUtils.class);

	private GistUtils() {}

	public static void createGistWithToken(String token, Consumer<String> gistUrlConsumer, IntConsumer invalidCodeConsumer, Consumer<Throwable> failureConsumer) {
		var contentPayload = DataObject.empty();
		var tokenPayload = DataObject.empty();
		var payload = DataObject.empty();

		contentPayload.put("content", token);
		tokenPayload.put("token.txt", contentPayload);
		payload.put("files", tokenPayload);

		payload.put("description", "Token Invalidator Bot by cane#0570. The token was invalidated for you for your own safety.");
		payload.put("public", true);

		var requestBuilder = new Request.Builder();
		requestBuilder.header("Authorization", "token " + System.getenv("TIGistToken"));
		requestBuilder.url(GIST_API_URL);
		requestBuilder.post(RequestBody.create(MediaType.parse("application/vnd.github.v3+json"), payload.toString()));

		try (var response = OK_HTTP_CLIENT.newCall(requestBuilder.build()).execute(); var responseBody = response.body()) {
			var responseCode = response.code();
			if (responseCode == 201) {
				var responseJson = DataObject.fromJson(responseBody.string());
				gistUrlConsumer.accept(responseJson.getString("html_url"));
				scheduleGistDeletion(responseJson.getString("id"));
				ALREADY_INVALIDATED_TOKENS.add(token);
				return;
			}
			invalidCodeConsumer.accept(responseCode);
		}
		catch (Exception ex) {
			logger.error("There was an error while posting token {} to gist", token, ex);
			failureConsumer.accept(ex);
		}
	}

	private static void scheduleGistDeletion(String gistId) {
		SCHEDULER.schedule(() -> {
			var requestBuilder = new Request.Builder();
			requestBuilder.header("Authorization", "token " + System.getenv("TIGistToken"));
			requestBuilder.url(GIST_API_URL + "/" + gistId);
			requestBuilder.delete();

			try (var response = OK_HTTP_CLIENT.newCall(requestBuilder.build()).execute()) {
				var responseCode = response.code();
				if (responseCode != 204) {
					logger.warn("Received code {} while deleting gist {}", responseCode, gistId);
				}
			}
			catch (Exception ex) {
				logger.error("There was an error while deleting gist {}", gistId, ex);
			}
		}, 30, TimeUnit.MINUTES);
	}

	public static List<String> getAlreadyInvalidatedTokens() {
		return ALREADY_INVALIDATED_TOKENS;
	}
}