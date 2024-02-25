package me.aragot.hglmoderation.data.reports;

import com.velocitypowered.api.proxy.Player;
import me.aragot.hglmoderation.HGLModeration;
import me.aragot.hglmoderation.admin.config.Config;
import me.aragot.hglmoderation.admin.preset.Preset;
import me.aragot.hglmoderation.admin.preset.PresetHandler;
import me.aragot.hglmoderation.data.Notification;
import me.aragot.hglmoderation.data.PlayerData;
import me.aragot.hglmoderation.data.Reasoning;
import me.aragot.hglmoderation.data.punishments.Punishment;
import me.aragot.hglmoderation.discord.HGLBot;
import me.aragot.hglmoderation.events.PlayerListener;
import me.aragot.hglmoderation.response.ResponseType;
import me.aragot.hglmoderation.tools.Notifier;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;


public class Report {

    private final String _id;
    private final String reportedUUID; //Reported Player UUID

    //Maybe change to HashMap<String, Long> for uuid and submitDate?
    private final String reporterUUID; //Reporter Player UUID
    private final long submittedAt;
    private final Reasoning reasoning;
    private ReportState state;
    private final Priority priority;

    private String reviewedBy = ""; //Minecraft Player UUID
    private String punishmentId = "";
    private String discordLog = "";

    private ArrayList<String> reportedUserMessages;

    public Report(String reportId, String reportedUUID, String reporterUUID, long submittedAt, Reasoning reasoning, Priority priority, ReportState state){
        this._id = reportId;
        this.reportedUUID = reportedUUID;
        this.reporterUUID = reporterUUID;
        this.submittedAt = submittedAt;
        this.reasoning = reasoning;
        if(Reasoning.getChatReasons().contains(reasoning)) this.reportedUserMessages = PlayerListener.userMessages.get(reportedUUID);
        this.priority = priority;
        this.state = state;
    }

    //Used for ReportCodec
    public Report(String reportId, String reportedUUID, String reporterUUID, long submittedAt, Reasoning reasoning, ReportState state, Priority priority, String reviewedBy, String punishmentId, String discordLog, ArrayList<String> reportedUserMessages) {
        this._id = reportId;
        this.reportedUUID = reportedUUID;
        this.reporterUUID = reporterUUID;
        this.submittedAt = submittedAt;
        this.reasoning = reasoning;
        this.state = state;
        this.priority = priority;
        this.reviewedBy = reviewedBy;
        this.punishmentId = punishmentId;
        this.discordLog = discordLog;
        this.reportedUserMessages = reportedUserMessages;
    }

    public static void submitReport(String reportedUUID, String reporterUUID, Reasoning reasoning, Priority priority){
        Report report = new Report(
                getNextReportId(),
                reportedUUID,
                reporterUUID,
                Instant.now().getEpochSecond(),
                reasoning,
                priority,
                ReportState.OPEN);

        if(HGLBot.instance == null) return;

        HGLBot.logReport(report);

        if(!HGLModeration.instance.getDatabase().pushReport(report)){
            TextChannel channel = HGLBot.instance.getTextChannelById(Config.instance.getReportChannelId());

            if(channel == null) return;
            channel.sendMessageEmbeds(
                    HGLBot.getEmbedTemplate(ResponseType.ERROR, "Couldn't push report to Database (ID:" + report.getId() + ")").build()
            ).queue();

        }

        Notifier.notify(Notification.REPORT, report.getMCReportComponent(true));
    }

    public static String getNextReportId(){
        //table is hex number
        //Report id is random 8 digit hex number
        String [] table = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F"};
        boolean isUnique = false;
        String id = "";
        while(!isUnique){
            Random rand = new Random();

            for(int i = 0; i < 8; i++)
                id += table[rand.nextInt(16)];

            if(HGLModeration.instance.getDatabase().getReportById(id) != null){
                id = "";
                continue;
            }

            isUnique = true;
        }

        return id;
    }

