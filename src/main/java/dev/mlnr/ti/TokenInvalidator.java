package dev.mlnr.ti;

import net.dv8tion.jda.api.GatewayEncoding;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TokenInvalidator {
	private static final Logger logger = LoggerFactory.getLogger(TokenInvalidator.class);

	public static void main(String[] args) {
		try {
			JDABuilder.create(System.getenv("TokenInvalidator"), GatewayIntent.GUILD_MESSAGES)
					.addEventListeners(new TokenListener())
					.setMemberCachePolicy(MemberCachePolicy.NONE)
					.setChunkingFilter(ChunkingFilter.NONE)
					.setGatewayEncoding(GatewayEncoding.ETF)
					.setActivity(Activity.watching("for tokens"))
					.build()
					.awaitReady();
		}
		catch (Exception ex) {
			logger.error("There was an error while building JDA", ex);
		}
	}
}