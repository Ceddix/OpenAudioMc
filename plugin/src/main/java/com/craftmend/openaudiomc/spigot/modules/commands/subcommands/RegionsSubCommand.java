package com.craftmend.openaudiomc.spigot.modules.commands.subcommands;

import com.craftmend.openaudiomc.generic.commands.interfaces.SubCommand;
import com.craftmend.openaudiomc.generic.commands.objects.Argument;
import com.craftmend.openaudiomc.generic.user.User;
import com.craftmend.openaudiomc.spigot.OpenAudioMcSpigot;
import com.craftmend.openaudiomc.spigot.modules.commands.subcommands.region.RegionCreateSubCommand;
import com.craftmend.openaudiomc.spigot.modules.commands.subcommands.region.RegionDeleteSubCommand;
import com.craftmend.openaudiomc.spigot.modules.commands.subcommands.region.RegionEditSubCommand;
import com.craftmend.openaudiomc.spigot.modules.commands.subcommands.region.RegionTempSubCommand;
import com.craftmend.openaudiomc.spigot.modules.regions.gui.RegionSelectionGui;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class RegionsSubCommand extends SubCommand {

    private final OpenAudioMcSpigot openAudioMcSpigot;

    public RegionsSubCommand(OpenAudioMcSpigot openAudioMcSpigot) {
        super("region");

        registerSubCommands(
                new RegionCreateSubCommand(openAudioMcSpigot),
                new RegionDeleteSubCommand(openAudioMcSpigot),
                new RegionTempSubCommand(openAudioMcSpigot),
                new RegionEditSubCommand(openAudioMcSpigot)
        );

        registerArguments(
                new Argument("create <WG-region> <source>",
                        "Assigns a sound to a WorldGuard region by name"),

                new Argument("temp <WG-region> <source> <duration>",
                        "Create a temporary region with it's own synced sound"),

                new Argument("delete <WG-region>",
                        "Unlink the sound from a WorldGuard specific region by name"),

                new Argument("edit",
                        "Change settings through the a GUI"),

                new Argument("edit volume <region> <volume>",
                        "Change the volume of a region"),

                new Argument("edit fade <region> <fade time MS>",
                        "Change the fade of a region")
        );
        this.openAudioMcSpigot = openAudioMcSpigot;
    }

    @Override
    public void onExecute(User sender, String[] args) {
        if (args.length == 0) {
            sender.makeExecuteCommand("oa help " + getCommand());
            return;
        }

        if (openAudioMcSpigot.getRegionModule() == null) {
            message(sender,ChatColor.RED + "You need to have WorldGuard installed in order to use the regions in OpenAudioMc.");
            return;
        }

        if ((args[0].equalsIgnoreCase("edit") || args[0].equalsIgnoreCase("gui"))) {

            // do we have any other args?
            if (args.length > 1) {
                delegateTo("edit", sender, args);
                return;
            }

            if (!(sender.getOriginal() instanceof Player)) {
                message(sender, ChatColor.RED + "You need to be a player to use this command");
                return;
            }

            Player player = (Player) sender.getOriginal();
            new RegionSelectionGui(player);
            return;
        }

        if (args[0].equalsIgnoreCase("temp") && args.length == 4) {
            delegateTo("temp", sender, args);
            return;
        }

        if (args[0].equalsIgnoreCase("create") && args.length == 3) {
            delegateTo("create", sender, args);
            return;
        }

        if (args[0].equalsIgnoreCase("delete") && args.length == 2) {
            delegateTo("delete", sender, args);
            return;
        }

        sender.makeExecuteCommand("oa help " + getCommand());
    }

}