    public static Priority getPriorityForReporter(Player player){
        PlayerData data = PlayerData.getPlayerData(player);

        if(data.getReportScore() < 0) return Priority.LOW;
        if(data.getReportScore() > 5) return Priority.HIGH;
        return Priority.MEDIUM;
    }

    public void setReviewedBy(String reviewerId){
        this.reviewedBy = reviewerId;
    }

    public String getReviewedBy(){
        return this.reviewedBy;
    }
    public String getId() {
        return _id;
    }

    public String getReportedUUID() {
        return reportedUUID;
    }

    public String getReporterUUID() {
        return reporterUUID;
    }

    public long getSubmittedAt() {
        return submittedAt;
    }

    public Reasoning getReasoning() {
        return reasoning;
    }

    public ReportState getState() {
        return state;
    }

    public void setState(ReportState state){
        this.state = state;
    }

    public Priority getPriority() {
        return priority;
    }

    public ArrayList<String> getReportedUserMessages() {
        return reportedUserMessages == null ? new ArrayList<>() : reportedUserMessages;
    }

    public void setPunishmentId(String punishmentId){
        this.punishmentId = punishmentId;
    }

    public String getPunishmentId(){
        return this.punishmentId;
    }

    public String getDiscordLog(){
        return this.discordLog;
    }

    public void setDiscordLog(String messageId){
        this.discordLog = messageId;
    }



    public Component getMcReportOverview(){
        MiniMessage mm = MiniMessage.miniMessage();
        String prio = "<white>Priority:</white> <red>" + this.priority.name() + "</red>";
        String reason = "<white>Reasoning:</white> <red>" + this.reasoning.name() + "</red>";
        String reportState = "<white>State:</white> <red>" + this.state.name() + "</red>";
        String reported = "<white>Reported:</white> <red>" + HGLModeration.instance.getServer().getPlayer(UUID.fromString(this.reportedUUID)).get().getUsername() + "</red>";

        String reportDetails = "<yellow><b>Report #" + this._id + "</b></yellow>\n\n" +
                "<gray>Reported Player:</gray> <red>" +  HGLModeration.instance.getServer().getPlayer(UUID.fromString(this.reportedUUID)).get().getUsername() + "</red>\n" +
                "<gray>Reported By:</gray> <red>" + HGLModeration.instance.getServer().getPlayer(UUID.fromString(this.reporterUUID)).get().getUsername() + "</red>\n" +
                "<gray>Reasoning:</gray> <red>" + this.reasoning.name() + "</red>\n" +
                "<gray>Priority:</gray> <red>" + this.priority.name() + "</red>\n" +
                "<gray>Submitted at:</gray> <red>" + new SimpleDateFormat("HH:mm:ss dd/MM/yyyy").format(new Date(this.submittedAt * 1000)) + "</red>\n" +
                "<gray>State:</gray> <red>" + this.state.name() + "</red>";

        if(Reasoning.getChatReasons().contains(this.getReasoning()))
            reportDetails += "\n<gray>User Messages:</gray>\n" + this.getFormattedUserMessages();

        String viewDetails = "<hover:show_text:'" + reportDetails + "'><white>[<blue><b>View Details</b></blue>]</white></hover>";

        String reviewReport = "<click:run_command:'/review " + this.getId() + "'><white>[<yellow><b>Review</b></yellow>]</white></click>";

        String deserialize = "                    " + prio + "\n" +
                "                    " + reason + "\n" +
                "                    " + reported + "\n" +
                "                    " + reportState + "\n\n" +
                "                    " + viewDetails + "   " + reviewReport + "\n" +
                "<gold>===================================================</gold>";
        return mm.deserialize(deserialize);
    }

