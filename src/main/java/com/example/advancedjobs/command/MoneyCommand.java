package com.example.advancedjobs.command;

import com.example.advancedjobs.AdvancedJobsMod;
import com.example.advancedjobs.util.DebugLog;
import com.example.advancedjobs.util.TextUtil;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class MoneyCommand {
    private MoneyCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("money")
            .executes(ctx -> balance(ctx.getSource().getPlayerOrException())));

        dispatcher.register(Commands.literal("bal")
            .executes(ctx -> balance(ctx.getSource().getPlayerOrException())));

        dispatcher.register(Commands.literal("pay")
            .then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01D))
                    .executes(ctx -> pay(
                        ctx.getSource().getPlayerOrException(),
                        EntityArgument.getPlayer(ctx, "player"),
                        DoubleArgumentType.getDouble(ctx, "amount")
                    )))));
    }

    private static int balance(ServerPlayer player) {
        double balance = AdvancedJobsMod.get().jobManager().economy().getBalance(player.getUUID());
        player.sendSystemMessage(Component.literal("Balance: " + TextUtil.fmt2(balance)));
        return 1;
    }

    private static int pay(ServerPlayer from, ServerPlayer to, double amount) {
        if (from.getUUID().equals(to.getUUID())) {
            from.sendSystemMessage(Component.literal("You cannot pay yourself."));
            return 0;
        }
        if (!AdvancedJobsMod.get().jobManager().economy().withdraw(from.getUUID(), amount, "player_pay")) {
            from.sendSystemMessage(Component.literal("Not enough balance."));
            return 0;
        }
        if (!AdvancedJobsMod.get().jobManager().economy().deposit(to.getUUID(), amount, "player_pay")) {
            AdvancedJobsMod.get().jobManager().economy().deposit(from.getUUID(), amount, "player_pay_refund");
            from.sendSystemMessage(Component.literal("Payment failed."));
            return 0;
        }
        DebugLog.log("Money transfer: from=" + from.getGameProfile().getName() + " to=" + to.getGameProfile().getName() + " amount=" + amount);
        from.sendSystemMessage(Component.literal("Sent " + TextUtil.fmt2(amount) + " to " + to.getGameProfile().getName()));
        to.sendSystemMessage(Component.literal("Received " + TextUtil.fmt2(amount) + " from " + from.getGameProfile().getName()));
        return 1;
    }
}
