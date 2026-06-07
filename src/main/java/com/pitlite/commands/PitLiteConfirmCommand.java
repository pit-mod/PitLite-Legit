package com.pitlite.commands;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;

public class PitLiteConfirmCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "pitlite_confirm";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/pitlite_confirm";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        CommandHandler.tryConfirmFromClick();
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }
}