    public Component getMCReportActions(){
        //HoverText
        MiniMessage mm = MiniMessage.miniMessage();
        Preset preset = PresetHandler.instance.getPresetForScore(this.reasoning, PlayerData.getPlayerData(this.getReportedUUID()).getPunishmentScore());
        String presetName = preset == null ? "None" : preset.getName();
        //Missing Punishment preset for command
        // /review <punishmentId> <boolean:accept> <preset/punishReporter>
        // /punish <type:[ban/mute/]> <boolean:notifReporters?trueByDefault>
        return mm.deserialize("<click:suggest_command:'/preset apply " + presetName + " " + this.getId() + "'><white>[<green><b>Punish</b></green>]</white></click>" +
                "   <click:suggest_command:'/review " + this.getId() + " decline'><white>[<red><b>Decline</b></red>]</white></click>" +
                "   <click:suggest_command:'/review " + this.getId() + " malicious'><white>[<red><b>Decline & Mark as malicious</b></red>]</white></click>\n" +
                "<hover:show_text:'" + getFormattedPunishments() + "'><white>[<blue><b>Previous Punishments</b></blue>]</white></hover>" +
                "   <hover:show_text:'" + getOtherFormattedReports() + "'><white>[<blue><b>Other Reports</b></blue>]</white></hover>\n" +
                "<click:run_command:'/server " + HGLModeration.instance.getServer().getPlayer(UUID.fromString(this.reportedUUID)).get().getCurrentServer().get().getServerInfo().getName() + "'>" +
                "<white>[<blue><b>Follow Player</b></blue>]</white></click>");
    }

    public Component getMCReportComponent(boolean incoming){
        MiniMessage mm = MiniMessage.miniMessage();
        if(incoming)
            return mm.deserialize("<gold>============= <white>Incoming</white> <red>Report: #" + this.getId() + "</red> =============</gold>\n")
                    .append(getMcReportOverview());

        return mm.deserialize(" <b><yellow>Report #" + this._id + "</yellow></b>\n")
                .append(getMcReportOverview());
    }

    private String getFormattedUserMessages(){
        if(this.reportedUserMessages == null || this.reportedUserMessages.isEmpty())
            return "<gray>No Messages sent</gray>";

        StringBuilder messages = new StringBuilder();
        String username = HGLModeration.instance.getServer().getPlayer(UUID.fromString(this.reportedUUID)).get().getUsername();
        for(String message : this.getReportedUserMessages())
            messages.append("\n<red>").append(username).append("</red>: ").append(message);

        return messages.toString();
    }

    public String getFormattedState(){
        return this.getState() == ReportState.DONE ? "was already <blue>reviewed</blue>" : "is already <yellow>under review</yellow>";
    }

    //Change display!!!!!
    private String getFormattedPunishments(){
        PlayerData data = PlayerData.getPlayerData(this.getReportedUUID());
        if(data.getPunishments().isEmpty()) return "No Punishments found";
        ArrayList<Punishment> punishments = HGLModeration.instance.getDatabase().getPunishmentsForPlayer(this.getReportedUUID());
        StringBuilder formatted = new StringBuilder("<gray><blue>ID</blue>   |   <blue>Type</blue>   |   <blue>Reason</blue>   |   <blue>Status</blue></gray>");
        for(Punishment punishment : punishments) {
            formatted.append("\n<gray>").append(punishment.getId()).append(" |</gray> <yellow>")
                    .append(punishment.getTypesAsString()).append("</yellow> <gray>|</gray> <red>")
                    .append(punishment.getReasoning()).append("</red> <gray>|</gray> ")
                    .append(punishment.isActive() ? "<green>⊙</green>" : "<red>⊙</red>");
        }

        return formatted.toString();
    }

    private String getOtherFormattedReports(){
        ArrayList<Report> reports = HGLModeration.instance.getDatabase().getReportsForPlayerExcept(this.getReportedUUID(), this._id);
        if(reports.isEmpty()) return "No Reports found";
        StringBuilder formatted = new StringBuilder("<gray><blue>ID</blue>   |   <blue><b>State</b></blue>   |   <blue>Reason</blue>");

        for(Report report : reports) {
            formatted.append("\n<gray>").append(report.getId()).append(" |</gray> <yellow>")
                    .append(report.getState().name()).append("</yellow> <gray>|</gray> <red>")
                    .append(report.getReasoning()).append("</red>");
        }

        return formatted.toString();
    }


}
