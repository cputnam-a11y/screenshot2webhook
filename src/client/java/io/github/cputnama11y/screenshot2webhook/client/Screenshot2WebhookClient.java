package io.github.cputnama11y.screenshot2webhook.client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.network.chat.Component;
import org.apache.commons.compress.archivers.sevenz.CLI;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

public class Screenshot2WebhookClient implements ClientModInitializer {
    HttpClient CLIENT = HttpClient.newHttpClient();
    private static final Gson GSON = new Gson();
    private static Config config;

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> {
            var base = ClientCommands.literal("screenshot2webbhook")
                    .then(
                            ClientCommands.literal("config")
                                    .then(
                                            ClientCommands.literal("reload")
                                                    .executes(ctx -> {
                                                        try {
                                                            config = Config.loadConfig();
                                                        } catch (IOException ex) {
                                                            ctx.getSource().sendError(Component.literal("Failed to reload config!"));
                                                            return -1;
                                                        }
                                                        return Command.SINGLE_SUCCESS;
                                                    })
                                    )
                    )
                    .then(
                            ClientCommands.literal("send2webhook")
                                    .then(
                                            ClientCommands.argument("screenshot", StringArgumentType.string())
                                                    .executes(ctx -> {
                                                        if (config.discordWebhookUrl() == null || config.discordWebhookUrl().trim().isEmpty()) {
                                                            ctx.getSource().sendError(Component.literal("you must configure a webhook url in the config to upload screenshots"));
                                                            return -1;
                                                        }
                                                        var screenshot = Path.of(StringArgumentType.getString(ctx, "screenshot"));
                                                        if (!Files.exists(screenshot)) {
                                                            ctx.getSource().sendError(Component.literal("Could not locate screenshot!"));
                                                            return -1;
                                                        }
                                                        HTTPRequestMultipartBody built;
                                                        try {
                                                            built = HTTPRequestMultipartBody.builder()
                                                                    .part(
                                                                            "payload_json",
                                                                            GSON.toJson(obj(p -> {
                                                                                p.add("attachments", arr(attachments -> {
                                                                                    attachments.add(obj(attachment -> {
                                                                                        attachment.addProperty("id", 0);
                                                                                        attachment.addProperty("filename", screenshot.getFileName().toString());
                                                                                    }));
                                                                                }));
                                                                            })),
                                                                            "application/json"
                                                                    )
                                                                    .part("files[0]", Files.readAllBytes(screenshot), "image/png", screenshot.getFileName().toString())
                                                                    .build();
                                                        } catch (IOException ex) {
                                                            ctx.getSource().sendError(Component.literal("Failed to read screenshot!"));
                                                            return -1;
                                                        }
                                                        CLIENT.sendAsync(
                                                                HttpRequest.newBuilder()
                                                                        .header("Content-Type", built.contentType())
                                                                        .header("User-Agent", "DiscordBot (https://github.com/cputnam-a11y/screenshot2webhook/, 1.0.0)")
                                                                        .uri(URI.create(config.discordWebhookUrl()))
                                                                        .POST(
                                                                                HttpRequest.BodyPublishers.ofByteArray(built.bytes())
                                                                        )
                                                                        .build(),
                                                                HttpResponse.BodyHandlers.ofString()
                                                        ).handleAsync(
                                                                (res, err) -> {
                                                                    if (err != null) {
                                                                        ctx.getSource().getClient().execute(() -> ctx.getSource().sendError(Component.literal("Failed to send webhook message")));
                                                                        return null;
                                                                    }
                                                                    if (res.statusCode() == 200) {
                                                                        ctx.getSource().getClient().execute(() ->  ctx.getSource().sendFeedback(Component.literal("Successfully Uploaded screenshot to discord")));
                                                                    } else {
                                                                        ctx.getSource().getClient().execute(() -> ctx.getSource().sendError(Component.literal("Failed to send webhook message")));
                                                                    }
                                                                    return null;
                                                                }
                                                        );
                                                        return Command.SINGLE_SUCCESS;
                                                    })
                                    )
                    );
            dispatcher.register(base);
        });
        try {
            config = Config.loadConfig();
        } catch (Exception e) {
            // hmm yes, error handling...
        }

        ClientLifecycleEvents.CLIENT_STOPPING.register(_ -> CLIENT.close());
    }

    static JsonObject obj(Consumer<JsonObject> init) {
        var obj = new JsonObject();
        init.accept(obj);
        return obj;
    }

    static JsonArray arr(Consumer<JsonArray> init) {
        var arr = new JsonArray();
        init.accept(arr);
        return arr;
    }
}