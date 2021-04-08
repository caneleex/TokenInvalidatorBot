package dev.mlnr.ti;

import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.Base64;
import java.util.regex.Pattern;

public class TokenListener extends ListenerAdapter {
	private static final Pattern TOKEN_REGEX = Pattern.compile("[A-Za-z\\d]{24}\\.[\\w-]{6}\\.[\\w-]{27}");
	private static final String DEV_DASHBOARD_TEMPLATE = "<https://discord.com/developers/applications/%s/bot>";

	@Override
	public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
		var author = event.getAuthor();
		if (author.isBot() || event.isWebhookMessage()) {
			return;
		}
		var matcher = TOKEN_REGEX.matcher(event.getMessage().getContentRaw());
		if (matcher.find()) {
			var token = matcher.group(0);
			var channel = event.getChannel();
			var mention = author.getAsMention();
			if (GistUtils.getAlreadyInvalidatedTokens().contains(token)) {
				channel.sendMessage(mention + ", i found a token in your message but it's already been invalidated.").queue();
				return;
			}
			var decoded = Base64.getDecoder().decode(token.substring(0, token.indexOf('.')));
			var decodedId = new String(decoded);
			var formattedMessage = String.format(DEV_DASHBOARD_TEMPLATE, decodedId);

			GistUtils.createGistWithToken(token, gistUrl -> {
				channel.sendMessage(mention + ", i found a token in your message and sent it to <" + gistUrl + "> to be invalidated.").queue();
			}, invalidResponseCode -> {
				channel.sendMessage(mention + ", i found a token in your message but received **" + invalidResponseCode + "** while sending it to gists. " +
						"Please regenerate the token yourself at " + formattedMessage + ".").queue();
			}, failure -> {
				channel.sendMessage(mention + ", i found a token in your message but received an error while sending it to gists. " +
						"Please regenerate the token yourself at " + formattedMessage + ".").queue();
			});
		}
	}
}