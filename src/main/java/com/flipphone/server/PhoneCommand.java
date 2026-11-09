package com.flipphone.server;

import java.util.Collection;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;

/**
 * /phone yen give|take|set|check &lt;player&gt; [amount]   and   /phone number &lt;player&gt;
 *
 * <p>Operator level 4 or the server console only (that is how players get yen on a server: an admin, a store
 * plug-in or a job system runs these commands). Works for offline players too.
 */
public final class PhoneCommand {
    private PhoneCommand() {}

    private static final long MAX = 1_000_000_000_000L; // 1 trillion yen cap per command

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("phone")
                .requires(s -> s.hasPermission(4))
                .then(Commands.literal("yen")
                        .then(Commands.literal("give").then(player().then(amount().executes(c -> yen(c, 0)))))
                        .then(Commands.literal("take").then(player().then(amount().executes(c -> yen(c, 1)))))
                        .then(Commands.literal("set").then(player().then(amount().executes(c -> yen(c, 2)))))
                        .then(Commands.literal("check").then(player().executes(PhoneCommand::check))))
                .then(Commands.literal("number").then(player().executes(PhoneCommand::number))));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, ?> player() {
        return Commands.argument("player", GameProfileArgument.gameProfile());
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, Long> amount() {
        return Commands.argument("amount", LongArgumentType.longArg(0, MAX));
    }

    /** @param mode 0 = give, 1 = take, 2 = set */
    private static int yen(CommandContext<CommandSourceStack> ctx, int mode) throws CommandSyntaxException {
        long amount = LongArgumentType.getLong(ctx, "amount");
        Collection<GameProfile> targets = GameProfileArgument.getGameProfiles(ctx, "player");
        int done = 0;
        for (GameProfile t : targets) {
            UUID id = t.getId();
            long have = PhoneService.getYen(id);
            if (mode == 1 && have < amount) {
                ctx.getSource().sendFailure(Component.literal(t.getName() + " only has \u00a5" + have + "."));
                continue;
            }
            long now = mode == 0 ? have + amount : (mode == 1 ? have - amount : amount);
            PhoneService.setYen(id, now);
            done++;
        }
        if (done > 0) {
            String verb = mode == 0 ? "Gave \u00a5" + amount + " to " : (mode == 1 ? "Took \u00a5" + amount + " from " : "Set the balance to \u00a5" + amount + " for ");
            String who = targets.size() == 1 ? targets.iterator().next().getName() : done + " players";
            ctx.getSource().sendSuccess(() -> Component.literal(verb + who + ".").withStyle(ChatFormatting.GREEN), true);
        }
        return done;
    }

    private static int check(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        for (GameProfile t : GameProfileArgument.getGameProfiles(ctx, "player")) {
            long have = PhoneService.getYen(t.getId());
            ctx.getSource().sendSuccess(() -> Component.literal(t.getName() + " has \u00a5" + have + ".").withStyle(ChatFormatting.YELLOW), false);
        }
        return 1;
    }

    private static int number(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        for (GameProfile t : GameProfileArgument.getGameProfiles(ctx, "player")) {
            String n = PhoneService.getNumber(t.getId());
            ctx.getSource().sendSuccess(() -> Component.literal(t.getName() + ": " + (n.isEmpty() ? "(no number yet - never joined)" : n))
                    .withStyle(ChatFormatting.YELLOW), false);
        }
        return 1;
    }
}
