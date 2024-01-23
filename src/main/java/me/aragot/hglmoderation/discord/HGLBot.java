package me.aragot.hglmoderation.discord;

import com.velocitypowered.api.proxy.ProxyServer;
import me.aragot.hglmoderation.HGLModeration;
import me.aragot.hglmoderation.admin.config.Config;
import me.aragot.hglmoderation.data.reports.Priority;
import me.aragot.hglmoderation.data.reports.Reasoning;
import me.aragot.hglmoderation.data.reports.Report;
import me.aragot.hglmoderation.discord.commands.CommandParser;
import me.aragot.hglmoderation.response.ResponseType;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.slf4j.Logger;

import java.awt.*;
import java.time.LocalDateTime;
import java.util.UUID;

public class HGLBot {

    private static JDA instance;
    private static ProxyServer server;
    private static final String authorId = "974206098364071977";

    public static void init(ProxyServer server, Logger logger){
        if(Config.instance.getDiscordBotToken().isEmpty()){
            logger.info("No valid Discord Bot Token found, please edit the config.json file and run this command: /dcbot init");
            return;
        }
        HGLBot.server = server;
        JDABuilder builder = JDABuilder.createDefault(Config.instance.getDiscordBotToken());

        builder.addEventListeners(new CommandParser());

        instance = builder.build();


        SubcommandData setChannel = new SubcommandData("set", "Set a log channel to receive updates");
        setChannel.addOption(OptionType.CHANNEL, "logchannel", "Log channel for reports and status updates", true);

        SubcommandData setPingRole = new SubcommandData("pingrole", "Sets a role to ping when receiving a new report.");
        setPingRole.addOption(OptionType.ROLE, "role", "Role to ping");

        SubcommandData reportList = new SubcommandData("list", "Returns a list with all open reports");

        SubcommandData reportStats = new SubcommandData("stats", "Returns all relevant statistics");

        SubcommandData reportGet = new SubcommandData("get", "Returns a report based on it's ID");
        reportGet.addOption(OptionType.STRING, "reportid", "ID of the Report", true);

        SubcommandData reportDecline = new SubcommandData("decline", "Declines a report based on the provided ID");
        reportDecline.addOption(OptionType.STRING, "reportid", "ID of the Report", true);

        instance.updateCommands().addCommands(
                Commands.slash("logs", "Modify the logs")
                        .addSubcommands(
                                setChannel,
                                setPingRole
                        )
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.BAN_MEMBERS)),
                Commands.slash("report", "Manage reports to your will")
                        .addSubcommands(
                                reportList,
                                reportStats,
                                reportGet,
                                reportDecline
                        )
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.BAN_MEMBERS))
        ).queue();

        logger.info("Discord Bot has been initialized started!");
    }

    public static EmbedBuilder getEmbedTemplate(ResponseType type){
        EmbedBuilder eb = new EmbedBuilder();
        switch(type){
            case ERROR:
                eb.setTitle("Error!");
                eb.setColor(Color.red);
                break;
            case SUCCESS:
                eb.setTitle("Success!");
                eb.setColor(Color.green);
            case DEFAULT:
                eb.setTitle("Hey, listen!");
                eb.setColor(Color.blue);
        }

        eb.setFooter("Found a bug? Please contact my author: <@" + authorId + ">", instance.getUserById(authorId).getAvatarUrl());
        return eb;
    }
    public static EmbedBuilder getEmbedTemplate(ResponseType type, String description){
        EmbedBuilder eb = new EmbedBuilder();
        eb.setDescription(description);
        switch(type){
            case ERROR:
                eb.setTitle("Error!");
                eb.setColor(Color.red);
                break;
            case SUCCESS:
                eb.setTitle("Success!");
                eb.setColor(Color.green);
            case DEFAULT:
                eb.setTitle("Hey, listen!");
                eb.setColor(Color.blue);
        }

        eb.setFooter("Found a bug? Please contact my author: <@" + authorId + ">", instance.getUserById(authorId).getAvatarUrl());
        return eb;
    }

    public static EmbedBuilder getReportEmbed(Report report, boolean incoming){
        String title = incoming ? "New Report coming in" : "Report Information";
        Color color = Color.green;

        if(report.getPriority() == Priority.MEDIUM) color = Color.yellow;
        else if(report.getPriority() == Priority.HIGH) color = Color.red;
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(title);
        eb.setColor(color);
        eb.setFooter("Found a bug? Please contact my author: <@" + authorId + ">", instance.getUserById(authorId).getAvatarUrl());

        String description = "Reported Name: " + server.getPlayer(UUID.fromString(report.getReportedUUID())) + "\n" +
                "Reported by: " + server.getPlayer(UUID.fromString(report.getReporterUUID())) + "\n" +
                "Reasoning: " + report.getReasoning().name() + "\n" +
                "Report ID" + report.getReportId() + "\n" +
                "Priority: " + report.getPriority() + "\n" +
                "Submitted at: <t:" + report.getSubmittedAt() + ":f>\n" +
                "State: " + report.getState().name();

        eb.setDescription(description);
        return eb;
    }
}
