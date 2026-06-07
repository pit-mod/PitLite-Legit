package com.pitlite.commands;

import com.pitlite.module.impl.player.StopYourAddiction;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;

public class PitLiteAcceptCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "pitlite_accept";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/pitlite_accept";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        StopYourAddiction.tryAcceptFromClick();
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
