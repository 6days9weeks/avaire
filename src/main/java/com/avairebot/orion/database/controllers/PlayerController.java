package com.avairebot.orion.database.controllers;

import com.avairebot.orion.Constants;
import com.avairebot.orion.Orion;
import com.avairebot.orion.cache.CacheType;
import com.avairebot.orion.database.collection.DataRow;
import com.avairebot.orion.database.transformers.PlayerTransformer;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.User;

import javax.annotation.CheckReturnValue;
import java.sql.SQLException;

public class PlayerController {

    private static final String CACHE_STRING = "database.player.%s.%s";

    @CheckReturnValue
    public static PlayerTransformer fetchPlayer(Orion orion, Message message) {
        return fetchPlayer(orion, message, message.getAuthor());
    }

    @CheckReturnValue
    public static PlayerTransformer fetchPlayer(Orion orion, Message message, User user) {
        if (!message.getChannelType().isGuild()) {
            return null;
        }

        final String cacheToken = String.format(CACHE_STRING,
            message.getGuild().getId(),
            user.getId()
        );

        if (orion.getCache().getAdapter(CacheType.MEMORY).has(cacheToken)) {
            return (PlayerTransformer) orion.getCache().getAdapter(CacheType.MEMORY).get(cacheToken);
        }

        try {
            PlayerTransformer transformer = new PlayerTransformer(orion.getDatabase().newQueryBuilder(Constants.PLAYER_EXPERIENCE_TABLE_NAME)
                .where("user_id", user.getId())
                .andWhere("guild_id", message.getGuild().getId())
                .get().first());

            if (!transformer.hasData()) {
                orion.getDatabase().newQueryBuilder(Constants.PLAYER_EXPERIENCE_TABLE_NAME)
                    .insert(statement -> {
                        statement.set("guild_id", message.getGuild().getId())
                            .set("user_id", user.getId())
                            .set("username", user.getName(), true)
                            .set("discriminator", user.getDiscriminator())
                            .set("avatar", user.getAvatarId())
                            .set("experience", 100);

                        orion.getCache().getAdapter(CacheType.MEMORY).put(cacheToken, new PlayerTransformer(new DataRow(statement.getItems())), 2);
                    });

                return (PlayerTransformer) orion.getCache().getAdapter(CacheType.MEMORY).get(cacheToken);
            }

            // If the users name haven't been encoded yet, we'll do it below.
            String username = transformer.getRawData().get("username").toString();
            if (username.startsWith("base64:")) {
                orion.getCache()
                    .getAdapter(CacheType.MEMORY)
                    .put(cacheToken, transformer, 300);

                return transformer;
            }

            orion.getDatabase().newQueryBuilder(Constants.PLAYER_EXPERIENCE_TABLE_NAME)
                .where("user_id", message.getAuthor().getId())
                .update(statement -> statement.set("username", message.getAuthor().getName(), true));

            return transformer;
        } catch (SQLException ex) {
            Orion.getLogger().error(ex.getMessage(), ex);
            return null;
        }
    }
}
