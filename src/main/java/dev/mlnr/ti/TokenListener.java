package dev.mlnr.ti;

import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.regex.Pattern;

public class TokenListener extends ListenerAdapter {
	private static final Pattern TOKEN_REGEX = Pattern.compile("[A-Za-z\\d]{24}\\.[\\w-]{6}\\.[\\w-]{27}");

	@Override
	public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
		var matcher = TOKEN_REGEX.matcher(event.getMessage().getContentStripped());
		if (matcher.find()) {
			var token = matcher.group(0);
			var channel = event.getChannel();
			var mention = event.getAuthor().getAsMention();
			if (GistUtils.getAlreadyInvalidatedTokens().contains(token)) {
				channel.sendMessage(mention + ", i found a token in your message but it's already been invalidated.").queue();
				return;
			}
			GistUtils.createGistWithToken(token, gistUrl -> {
				channel.sendMessage(mention + ", i found a token in your message and sent it to <" + gistUrl + "> to be invalidated.").queue();
			}, invalidResponseCode -> {
				channel.sendMessage(mention + ", i found a token in your message but received **" + invalidResponseCode + "** while sending it to gists. Please regenerate the token yourself.").queue();
			}, failure -> {
				channel.sendMessage(mention + ", i found a token in your message but received an error while sending it to gists. Please regenerate the token yourself.").queue();
			});
		}
	}
}