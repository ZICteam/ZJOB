package com.example.advancedjobs.gui;

import com.example.advancedjobs.client.ClientJobState;
import com.example.advancedjobs.config.ConfigManager;
import com.example.advancedjobs.network.ChooseJobPacket;
import com.example.advancedjobs.network.ClaimSalaryPacket;
import com.example.advancedjobs.network.LeaveJobPacket;
import com.example.advancedjobs.network.PacketHandler;
import com.example.advancedjobs.network.RequestLeaderboardPacket;
import com.example.advancedjobs.network.RerollContractsPacket;
import com.example.advancedjobs.network.UpgradePerkPacket;
import com.example.advancedjobs.util.TextUtil;
import com.example.advancedjobs.util.TimeUtil;
import com.example.advancedjobs.util.XpFormulaUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

public class JobsMainScreen extends Screen {
    private static final int LIST_SIZE = 7;
    private static final int TOP_PAGE_SIZE = 4;
    private static final int BAR_WIDTH = 120;
    private static final String OVERALL_LEADERBOARD_ID = "all";
    private static final ResourceLocation ADVANCEMENTS_WINDOW = new ResourceLocation("minecraft", "textures/gui/advancements/window.png");
    private static final ResourceLocation ADVANCEMENTS_WIDGETS = new ResourceLocation("minecraft", "textures/gui/advancements/widgets.png");
    private static final ResourceLocation ADVANCEMENTS_TABS = new ResourceLocation("minecraft", "textures/gui/advancements/tabs.png");
    private static final ResourceLocation ADVANCEMENTS_STONE = new ResourceLocation("minecraft", "textures/gui/advancements/backgrounds/stone.png");
    private static final ResourceLocation GENERIC_54 = new ResourceLocation("minecraft", "textures/gui/container/generic_54.png");
    private static final ResourceLocation SKILL_STATUS_CHECK = new ResourceLocation("advancedjobs", "textures/gui/skills/status_check.png");
    private static final ResourceLocation SKILL_STATUS_CROSS = new ResourceLocation("advancedjobs", "textures/gui/skills/status_cross.png");
    private static final ResourceLocation SKILL_STATUS_PLUS = new ResourceLocation("advancedjobs", "textures/gui/skills/status_plus.png");

    private Tab currentTab = Tab.PROFESSIONS;
    private int listOffset;
    private int skillOffset;
    private int salaryOffset;
    private int topOffset;
    private int passiveOffset;
    private int perkOffset;
    private int effectOffset;
    private int selectedSkillBranch;
    private int skillPanX;
    private int skillPanY;
    private int leaderboardJobIndex;
    private TopSort topSort = TopSort.LEVEL;
    private JsonObject selectedJob;
    private String lastRequestedLeaderboardJobId;
    private final String preferredJobId;
    private ScrollTarget activeScrollbar = ScrollTarget.NONE;
    private int activeScrollbarX;
    private int activeScrollbarY;
    private int activeScrollbarHeight;
    private boolean draggingScrollbar;
    private boolean draggingSkillTree;

    public JobsMainScreen() {
        this(Tab.PROFESSIONS, null);
    }

    public JobsMainScreen(String preferredJobId) {
        this(Tab.PROFESSIONS, preferredJobId);
    }

    protected JobsMainScreen(Tab initialTab) {
        this(initialTab, null);
    }

    protected JobsMainScreen(Tab initialTab, String preferredJobId) {
        super(Component.translatable("gui.advancedjobs.title"));
        this.currentTab = initialTab;
        this.preferredJobId = preferredJobId;
    }

    @Override
    protected void init() {
        syncSelectedJob();
        clearWidgets();
        if (!usesCompactWindowLayout()) {
            int x = 20;
            for (Tab tab : Tab.values()) {
                Tab targetTab = tab;
                addRenderableWidget(Button.builder(Component.translatable(tab.key), button -> {
                    currentTab = targetTab;
                    listOffset = 0;
                    skillOffset = 0;
                    salaryOffset = 0;
                    selectedSkillBranch = 0;
                    skillPanX = 0;
                    skillPanY = 0;
                    topOffset = 0;
                    init();
                }).bounds(x, 18, 78, 20).build());
                x += 80;
            }

            if (currentTab != Tab.SALARY) {
                addRenderableWidget(claimSalaryButton("gui.advancedjobs.claim_salary", claimButtonX(), 18, 120));
            }

            switch (currentTab) {
                case PROFESSIONS -> addProfessionButtons();
                case MY_JOB -> addMyJobButtons();
                case SKILLS -> {
                }
                case SALARY -> addRenderableWidget(claimSalaryButton("gui.advancedjobs.claim_now", salaryActionButtonX(), salaryActionButtonY(), 120));
                case CONTRACTS -> addContractsButtons();
                case TOP -> addTopButtons();
                default -> {
                }
            }
            if (switchableJobTab() && ClientJobState.jobs().size() > 1) {
                addSelectedJobButtons();
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        switch (currentTab) {
            case PROFESSIONS -> {
                int max = Math.max(0, ClientJobState.jobs().size() - LIST_SIZE);
                listOffset = clampOffset(listOffset - (int) Math.signum(delta), max);
                init();
                return true;
            }
            case DAILY -> {
                int max = Math.max(0, visibleDailyTasks().size() - dailyVisibleRows());
                listOffset = clampOffset(listOffset - (int) Math.signum(delta), max);
                init();
                return true;
            }
            case CONTRACTS -> {
                int max = Math.max(0, visibleContracts().size() - contractsVisibleRows());
                listOffset = clampOffset(listOffset - (int) Math.signum(delta), max);
                init();
                return true;
            }
            case SALARY -> {
                if (isHovering(salaryViewportX(), salaryViewportY(), salaryViewportWidth(), salaryViewportHeight(), (int) mouseX, (int) mouseY)) {
                    int max = Math.max(0, salaryLines().size() - salaryVisibleRows());
                    salaryOffset = clampOffset(salaryOffset - (int) Math.signum(delta), max);
                    init();
                    return true;
                }
                return super.mouseScrolled(mouseX, mouseY, delta);
            }
            case TOP -> {
                int max = Math.max(0, leaderboardEntries().size() - TOP_PAGE_SIZE);
                topOffset = clampOffset(topOffset - (int) Math.signum(delta), max);
                init();
                return true;
            }
            case SKILLS -> {
                if (isHovering(skillViewportX(), skillViewportY(), skillViewportWidth(), skillViewportHeight(), (int) mouseX, (int) mouseY)) {
                    int max = Math.max(0, skillMaxRows() - skillVisibleRows());
                    skillOffset = clampOffset(skillOffset - (int) Math.signum(delta), max);
                    init();
                    return true;
                }
                return super.mouseScrolled(mouseX, mouseY, delta);
            }
            case MY_JOB -> {
                if (selectedJob == null) {
                    return super.mouseScrolled(mouseX, mouseY, delta);
                }
                if (isHovering(16, 288, myJobPassivePanelWidth(), myJobPassivePanelHeight(), (int) mouseX, (int) mouseY)) {
                    int max = Math.max(0, passivesOrEmpty(selectedJob).size() - myJobPassiveVisibleCount());
                    passiveOffset = clampOffset(passiveOffset - (int) Math.signum(delta), max);
                    init();
                    return true;
                }
                if (isHovering(myJobRightColumnX() - 4, 288, myJobRightPanelWidth(), myJobPerkPanelHeight(), (int) mouseX, (int) mouseY)) {
                    int max = Math.max(0, unlockedSkillEntries().size() - myJobPerkVisibleCount());
                    perkOffset = clampOffset(perkOffset - (int) Math.signum(delta), max);
                    init();
                    return true;
                }
                if (isHovering(myJobRightColumnX() - 4, myJobEffectsPanelY(), myJobRightPanelWidth(), myJobEffectPanelHeight(), (int) mouseX, (int) mouseY)) {
                    int max = Math.max(0, activeEffects().size() - myJobEffectVisibleCount());
                    effectOffset = clampOffset(effectOffset - (int) Math.signum(delta), max);
                    init();
                    return true;
                }
                return super.mouseScrolled(mouseX, mouseY, delta);
            }
            default -> {
                return super.mouseScrolled(mouseX, mouseY, delta);
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        syncSelectedJob();
        activeScrollbar = ScrollTarget.NONE;
        List<Component> tooltip = new ArrayList<>();
        if (usesCompactWindowLayout()) {
            graphics.fill(0, 0, this.width, this.height, 0xAA000000);
        }
        if (usesGlobalHud()) {
            graphics.drawCenteredString(this.font, this.title, this.width / 2, 2, 0xFFFFFF);
            int hudRight = hudRightX();
            int hudWidth = Math.max(160, Math.min(260, this.width / 4));
            drawRightAlignedClampedText(graphics,
                Component.translatable("gui.advancedjobs.balance", TextUtil.fmt2(ClientJobState.balance())),
                hudRight, 44, hudWidth, 0xFFD37F);
            drawRightAlignedClampedText(graphics,
                Component.translatable("gui.advancedjobs.active_job",
                    ClientJobState.activeJobId() == null ? Component.translatable("gui.advancedjobs.none") : jobName(ClientJobState.activeJobId())),
                hudRight, 58, hudWidth, 0x9BE39B);
            if (ClientJobState.allowSecondaryJob()) {
                drawRightAlignedClampedText(graphics,
                    Component.translatable("gui.advancedjobs.secondary_job",
                        ClientJobState.secondaryJobId() == null ? Component.translatable("gui.advancedjobs.none") : jobName(ClientJobState.secondaryJobId())),
                    hudRight, 72, hudWidth, 0x9AD0FF);
            }
        }

        switch (currentTab) {
            case PROFESSIONS -> renderProfessions(graphics, mouseX, mouseY, tooltip);
            case MY_JOB -> renderMyJob(graphics, mouseX, mouseY, tooltip);
            case SKILLS -> renderSkills(graphics, mouseX, mouseY, tooltip);
            case SALARY -> renderSalary(graphics);
            case DAILY -> renderDaily(graphics, mouseX, mouseY, tooltip);
            case CONTRACTS -> renderContracts(graphics, mouseX, mouseY, tooltip);
            case TOP -> renderTop(graphics, mouseX, mouseY, tooltip);
            case HELP -> renderHelp(graphics);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
        if (!tooltip.isEmpty()) {
            graphics.renderTooltip(this.font, tooltip, java.util.Optional.empty(), mouseX, mouseY);
        }
    }

    private boolean usesGlobalHud() {
        return !usesCompactWindowLayout();
    }

    private boolean usesCompactWindowLayout() {
        return currentTab == Tab.MY_JOB
            || currentTab == Tab.SKILLS
            || currentTab == Tab.DAILY
            || currentTab == Tab.CONTRACTS
            || currentTab == Tab.SALARY
            || currentTab == Tab.TOP;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && currentTab == Tab.SKILLS) {
            int hoveredTab = skillBranchTabAt((int) mouseX, (int) mouseY);
            if (hoveredTab >= 0) {
                selectedSkillBranch = hoveredTab;
                skillOffset = 0;
                skillPanX = 0;
                skillPanY = 0;
                init();
                return true;
            }
            JsonObject hoveredNode = skillNodeAt((int) mouseX, (int) mouseY);
            if (hoveredNode != null && !hoveredNode.get("unlocked").getAsBoolean() && selectedJob != null) {
                PacketHandler.CHANNEL.sendToServer(new UpgradePerkPacket(selectedJob.get("id").getAsString(), hoveredNode.get("id").getAsString()));
                return true;
            }
        }
        if (button == 0 && activeScrollbar != ScrollTarget.NONE
            && isHovering(activeScrollbarX, activeScrollbarY, 6, activeScrollbarHeight, (int) mouseX, (int) mouseY)) {
            draggingScrollbar = true;
            scrollToMouse(activeScrollbar, mouseY);
            init();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && draggingScrollbar && activeScrollbar != ScrollTarget.NONE) {
            scrollToMouse(activeScrollbar, mouseY);
            init();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            draggingScrollbar = false;
            draggingSkillTree = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void addProfessionButtons() {
        List<JsonObject> jobs = ClientJobState.jobs();
        int primaryX = actionButtonPrimaryX();
        int secondaryX = actionButtonSecondaryX();
        for (int i = 0; i < Math.min(LIST_SIZE, Math.max(0, jobs.size() - listOffset)); i++) {
            JsonObject job = jobs.get(listOffset + i);
            String id = job.get("id").getAsString();
            int y = 84 + i * 24;
            addRenderableWidget(Button.builder(Component.translatable("gui.advancedjobs.select_primary"), button -> {
                PacketHandler.CHANNEL.sendToServer(new ChooseJobPacket(id, false));
                selectedJob = job;
            }).bounds(primaryX, y, 86, 20).build());
            if (ClientJobState.allowSecondaryJob()) {
                addRenderableWidget(Button.builder(Component.translatable("gui.advancedjobs.select_secondary"), button -> {
                    PacketHandler.CHANNEL.sendToServer(new ChooseJobPacket(id, true));
                    selectedJob = job;
                }).bounds(secondaryX, y, 86, 20).build());
            }
        }
    }

    private void addSkillButtons() {
        List<JsonObject> entries = skillEntries();
        int unlockX = skillUnlockButtonX();
        for (int i = 0; i < Math.min(LIST_SIZE, Math.max(0, entries.size() - skillOffset)); i++) {
            JsonObject node = entries.get(skillOffset + i);
            if (!node.get("unlocked").getAsBoolean()) {
                String nodeId = node.get("id").getAsString();
                String jobId = selectedJob != null ? selectedJob.get("id").getAsString() : null;
                if (jobId != null) {
                    addRenderableWidget(Button.builder(Component.translatable("gui.advancedjobs.unlock"), button ->
                        PacketHandler.CHANNEL.sendToServer(new UpgradePerkPacket(jobId, nodeId)))
                        .bounds(unlockX, 84 + i * 24, 80, 20)
                        .build());
                }
            }
        }
    }

    private void addMyJobButtons() {
        int clearX = wideActionButtonX();
        if (ClientJobState.activeJobId() != null) {
            addRenderableWidget(Button.builder(Component.translatable("gui.advancedjobs.clear_primary"), button ->
                PacketHandler.CHANNEL.sendToServer(new LeaveJobPacket(false)))
                .bounds(clearX, 84, 120, 20)
                .build());
        }
        if (ClientJobState.allowSecondaryJob() && ClientJobState.secondaryJobId() != null) {
            addRenderableWidget(Button.builder(Component.translatable("gui.advancedjobs.clear_secondary"), button ->
                PacketHandler.CHANNEL.sendToServer(new LeaveJobPacket(true)))
                .bounds(clearX, 108, 120, 20)
                .build());
        }
    }

    private void addSelectedJobButtons() {
        int leftX = actionButtonPrimaryX();
        int rightX = selectedJobNextButtonX();
        addRenderableWidget(Button.builder(Component.literal("<"), button -> {
            shiftSelectedJob(-1);
            init();
        }).bounds(leftX, 56, 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), button -> {
            shiftSelectedJob(1);
            init();
        }).bounds(rightX, 56, 20, 20).build());
    }

    private void addTopButtons() {
        List<JsonObject> jobs = ClientJobState.jobs();
        if (jobs.isEmpty()) {
            return;
        }
        leaderboardJobIndex = clampOffset(leaderboardJobIndex, jobs.size());
        requestLeaderboard(currentLeaderboardJobId());
        addRenderableWidget(Button.builder(Component.literal("<"), button -> {
            leaderboardJobIndex = (leaderboardJobIndex - 1 + jobs.size() + 1) % (jobs.size() + 1);
            topOffset = 0;
            requestLeaderboard(currentLeaderboardJobId());
            init();
        }).bounds(topJobPrevButtonX(), topControlsY(), 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), button -> {
            leaderboardJobIndex = (leaderboardJobIndex + 1) % (jobs.size() + 1);
            topOffset = 0;
            requestLeaderboard(currentLeaderboardJobId());
            init();
        }).bounds(topJobNextButtonX(), topControlsY(), 20, 20).build());
        int topNextX = topPageNextButtonX();
        int topPrevX = topPagePrevButtonX();
        int topSortX = topSortButtonX();
        addRenderableWidget(Button.builder(Component.translatable("gui.advancedjobs.prev"), button -> {
            topOffset = clampOffset(topOffset - TOP_PAGE_SIZE, Math.max(0, leaderboardEntries().size() - TOP_PAGE_SIZE));
            init();
        }).bounds(topPrevX, topControlsY(), 50, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.advancedjobs.next"), button -> {
            topOffset = clampOffset(topOffset + TOP_PAGE_SIZE, Math.max(0, leaderboardEntries().size() - TOP_PAGE_SIZE));
            init();
        }).bounds(topNextX, topControlsY(), 50, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.advancedjobs.sort_button"), button -> {
            topSort = topSort.next();
            topOffset = 0;
            init();
        }).bounds(topSortX, topControlsY(), 96, 20).build());
    }

    private void addContractsButtons() {
        if (selectedJob == null) {
            return;
        }
        Button reroll = Button.builder(Component.translatable("gui.advancedjobs.reroll_contracts"), button ->
                PacketHandler.CHANNEL.sendToServer(new RerollContractsPacket(selectedJob.get("id").getAsString())))
            .bounds(contractsActionButtonX(), contractsActionButtonY(), 140, 20)
            .build();
        reroll.active = ClientJobState.contractRerollCooldownRemaining() <= 0L;
        addRenderableWidget(reroll);
    }

    private void renderProfessions(GuiGraphics graphics, int mouseX, int mouseY, List<Component> tooltip) {
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.professions"), 20, 54, contentWidth(), 0xFFFFFF);
        List<JsonObject> jobs = ClientJobState.jobs();
        JsonObject detailsJob = selectedJob;
        int compactDetailsPanelX = professionDetailsPanelX();
        drawListStatus(graphics, jobs.size(), LIST_SIZE, listOffset, Math.max(220, compactDetailsPanelX - 120), 54);
        int listRowRight = Math.max(280, compactDetailsPanelX - 16);
        int listNameX = 42;
        int listLevelX = 42;
        int progressX = 132;
        int progressWidth = Math.max(72, Math.min(BAR_WIDTH, listRowRight - progressX - 8));
        int titleWidth = Math.max(90, Math.min(118, progressX - listNameX - 8));
        int slotX = listNameX + titleWidth + 6;
        int descX = progressX + progressWidth + 8;
        int descWidth = Math.max(96, listRowRight - descX);
        int levelWidth = Math.max(80, progressX - listLevelX - 8);
        for (int i = 0; i < Math.min(LIST_SIZE, Math.max(0, jobs.size() - listOffset)); i++) {
            JsonObject job = jobs.get(listOffset + i);
            int y = 86 + i * 24;
            String jobId = job.get("id").getAsString();
            boolean active = jobId.equals(ClientJobState.activeJobId());
            int titleColor = active ? 0x7FE38A : 0xFFFFFF;
            renderJobIcon(graphics, job, 20, y - 2);
            drawClampedText(graphics, Component.translatable(job.get("nameKey").getAsString()), listNameX, y, titleWidth, titleColor);
            Component slotBadge = professionSlotBadge(jobId);
            if (!slotBadge.getString().isEmpty()) {
                drawClampedText(graphics, slotBadge, slotX, y, Math.max(36, progressX - slotX - 8), slotBadgeColor(jobId));
            }
            drawClampedText(graphics, Component.translatable(job.get("descriptionKey").getAsString()),
                descX, y, descWidth, 0xB8B8B8);
            drawClampedText(graphics, Component.translatable("gui.advancedjobs.level_xp", job.get("level").getAsInt(), TextUtil.fmt2(job.get("xp").getAsDouble())),
                listLevelX, y + 10, levelWidth, 0x9AD0FF);
            double requiredXp = requiredXp(job);
            drawProgressBar(graphics, progressX, y + 10, progressWidth, 8, job.get("xp").getAsDouble(), requiredXp, 0x334455, active ? 0x4CAF50 : 0x4A90E2);
            if (isHovering(20, y - 2, listRowRight - 20, 22, mouseX, mouseY)) {
                detailsJob = job;
                tooltip.add(Component.translatable(job.get("nameKey").getAsString()));
                tooltip.add(Component.translatable(job.get("descriptionKey").getAsString()));
                tooltip.add(Component.translatable("gui.advancedjobs.tooltip.level_xp", job.get("level").getAsInt(), TextUtil.fmt2(job.get("xp").getAsDouble()), TextUtil.fmt2(requiredXp)));
                tooltip.add(Component.translatable("gui.advancedjobs.tooltip.slot", selectedJobSlotLabel(jobId)));
                tooltip.add(Component.translatable("gui.advancedjobs.tooltip.daily_contracts", countEntries(job, "dailyTasks"), countEntries(job, "contracts")));
                tooltip.add(Component.translatable("gui.advancedjobs.tooltip.content_summary",
                    job.has("rewardCount") ? job.get("rewardCount").getAsInt() : 0,
                    job.has("passives") ? job.getAsJsonArray("passives").size() : 0,
                    job.has("skillBranches") ? job.getAsJsonArray("skillBranches").size() : 0));
                appendPassivePreviewTooltip(job, tooltip);
                if (!compactProfessionTooltip()) {
                    appendBranchPreviewTooltip(job, tooltip);
                }
                appendRewardPreviewTooltip(job, tooltip);
            }
        }
        drawScrollbar(graphics, ScrollTarget.PROFESSIONS, listRowRight + 4, 84, LIST_SIZE * 24, jobs.size(), LIST_SIZE, listOffset);
        renderProfessionDetails(graphics, detailsJob);
    }

    private void renderMyJob(GuiGraphics graphics, int mouseX, int mouseY, List<Component> tooltip) {
        int panelX = myJobWindowX();
        int panelY = myJobWindowY();
        int panelWidth = myJobWindowWidth();
        int panelHeight = myJobWindowHeight();
        int viewportX = myJobViewportX();
        int viewportY = myJobViewportY();
        int viewportWidth = myJobViewportWidth();
        int viewportHeight = myJobViewportHeight();

        drawCompactWindowFrame(graphics, panelX, panelY, panelWidth, panelHeight);
        graphics.enableScissor(viewportX, viewportY, viewportX + viewportWidth, viewportY + viewportHeight);
        drawSkillViewportBackground(graphics, viewportX, viewportY, viewportWidth, viewportHeight);
        graphics.disableScissor();

        if (selectedJob == null) {
            drawCompactWindowHeader(graphics, panelX, panelY, panelWidth,
                Component.translatable("gui.advancedjobs.my_job"),
                null);
            drawClampedText(graphics, Component.translatable("gui.advancedjobs.no_active_profession"),
                viewportX + 8, viewportY + 8, viewportWidth - 16, 0xD0D0D0);
            return;
        }

        Component jobName = Component.translatable(selectedJob.get("nameKey").getAsString());
        drawCompactWindowHeader(graphics, panelX, panelY, panelWidth,
            Component.translatable("gui.advancedjobs.my_job"),
            Component.translatable("gui.advancedjobs.level_short", selectedJob.get("level").getAsInt()));

        int leftX = viewportX + 8;
        int leftWidth = 156;
        int rightX = viewportX + 176;
        int rightWidth = viewportWidth - 184;
        int summaryY = viewportY + 8;
        int summaryLineWidth = leftWidth - 4;

        drawClampedText(graphics, jobName, leftX, summaryY, summaryLineWidth, 0xFFFFFF);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.level", selectedJob.get("level").getAsInt()), leftX, summaryY + 14, summaryLineWidth, 0x9BE39B);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.xp", TextUtil.fmt2(selectedJob.get("xp").getAsDouble())), leftX, summaryY + 26, summaryLineWidth, 0x9AD0FF);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.pending_salary", TextUtil.fmt2(selectedJob.get("pendingSalary").getAsDouble())), leftX, summaryY + 38, summaryLineWidth, 0xFFD37F);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.skill_points", selectedJob.get("skillPoints").getAsInt()), leftX, summaryY + 50, summaryLineWidth, 0xFFBE7F);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.job_slot", selectedJobSlotLabel()), leftX, summaryY + 62, summaryLineWidth, 0x9AD0FF);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.my_job_summary",
            completedEntries(selectedJob, "dailyTasks"), countEntries(selectedJob, "dailyTasks"),
            completedEntries(selectedJob, "contracts"), countEntries(selectedJob, "contracts")), leftX, summaryY + 74, summaryLineWidth, 0xD0D0D0);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.my_job_history",
            selectedJob.has("completedDailyCount") ? selectedJob.get("completedDailyCount").getAsInt() : 0,
            selectedJob.has("completedContractCount") ? selectedJob.get("completedContractCount").getAsInt() : 0), leftX, summaryY + 86, summaryLineWidth, 0xB8B8B8);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.unlocked_titles",
            ClientJobState.unlockedTitles().size(), latestUnlockedTitle()), leftX, summaryY + 98, summaryLineWidth, 0xCFAF6A);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.milestones",
            selectedJob.has("milestoneCount") ? selectedJob.get("milestoneCount").getAsInt() : 0,
            latestMilestone(selectedJob)), leftX, summaryY + 110, summaryLineWidth, 0x9AD0FF);
        if (isHovering(leftX, summaryY + 98, summaryLineWidth, 12, mouseX, mouseY)) {
            appendTitleTooltip(tooltip);
        }
        if (isHovering(leftX, summaryY + 110, summaryLineWidth, 12, mouseX, mouseY)) {
            appendMilestoneTooltip(selectedJob, tooltip);
        }

        double requiredXp = requiredXp(selectedJob);
        drawProgressBar(graphics, leftX, viewportY + 126, leftWidth - 12, 8, selectedJob.get("xp").getAsDouble(), requiredXp, 0x334455, 0x4CAF50);
        if (isHovering(leftX, viewportY + 126, leftWidth - 12, 8, mouseX, mouseY)) {
            tooltip.add(Component.translatable("gui.advancedjobs.tooltip.profession_xp"));
            tooltip.add(Component.translatable("gui.advancedjobs.tooltip.value_of", TextUtil.fmt2(selectedJob.get("xp").getAsDouble()), TextUtil.fmt2(requiredXp)));
        }

        JsonArray passives = passivesOrEmpty(selectedJob);
        int passiveVisible = 2;
        int passiveY = viewportY + 142;
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.passives_short"), leftX, passiveY, leftWidth - 16, 0xE0D6AF);
        for (int i = 0; i < Math.min(passiveVisible, Math.max(0, passives.size() - passiveOffset)); i++) {
            JsonObject passive = passives.get(passiveOffset + i).getAsJsonObject();
            JsonArray keys = passive.getAsJsonArray("keys");
            String key = keys.size() > 0 ? keys.get(0).getAsString() : "missing";
            int rowY = passiveY + 14 + i * 28;
            boolean hovered = isHovering(leftX, rowY, leftWidth - 10, 24, mouseX, mouseY);
            renderVanillaTaskRow(graphics, leftX, rowY, leftWidth - 10, 24,
                new ItemStack(Items.BOOK),
                translatedOrFallback(key, fallbackPassiveName(key, passive.get("level").getAsInt())),
                Component.translatable("gui.advancedjobs.achievement_passive_status", passive.get("level").getAsInt()),
                0xFFC69D51,
                hovered);
            if (hovered) {
                appendPassiveCardTooltip(passive, tooltip);
            }
        }
        drawScrollbar(graphics, ScrollTarget.MY_JOB_PASSIVES, leftX + leftWidth - 8, passiveY + 14, passiveVisible * 28 - 4, passives.size(), passiveVisible, passiveOffset);

        List<JsonObject> unlockedPerks = unlockedSkillEntries();
        int perkVisible = 2;
        int perkY = viewportY + 8;
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.perks_short"), rightX, perkY, rightWidth - 12, 0xC7DBFF);
        if (unlockedPerks.isEmpty()) {
            boolean hovered = isHovering(rightX, perkY + 14, rightWidth - 6, 24, mouseX, mouseY);
            renderVanillaTaskRow(graphics, rightX, perkY + 14, rightWidth - 6, 24,
                new ItemStack(Items.PAPER),
                Component.translatable("gui.advancedjobs.active_perks"),
                Component.translatable("gui.advancedjobs.no_unlocked_perks"),
                0xFF7B879A,
                hovered);
            if (hovered) {
                tooltip.add(Component.translatable("gui.advancedjobs.no_unlocked_perks"));
            }
        } else {
            for (int i = 0; i < Math.min(perkVisible, Math.max(0, unlockedPerks.size() - perkOffset)); i++) {
                JsonObject node = unlockedPerks.get(perkOffset + i);
                int rowY = perkY + 14 + i * 28;
                boolean hovered = isHovering(rightX, rowY, rightWidth - 6, 24, mouseX, mouseY);
                renderVanillaTaskRow(graphics, rightX, rowY, rightWidth - 6, 24,
                    skillNodeIconStack(node),
                    translatedOrFallback(node.get("translationKey").getAsString(), fallbackNodeName(node)),
                    Component.translatable("gui.advancedjobs.achievement_perk_status", effectLabel(node.get("effectType").getAsString())),
                    perkAccentColor(node),
                    hovered);
                if (hovered) {
                    appendUnlockedPerkCardTooltip(node, tooltip);
                }
            }
        }
        drawScrollbar(graphics, ScrollTarget.MY_JOB_PERKS, rightX + rightWidth - 4, perkY + 14, perkVisible * 28 - 4, unlockedPerks.size(), perkVisible, perkOffset);

        List<MobEffectInstance> activeEffects = activeEffects();
        int effectVisible = 2;
        int effectY = viewportY + 82;
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.bonuses_short"), rightX, effectY, rightWidth - 12, 0xC6FFD7);
        if (activeEffects.isEmpty()) {
            boolean hovered = isHovering(rightX, effectY + 14, rightWidth - 6, 24, mouseX, mouseY);
            renderVanillaTaskRow(graphics, rightX, effectY + 14, rightWidth - 6, 24,
                new ItemStack(Items.GLASS_BOTTLE),
                Component.translatable("gui.advancedjobs.active_effects"),
                Component.translatable("gui.advancedjobs.no_active_effects"),
                0xFF6F8475,
                hovered);
            if (hovered) {
                tooltip.add(Component.translatable("gui.advancedjobs.no_active_effects"));
            }
        } else {
            for (int i = 0; i < Math.min(effectVisible, Math.max(0, activeEffects.size() - effectOffset)); i++) {
                MobEffectInstance effect = activeEffects.get(effectOffset + i);
                int rowY = effectY + 14 + i * 28;
                boolean hovered = isHovering(rightX, rowY, rightWidth - 6, 24, mouseX, mouseY);
                renderVanillaTaskRow(graphics, rightX, rowY, rightWidth - 6, 24,
                    new ItemStack(Items.EXPERIENCE_BOTTLE),
                    effect.getEffect().getDisplayName(),
                    Component.translatable("gui.advancedjobs.achievement_effect_status",
                        effect.getAmplifier() + 1,
                        TimeUtil.formatRemainingSeconds(Math.max(0, effect.getDuration() / 20))),
                    0xFF76E0A0,
                    hovered);
                if (hovered) {
                    appendActiveEffectCardTooltip(effect, tooltip);
                }
            }
        }
        drawScrollbar(graphics, ScrollTarget.MY_JOB_EFFECTS, rightX + rightWidth - 4, effectY + 14, effectVisible * 28 - 4, activeEffects.size(), effectVisible, effectOffset);

        int rewardY = viewportY + 156;
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.reward_modifiers_short"), rightX, rewardY, rightWidth - 12, 0xE0D6FF);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.help.world_multiplier",
            targetLabel(ClientJobState.currentWorldId()), TextUtil.fmt2(ClientJobState.worldRewardMultiplier())), rightX, rewardY + 12, rightWidth - 12, 0xD0D0D0);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.help.biome_multiplier",
            targetLabel(ClientJobState.currentBiomeId()), TextUtil.fmt2(ClientJobState.biomeRewardMultiplier())), rightX, rewardY + 24, rightWidth - 12, 0xD0D0D0);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.my_job_modifier_stack",
            TextUtil.fmt2(ClientJobState.vipRewardMultiplier()),
            TextUtil.fmt2(ClientJobState.eventRewardMultiplier()),
            TextUtil.fmt2(ClientJobState.effectiveRewardMultiplier())), rightX, rewardY + 36, rightWidth - 12, 0x9BE39B);
    }

    private int myJobWindowWidth() {
        return 420;
    }

    private int myJobWindowHeight() {
        return 268;
    }

    private int myJobWindowX() {
        return (this.width - myJobWindowWidth()) / 2;
    }

    private int myJobWindowY() {
        return Math.max(44, (this.height - myJobWindowHeight()) / 2);
    }

    private int myJobViewportX() {
        return myJobWindowX() + 9;
    }

    private int myJobViewportY() {
        return myJobWindowY() + 18;
    }

    private int myJobViewportWidth() {
        return myJobWindowWidth() - 18;
    }

    private int myJobViewportHeight() {
        return myJobWindowHeight() - 27;
    }

    private boolean compactMyJobLists() {
        return this.width <= 1100 || this.height <= 760;
    }

    private int myJobPassiveVisibleCount() {
        return compactMyJobLists() ? 3 : 5;
    }

    private int myJobPerkVisibleCount() {
        return compactMyJobLists() ? 3 : 4;
    }

    private int myJobEffectVisibleCount() {
        return compactMyJobLists() ? 3 : 4;
    }

    private int myJobRightColumnX() {
        return Math.min(340, Math.max(280, this.width / 2 + 8));
    }

    private int myJobRightPanelWidth() {
        return Math.max(180, remainingWidth(myJobRightColumnX(), 24, 170) + 28);
    }

    private int myJobPassivePanelWidth() {
        int myJobLeftListWidth = Math.max(180, Math.min(280, this.width / 2 - 60));
        int passiveCardWidth = Math.max(180, myJobLeftListWidth);
        return passiveCardWidth + 28;
    }

    private int myJobPassivePanelHeight() {
        return 20 + Math.max(1, Math.min(myJobPassiveVisibleCount(), passivesOrEmpty(selectedJob).size())) * 28
            + (passivesOrEmpty(selectedJob).size() > myJobPassiveVisibleCount() + passiveOffset ? 14 : 0);
    }

    private int myJobPerkPanelHeight() {
        List<JsonObject> perks = unlockedSkillEntries();
        return 20 + Math.max(1, Math.min(myJobPerkVisibleCount(), perks.size())) * 28
            + (perks.size() > myJobPerkVisibleCount() + perkOffset ? 14 : 0);
    }

    private int myJobEffectsPanelY() {
        return 300 + myJobPerkPanelHeight() + 16;
    }

    private int myJobEffectPanelHeight() {
        List<MobEffectInstance> effects = activeEffects();
        return 20 + Math.max(1, Math.min(myJobEffectVisibleCount(), effects.size())) * 28
            + (effects.size() > myJobEffectVisibleCount() + effectOffset ? 14 : 0);
    }

    private JsonArray passivesOrEmpty(JsonObject job) {
        return job != null && job.has("passives") ? job.getAsJsonArray("passives") : new JsonArray();
    }

    private void drawAchievementPanel(GuiGraphics graphics, int x, int y, int width, int height, int borderColor, int fillColor) {
        graphics.fill(x, y, x + width, y + height, 0xAA101010);
        graphics.fill(x, y, x + width, y + 1, 0xFF8B8B8B);
        graphics.fill(x, y, x + 1, y + height, 0xFF8B8B8B);
        graphics.fill(x, y + height - 1, x + width, y + height, 0xFF2A2A2A);
        graphics.fill(x + width - 1, y, x + width, y + height, 0xFF2A2A2A);
        graphics.fill(x + 6, y + 6, x + 8, y + height - 6, borderColor & 0x66FFFFFF);
    }

    private void drawAchievementConnector(GuiGraphics graphics, int centerX, int y, int color) {
        graphics.fill(centerX, y, centerX + 2, y + 8, color);
        graphics.fill(centerX - 2, y + 6, centerX + 4, y + 8, color);
    }

    private void renderAchievementCard(GuiGraphics graphics, int x, int y, int width, int height, Component badge, Component title,
                                       Component status, int accentColor, int fillColor, int borderColor,
                                       boolean unlocked, boolean hovered, boolean glowing) {
        if (glowing) {
            graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, accentColor & 0x22FFFFFF);
        }
        graphics.fill(x, y, x + width, y + height, 0xFF6A6A6A);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF2F2F2F);
        if (hovered) {
            graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0x22FFFFFF);
        }
        graphics.fill(x + 2, y + 2, x + width - 2, y + 4, unlocked ? accentColor : 0xFF555555);
        graphics.fill(x + 4, y + 4, x + 20, y + 20, unlocked ? 0xFF8B8B8B : 0xFF686868);
        graphics.fill(x + 5, y + 5, x + 19, y + 19, unlocked ? accentColor : 0xFF3E3E3E);
        graphics.drawCenteredString(this.font, badge, x + 12, y + 9, unlocked ? 0xFFF6E2A5 : 0xFFB8B8B8);
        graphics.fill(x + 24, y + height - 4, x + width - 4, y + height - 3, unlocked ? accentColor : 0xFF555555);
        drawClampedText(graphics, title, x + 28, y + 4, Math.max(80, width - 34), unlocked ? 0xFFFFFF : 0xFFD0D0D0);
        drawClampedText(graphics, status, x + 28, y + 14, Math.max(80, width - 34), unlocked ? 0xC6C6C6 : 0xA0A0A0);
    }

    private int perkAccentColor(JsonObject node) {
        int requiredLevel = node.has("requiredLevel") ? node.get("requiredLevel").getAsInt() : 0;
        int cost = node.has("cost") ? node.get("cost").getAsInt() : 0;
        if (requiredLevel >= 30 || cost >= 4) {
            return 0xFFFFC857;
        }
        if (requiredLevel >= 20 || cost >= 3) {
            return 0xFFA86CFF;
        }
        return 0xFF7EB6FF;
    }

    private void renderSkills(GuiGraphics graphics, int mouseX, int mouseY, List<Component> tooltip) {
        if (selectedJob == null) {
            drawClampedText(graphics, Component.translatable("gui.advancedjobs.skills"), 20, 54, contentWidth(), 0xFFFFFF);
            drawClampedText(graphics, Component.translatable("gui.advancedjobs.select_profession_first"), 20, 84, contentWidth(), 0xD0D0D0);
            return;
        }
        JsonArray branches = selectedJob.has("skillBranches") ? selectedJob.getAsJsonArray("skillBranches") : new JsonArray();
        if (branches.isEmpty()) {
            drawClampedText(graphics, Component.translatable("gui.advancedjobs.skills"), 20, 54, contentWidth(), 0xFFFFFF);
            drawClampedText(graphics, Component.translatable("gui.advancedjobs.no_skills_available"), 20, 96, contentWidth(), 0xD0D0D0);
            return;
        }
        int panelX = skillWindowX();
        int panelY = skillWindowY();
        int panelWidth = skillWindowWidth();
        int panelHeight = skillWindowHeight();
        int contentX = skillViewportX();
        int contentY = skillViewportY();
        int contentWidth = skillViewportWidth();
        int contentHeight = skillViewportHeight();
        JsonObject currentBranch = currentSkillBranch();
        drawVanillaAdvancementWindow(graphics, panelX, panelY, panelWidth, panelHeight);
        drawVanillaSkillsHeader(graphics, panelX, panelY, panelWidth);
        renderSkillBranchTabs(graphics, branches, mouseX, mouseY);
        drawCompactWindowHeader(graphics, panelX, panelY, panelWidth,
            Component.translatable("gui.advancedjobs.skills"),
            Component.translatable("gui.advancedjobs.skill_points_short", selectedJob.get("skillPoints").getAsInt()));
        drawSkillViewportBackground(graphics, contentX, contentY, contentWidth, contentHeight);
        int hoveredTab = skillBranchTabAt(mouseX, mouseY);
        if (hoveredTab >= 0 && hoveredTab < branches.size()) {
            JsonObject branch = branches.get(hoveredTab).getAsJsonObject();
            tooltip.add(translatedOrFallback(branch.get("translationKey").getAsString(), humanizeId(branch.get("id").getAsString())));
        }
        List<SkillNodeLayout> layouts = currentBranch != null ? buildSkillNodeLayouts(currentBranch) : List.of();
        graphics.enableScissor(contentX, contentY, contentX + contentWidth, contentY + contentHeight);
        if (currentBranch != null) {
            for (SkillNodeLayout layout : layouts) {
                if (!layout.visible()) {
                    continue;
                }
                JsonObject node = layout.node();
                boolean hovered = isHovering(layout.x(), layout.y(), layout.width(), layout.height(), mouseX, mouseY);
                SkillNodeState state = skillNodeState(node);
                renderVanillaSkillNode(graphics, layout.x(), layout.y(), layout.width(), layout.height(),
                    skillNodeIconStack(node),
                    translatedOrFallback(node.get("translationKey").getAsString(), fallbackNodeName(node)),
                    skillNodeStatus(node), state, hovered);
                if (hovered) {
                    tooltip.add(translatedOrFallback(node.get("translationKey").getAsString(), fallbackNodeName(node)));
                    tooltip.add(skillNodeTooltipState(node));
                    tooltip.add(Component.translatable("gui.advancedjobs.tooltip.branch",
                        translatedOrFallback(currentBranch.get("translationKey").getAsString(), humanizeId(currentBranch.get("id").getAsString())).getString()));
                    tooltip.add(Component.translatable("gui.advancedjobs.tooltip.effect",
                        effectLabel(node.get("effectType").getAsString()), node.get("effectValue").getAsDouble()));
                    tooltip.add(Component.translatable("gui.advancedjobs.tooltip.skill_requirements",
                        selectedJob != null ? selectedJob.get("level").getAsInt() : 0,
                        node.get("requiredLevel").getAsInt(),
                        selectedJob != null ? selectedJob.get("skillPoints").getAsInt() : 0,
                        node.get("cost").getAsInt()));
                    if (node.has("parentId")) {
                        tooltip.add(Component.translatable("gui.advancedjobs.parent",
                            skillNodeNameById(node.get("parentId").getAsString())));
                    }
                }
            }
        }
        graphics.disableScissor();
        if (currentBranch != null) {
            drawScrollbar(graphics, ScrollTarget.SKILLS, contentX + contentWidth - 6, contentY + 1, contentHeight - 2,
                skillMaxRows(), skillVisibleRows(), skillOffset);
        }
    }

    private boolean compactSkillsLayout() {
        return this.width <= 1100;
    }

    private int skillVisibleRows() {
        int contentHeight = skillViewportHeight() - 12;
        return Math.max(2, contentHeight / 30);
    }

    private int skillMaxRows() {
        JsonObject branch = currentSkillBranch();
        if (branch == null) {
            return 0;
        }
        return branch.getAsJsonArray("nodes").size();
    }

    private JsonObject skillNodeAt(int mouseX, int mouseY) {
        JsonObject branch = currentSkillBranch();
        if (branch == null) {
            return null;
        }
        for (SkillNodeLayout layout : buildSkillNodeLayouts(branch)) {
            if (layout.visible() && isHovering(layout.x(), layout.y(), layout.width(), layout.height(), mouseX, mouseY)) {
                return layout.node();
            }
        }
        return null;
    }

    private int skillWindowX() {
        return (this.width - skillWindowWidth()) / 2;
    }

    private int skillWindowY() {
        return Math.max(32, (this.height - skillWindowHeight()) / 2);
    }

    private int skillWindowWidth() {
        return 252;
    }

    private int skillWindowHeight() {
        return 140;
    }

    private int skillViewportX() {
        return skillWindowX() + 9;
    }

    private int skillViewportY() {
        return skillWindowY() + 18;
    }

    private int skillViewportWidth() {
        return 234;
    }

    private int skillViewportHeight() {
        return 113;
    }

    private void drawVanillaAdvancementWindow(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.blit(ADVANCEMENTS_WINDOW, x, y, 0, 0, width, height);
    }

    private void drawCompactWindowFrame(GuiGraphics graphics, int x, int y, int width, int height) {
        int right = x + width;
        int bottom = y + height;
        int headerBottom = y + 27;
        graphics.fill(x, y, right, bottom, 0xFF000000);
        graphics.fill(x + 1, y + 1, right - 1, bottom - 1, 0xFFC6C6C6);
        graphics.fill(x + 2, y + 2, right - 2, bottom - 2, 0xFF8B8B8B);
        graphics.fill(x + 3, y + 3, right - 3, bottom - 3, 0xFF2F2F2F);
        graphics.fill(x + 4, y + 4, right - 4, headerBottom, 0xFFDADADA);
        graphics.fill(x + 4, headerBottom, right - 4, headerBottom + 1, 0xFF555555);
        graphics.fill(x + 4, headerBottom + 1, right - 4, bottom - 4, 0xFF1E1E1E);
    }

    private void drawVanillaSkillsHeader(GuiGraphics graphics, int x, int y, int width) {
        // Vanilla advancements use the window texture directly for the title row.
    }

    private void drawCompactWindowHeader(GuiGraphics graphics, int panelX, int panelY, int panelWidth, Component title, Component status) {
        graphics.drawString(this.font, title, panelX + 8, panelY + 6, 4210752, false);
        if (status != null) {
            graphics.drawString(this.font, status,
                panelX + panelWidth - 8 - this.font.width(status), panelY + 6, 0x5A6B40, false);
        }
    }

    private void drawSkillViewportBackground(GuiGraphics graphics, int x, int y, int width, int height) {
        for (int tileY = y; tileY < y + height; tileY += 16) {
            for (int tileX = x; tileX < x + width; tileX += 16) {
                int drawWidth = Math.min(16, x + width - tileX);
                int drawHeight = Math.min(16, y + height - tileY);
                graphics.blit(ADVANCEMENTS_STONE, tileX, tileY, drawWidth, drawHeight, 0.0F, 0.0F, 16, 16, 16, 16);
            }
        }
    }

    private void renderVanillaSkillNode(GuiGraphics graphics, int x, int y, int width, int height, ItemStack iconStack,
                                        Component title, Component status, SkillNodeState state, boolean hovered) {
        boolean unlocked = state == SkillNodeState.UNLOCKED;
        int textureY = unlocked ? 0 : 26;
        graphics.blit(ADVANCEMENTS_WIDGETS, x, y, 0, textureY, 200, 26);
        graphics.fill(x + 1, y + 22, x + width - 1, y + 25, skillNodeStateColor(state));
        if (hovered) {
            graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0x12FFFFFF);
        }
        graphics.renderFakeItem(iconStack.isEmpty() ? new ItemStack(Items.PAPER) : iconStack, x + 8, y + 5);
        int statusIconSize = 16;
        int statusIconX = x + width - statusIconSize - 10;
        int titleWidth = Math.max(56, statusIconX - (x + 32) - 8);
        drawClampedText(graphics, title, x + 32, y + 9, titleWidth, 0xFFFFFF);
        drawSkillStatusIcon(graphics, statusIconX, y + 5, state, hovered);
    }

    private void renderSkillBranchTabs(GuiGraphics graphics, JsonArray branches, int mouseX, int mouseY) {
        int tabWidth = skillBranchTabWidth(branches.size());
        int baseX = skillBranchBaseX(branches.size(), tabWidth);
        int tabY = skillWindowY() - 28;
        for (int i = 0; i < branches.size(); i++) {
            JsonObject branch = branches.get(i).getAsJsonObject();
            int x = baseX + i * (tabWidth + 4);
            boolean selected = i == selectedSkillBranch;
            boolean hovered = isHovering(x, tabY, tabWidth, 32, mouseX, mouseY);
            drawVanillaTab(graphics, x, tabY, i, selected);
            graphics.renderFakeItem(skillBranchIconStack(branch.get("id").getAsString()), x + 6, tabY + 9);
        }
    }

    private int skillBranchTabAt(int mouseX, int mouseY) {
        if (selectedJob == null || !selectedJob.has("skillBranches")) {
            return -1;
        }
        JsonArray branches = selectedJob.getAsJsonArray("skillBranches");
        int tabWidth = skillBranchTabWidth(branches.size());
        int baseX = skillBranchBaseX(branches.size(), tabWidth);
        int tabY = skillWindowY() - 28;
        for (int i = 0; i < branches.size(); i++) {
            int x = baseX + i * (tabWidth + 4);
            if (isHovering(x, tabY, tabWidth, 32, mouseX, mouseY)) {
                return i;
            }
        }
        return -1;
    }

    private int skillBranchTabWidth(int branchCount) {
        return 28;
    }

    private boolean compactSkillBranchTabs(int branchCount) {
        return true;
    }

    private int skillBranchBaseX(int branchCount, int tabWidth) {
        int totalWidth = branchCount * tabWidth + Math.max(0, branchCount - 1) * 4;
        return skillWindowX() + 8;
    }

    private void drawVanillaTab(GuiGraphics graphics, int x, int y, int index, boolean selected) {
        int u = 0;
        if (index > 0) {
            u += 28;
        }
        if (selected) {
            graphics.blit(ADVANCEMENTS_TABS, x, y, u, 32, 28, 32);
        } else {
            graphics.blit(ADVANCEMENTS_TABS, x, y, u, 0, 28, 32);
        }
    }

    private void drawVanillaListRowFrame(GuiGraphics graphics, int x, int y, int width, int height, int accentColor, boolean hovered) {
        graphics.fill(x, y, x + width, y + height, 0xFF8B8B8B);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF2F2F2F);
        graphics.fill(x + 2, y + 2, x + width - 2, y + 3, accentColor);
        graphics.fill(x + 2, y + height - 3, x + width - 2, y + height - 2, 0xFF1A1A1A);
        if (hovered) {
            graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0x12FFFFFF);
        }
    }

    private void drawVanillaIconSlot(GuiGraphics graphics, int x, int y, ItemStack iconStack, boolean highlighted) {
        graphics.fill(x, y, x + 18, y + 18, 0xFF8B8B8B);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF202020);
        graphics.fill(x + 2, y + 2, x + 16, y + 16, highlighted ? 0xFF3A3A3A : 0xFF2A2A2A);
        graphics.renderItem(iconStack, x + 1, y + 1);
    }

    private JsonObject currentSkillBranch() {
        if (selectedJob == null || !selectedJob.has("skillBranches")) {
            return null;
        }
        JsonArray branches = selectedJob.getAsJsonArray("skillBranches");
        if (branches.isEmpty()) {
            return null;
        }
        selectedSkillBranch = clampOffset(selectedSkillBranch, branches.size() - 1);
        return branches.get(selectedSkillBranch).getAsJsonObject();
    }

    private boolean skillTreeCanPan() {
        return skillMaxRows() > skillVisibleRows();
    }

    private int clampSkillPanX(int value) {
        JsonObject branch = currentSkillBranch();
        int totalWidth = branch != null ? skillTreeTotalWidth(branch) : skillViewportWidth();
        return clampSkillPanX(value, totalWidth, skillViewportWidth());
    }

    private int clampSkillPanX(int value, int totalWidth, int viewportWidth) {
        int min = Math.min(0, viewportWidth - totalWidth);
        return Math.max(min, Math.min(0, value));
    }

    private int clampSkillPanY(int value) {
        JsonObject branch = currentSkillBranch();
        int totalHeight = branch != null ? skillTreeTotalHeight(branch) : skillViewportHeight();
        return clampSkillPanY(value, totalHeight, skillViewportHeight());
    }

    private int clampSkillPanY(int value, int totalHeight, int viewportHeight) {
        int min = Math.min(0, viewportHeight - totalHeight);
        return Math.max(min, Math.min(0, value));
    }

    private int branchCountForSkills() {
        if (selectedJob == null || !selectedJob.has("skillBranches")) {
            return 0;
        }
        return selectedJob.getAsJsonArray("skillBranches").size();
    }

    private List<SkillNodeLayout> buildSkillNodeLayouts(JsonObject branch) {
        List<SkillNodeLayout> layouts = new ArrayList<>();
        JsonArray nodes = branch.getAsJsonArray("nodes");
        if (nodes.isEmpty()) {
            return layouts;
        }
        int baseX = skillViewportX() + 8;
        int baseY = skillViewportY() + 8;
        int nodeWidth = 200;
        int firstIndex = Math.min(skillOffset, Math.max(0, nodes.size() - 1));
        int visibleCount = skillVisibleRows();
        for (int i = 0; i < visibleCount && firstIndex + i < nodes.size(); i++) {
            JsonObject node = nodes.get(firstIndex + i).getAsJsonObject();
            int y = baseY + i * 30;
            layouts.add(new SkillNodeLayout(node, node.get("id").getAsString(), node.has("parentId") ? node.get("parentId").getAsString() : null,
                baseX, y, nodeWidth, 26, 0, firstIndex + i, true));
        }
        return layouts;
    }

    private int skillTreeRowCount(JsonObject branch) {
        JsonArray nodes = branch.getAsJsonArray("nodes");
        if (nodes.isEmpty()) {
            return 0;
        }
        Map<String, JsonObject> nodeById = new HashMap<>();
        Map<String, Integer> depthCache = new HashMap<>();
        for (JsonElement element : nodes) {
            JsonObject node = element.getAsJsonObject();
            nodeById.put(node.get("id").getAsString(), node);
        }
        Map<Integer, Integer> rowByDepth = new HashMap<>();
        for (JsonElement element : nodes) {
            JsonObject node = element.getAsJsonObject();
            int depth = skillNodeDepth(node, nodeById, depthCache);
            rowByDepth.put(depth, rowByDepth.getOrDefault(depth, 0) + 1);
        }
        int maxRows = 0;
        for (int rows : rowByDepth.values()) {
            maxRows = Math.max(maxRows, rows);
        }
        return maxRows;
    }

    private int skillTreeTotalWidth(JsonObject branch) {
        JsonArray nodes = branch.getAsJsonArray("nodes");
        if (nodes.isEmpty()) {
            return skillViewportWidth();
        }
        Map<String, JsonObject> nodeById = new HashMap<>();
        Map<String, Integer> depthCache = new HashMap<>();
        for (JsonElement element : nodes) {
            JsonObject node = element.getAsJsonObject();
            nodeById.put(node.get("id").getAsString(), node);
        }
        int columnCount = skillTreeColumnCount(branch, nodeById, depthCache);
        int nodeWidth = skillTreeNodeWidth(columnCount, skillViewportWidth());
        int columnGap = skillTreeColumnGap(columnCount, nodeWidth, skillViewportWidth());
        return nodeWidth * columnCount + columnGap * Math.max(0, columnCount - 1);
    }

    private int skillTreeTotalHeight(JsonObject branch) {
        return Math.max(26, skillTreeRowCount(branch) * 38 - 12);
    }

    private int skillTreeColumnCount(JsonObject branch, Map<String, JsonObject> nodeById, Map<String, Integer> depthCache) {
        int maxDepth = 0;
        for (JsonElement element : branch.getAsJsonArray("nodes")) {
            maxDepth = Math.max(maxDepth, skillNodeDepth(element.getAsJsonObject(), nodeById, depthCache));
        }
        return Math.max(1, maxDepth + 1);
    }

    private int skillNodeDepth(JsonObject node, Map<String, JsonObject> nodeById, Map<String, Integer> depthCache) {
        String id = node.get("id").getAsString();
        Integer cached = depthCache.get(id);
        if (cached != null) {
            return cached;
        }
        int depth = 0;
        if (node.has("parentId")) {
            JsonObject parent = nodeById.get(node.get("parentId").getAsString());
            if (parent != null) {
                depth = skillNodeDepth(parent, nodeById, depthCache) + 1;
            }
        }
        depthCache.put(id, depth);
        return depth;
    }

    private int skillTreeNodeWidth(int columnCount, int viewportWidth) {
        return 200;
    }

    private int skillTreeColumnGap(int columnCount, int nodeWidth, int viewportWidth) {
        if (columnCount <= 1) {
            return 0;
        }
        return 28;
    }

    private void drawSkillTreeConnector(GuiGraphics graphics, SkillNodeLayout parent, SkillNodeLayout child) {
        int startX = parent.x() + parent.width() - 2;
        int startY = parent.y() + parent.height() / 2;
        int endX = child.x() + 2;
        int endY = child.y() + child.height() / 2;
        int midX = startX + Math.max(8, (endX - startX) / 2);
        int color = 0xFF7C7C7C;
        graphics.fill(startX, startY, midX, startY + 1, color);
        graphics.fill(midX, Math.min(startY, endY), midX + 1, Math.max(startY, endY) + 1, color);
        graphics.fill(midX, endY, endX, endY + 1, color);
    }

    private record SkillNodeLayout(JsonObject node, String id, String parentId, int x, int y, int width, int height,
                                    int depth, int row, boolean visible) {
    }

    private enum SkillNodeState {
        UNLOCKED,
        AVAILABLE,
        LOCKED
    }

    private int skillNodeAccentColor(JsonObject node) {
        boolean unlocked = node.has("unlocked") && node.get("unlocked").getAsBoolean();
        int requiredLevel = node.has("requiredLevel") ? node.get("requiredLevel").getAsInt() : 0;
        int cost = node.has("cost") ? node.get("cost").getAsInt() : 0;
        if (!unlocked) {
            return 0xFF6F7C8B;
        }
        if (requiredLevel >= 30 || cost >= 4) {
            return 0xFFFFC857;
        }
        if (requiredLevel >= 20 || cost >= 3) {
            return 0xFFA86CFF;
        }
        return 0xFF7FE38A;
    }

    private String skillNodeBadge(JsonObject node) {
        String branchId = node.has("branchId") ? node.get("branchId").getAsString() : "";
        return skillBranchBadge(branchId);
    }

    private ItemStack skillBranchIconStack(String branchId) {
        return switch (branchId) {
            case "income" -> new ItemStack(Items.EMERALD);
            case "resource" -> new ItemStack(Items.IRON_PICKAXE);
            case "utility" -> new ItemStack(Items.COMPASS);
            default -> new ItemStack(Items.PAPER);
        };
    }

    private ItemStack skillNodeIconStack(JsonObject node) {
        String effectType = node.has("effectType") ? node.get("effectType").getAsString() : "";
        return switch (effectType) {
            case "salary_bonus", "salary_multiplier", "money_bonus" -> new ItemStack(Items.EMERALD);
            case "xp_bonus", "xp_multiplier" -> new ItemStack(Items.EXPERIENCE_BOTTLE);
            case "mining_speed", "haste", "break_speed" -> new ItemStack(Items.IRON_PICKAXE);
            case "attack_damage", "damage_bonus", "melee_bonus" -> new ItemStack(Items.IRON_SWORD);
            case "defense", "resistance", "armor_bonus" -> new ItemStack(Items.SHIELD);
            case "movement_speed", "speed" -> new ItemStack(Items.FEATHER);
            case "luck", "fortune" -> new ItemStack(Items.RABBIT_FOOT);
            case "fire_resistance" -> new ItemStack(Items.MAGMA_CREAM);
            case "night_vision" -> new ItemStack(Items.GOLDEN_CARROT);
            case "health_bonus", "regeneration" -> new ItemStack(Items.GOLDEN_APPLE);
            case "hunger_saturation", "saturation" -> new ItemStack(Items.BREAD);
            default -> skillBranchIconStack(node.has("branchId") ? node.get("branchId").getAsString() : "");
        };
    }

    private String skillBranchBadge(String branchId) {
        return switch (branchId) {
            case "income" -> "$";
            case "resource" -> "*";
            case "utility" -> "+";
            default -> "N";
        };
    }

    private Component skillNodeStatus(JsonObject node) {
        return switch (skillNodeState(node)) {
            case UNLOCKED -> Component.translatable("gui.advancedjobs.skill_node_unlocked_status",
                effectLabel(node.get("effectType").getAsString()),
                node.get("effectValue").getAsDouble());
            case AVAILABLE -> Component.translatable("gui.advancedjobs.skill_node_available_status",
                node.get("cost").getAsInt());
            case LOCKED -> Component.translatable("gui.advancedjobs.skill_node_locked_status",
                node.get("requiredLevel").getAsInt(),
                node.get("cost").getAsInt());
        };
    }

    private Component skillNodeTooltipState(JsonObject node) {
        return switch (skillNodeState(node)) {
            case UNLOCKED -> Component.translatable("gui.advancedjobs.tooltip.skill_state_unlocked");
            case AVAILABLE -> Component.translatable("gui.advancedjobs.tooltip.skill_state_available");
            case LOCKED -> skillNodeLockedReason(node);
        };
    }

    private Component skillNodeLockedReason(JsonObject node) {
        if (node.has("parentId") && !isUnlockedNode(selectedJob, node.get("parentId").getAsString())) {
            return Component.translatable("gui.advancedjobs.tooltip.skill_state_parent",
                skillNodeNameById(node.get("parentId").getAsString()));
        }
        int currentLevel = selectedJob != null ? selectedJob.get("level").getAsInt() : 0;
        int currentPoints = selectedJob != null ? selectedJob.get("skillPoints").getAsInt() : 0;
        int requiredLevel = node.get("requiredLevel").getAsInt();
        int cost = node.get("cost").getAsInt();
        if (currentLevel < requiredLevel && currentPoints < cost) {
            return Component.translatable("gui.advancedjobs.tooltip.skill_state_level_points", requiredLevel, cost);
        }
        if (currentLevel < requiredLevel) {
            return Component.translatable("gui.advancedjobs.tooltip.skill_state_level", requiredLevel);
        }
        if (currentPoints < cost) {
            return Component.translatable("gui.advancedjobs.tooltip.skill_state_points", cost);
        }
        return Component.translatable("gui.advancedjobs.tooltip.skill_state_locked");
    }

    private SkillNodeState skillNodeState(JsonObject node) {
        if (node.has("unlocked") && node.get("unlocked").getAsBoolean()) {
            return SkillNodeState.UNLOCKED;
        }
        if (selectedJob == null) {
            return SkillNodeState.LOCKED;
        }
        if (node.has("parentId") && !isUnlockedNode(selectedJob, node.get("parentId").getAsString())) {
            return SkillNodeState.LOCKED;
        }
        int currentLevel = selectedJob.get("level").getAsInt();
        int currentPoints = selectedJob.get("skillPoints").getAsInt();
        int requiredLevel = node.get("requiredLevel").getAsInt();
        int cost = node.get("cost").getAsInt();
        return currentLevel >= requiredLevel && currentPoints >= cost ? SkillNodeState.AVAILABLE : SkillNodeState.LOCKED;
    }

    private int skillNodeStateColor(SkillNodeState state) {
        return switch (state) {
            case UNLOCKED -> 0xFF3FAF5A;
            case AVAILABLE -> 0xFFD6B33D;
            case LOCKED -> 0xFF5A5A5A;
        };
    }

    private int skillNodeStateTextColor(SkillNodeState state) {
        return switch (state) {
            case UNLOCKED -> 0xFFB8F3C0;
            case AVAILABLE -> 0xFFFFE08A;
            case LOCKED -> 0xFFAFAFAF;
        };
    }

    private void drawSkillStatusIcon(GuiGraphics graphics, int x, int y, SkillNodeState state, boolean hovered) {
        ResourceLocation texture = switch (state) {
            case UNLOCKED -> SKILL_STATUS_CHECK;
            case AVAILABLE -> SKILL_STATUS_PLUS;
            case LOCKED -> SKILL_STATUS_CROSS;
        };
        int size = 16;
        if (hovered) {
            graphics.fill(x - 1, y - 1, x + size + 1, y + size + 1, 0x18000000);
        }
        graphics.blit(texture, x, y, 0, 0, size, size, size, size);
    }

    private String skillNodeNameById(String nodeId) {
        if (selectedJob == null || !selectedJob.has("skillBranches")) {
            return nodeId;
        }
        for (JsonElement branchElement : selectedJob.getAsJsonArray("skillBranches")) {
            JsonObject branch = branchElement.getAsJsonObject();
            for (JsonElement nodeElement : branch.getAsJsonArray("nodes")) {
                JsonObject node = nodeElement.getAsJsonObject();
                if (nodeId.equals(node.get("id").getAsString())) {
                    return translatedOrFallback(node.get("translationKey").getAsString(), fallbackNodeName(node)).getString();
                }
            }
        }
        return nodeId;
    }

    private void drawScrollbar(GuiGraphics graphics, ScrollTarget target, int x, int y, int height, int total, int visible, int offset) {
        if (total <= visible) {
            return;
        }
        activeScrollbar = target;
        activeScrollbarX = x;
        activeScrollbarY = y;
        activeScrollbarHeight = height;
        graphics.fill(x, y, x + 6, y + height, 0xFF2F2F2F);
        graphics.fill(x, y, x + 6, y + 1, 0xFF8B8B8B);
        graphics.fill(x, y, x + 1, y + height, 0xFF8B8B8B);
        graphics.fill(x + 5, y, x + 6, y + height, 0xFF1A1A1A);
        graphics.fill(x, y + height - 1, x + 6, y + height, 0xFF1A1A1A);
        int thumbHeight = Math.max(12, (int) ((height * (double) visible) / total));
        int maxOffset = Math.max(1, total - visible);
        int travel = Math.max(1, height - thumbHeight);
        int thumbY = y + (int) ((travel * (double) offset) / maxOffset);
        graphics.fill(x + 1, thumbY, x + 5, thumbY + thumbHeight, 0xFF8B8B8B);
        graphics.fill(x + 1, thumbY, x + 5, thumbY + 1, 0xFFF0F0F0);
        graphics.fill(x + 1, thumbY, x + 2, thumbY + thumbHeight, 0xFFF0F0F0);
        graphics.fill(x + 4, thumbY, x + 5, thumbY + thumbHeight, 0xFF3A3A3A);
        graphics.fill(x + 1, thumbY + thumbHeight - 1, x + 5, thumbY + thumbHeight, 0xFF3A3A3A);
    }

    private void scrollToMouse(ScrollTarget target, double mouseY) {
        int total = totalEntriesFor(target);
        int visible = visibleEntriesFor(target);
        if (total <= visible || activeScrollbarHeight <= 0) {
            return;
        }
        int thumbHeight = Math.max(12, (int) ((activeScrollbarHeight * (double) visible) / total));
        int trackTravel = Math.max(1, activeScrollbarHeight - thumbHeight);
        double relative = mouseY - activeScrollbarY - thumbHeight / 2.0D;
        double normalized = Math.max(0.0D, Math.min(1.0D, relative / trackTravel));
        int maxOffset = Math.max(0, total - visible);
        setOffsetFor(target, (int) Math.round(normalized * maxOffset));
    }

    private int totalEntriesFor(ScrollTarget target) {
        return switch (target) {
            case PROFESSIONS -> ClientJobState.jobs().size();
            case SALARY -> salaryLines().size();
            case DAILY -> visibleDailyTasks().size();
            case CONTRACTS -> visibleContracts().size();
            case SKILLS -> skillMaxRows();
            case MY_JOB_PASSIVES -> passivesOrEmpty(selectedJob).size();
            case MY_JOB_PERKS -> unlockedSkillEntries().size();
            case MY_JOB_EFFECTS -> activeEffects().size();
            case TOP -> leaderboardEntries().size();
            default -> 0;
        };
    }

    private int visibleEntriesFor(ScrollTarget target) {
        return switch (target) {
            case TOP -> topVisibleRows();
            case SALARY -> salaryVisibleRows();
            case DAILY -> dailyVisibleRows();
            case CONTRACTS -> contractsVisibleRows();
            case SKILLS -> skillVisibleRows();
            case MY_JOB_PASSIVES -> myJobPassiveVisibleCount();
            case MY_JOB_PERKS -> myJobPerkVisibleCount();
            case MY_JOB_EFFECTS -> myJobEffectVisibleCount();
            default -> LIST_SIZE;
        };
    }

    private void setOffsetFor(ScrollTarget target, int value) {
        int max = Math.max(0, totalEntriesFor(target) - visibleEntriesFor(target));
        int clamped = clampOffset(value, max);
        switch (target) {
            case PROFESSIONS, DAILY, CONTRACTS -> listOffset = clamped;
            case SALARY -> salaryOffset = clamped;
            case SKILLS -> skillOffset = clamped;
            case MY_JOB_PASSIVES -> passiveOffset = clamped;
            case MY_JOB_PERKS -> perkOffset = clamped;
            case MY_JOB_EFFECTS -> effectOffset = clamped;
            case TOP -> topOffset = clamped;
            default -> {
            }
        }
    }

    private void renderSalary(GuiGraphics graphics) {
        if (selectedJob == null) {
            drawClampedText(graphics, Component.translatable("gui.advancedjobs.no_active_profession"), 20, 84, contentWidth(), 0xD0D0D0);
            return;
        }
        int panelX = salaryWindowX();
        int panelY = salaryWindowY();
        int panelWidth = salaryWindowWidth();
        int panelHeight = salaryWindowHeight();
        int viewportX = salaryViewportX();
        int viewportY = salaryViewportY();
        int viewportWidth = salaryViewportWidth();
        int viewportHeight = salaryViewportHeight();

        drawVanillaAdvancementWindow(graphics, panelX, panelY, panelWidth, panelHeight);
        drawSkillViewportBackground(graphics, viewportX, viewportY, viewportWidth, viewportHeight);
        drawCompactWindowHeader(graphics, panelX, panelY, panelWidth,
            Component.translatable("gui.advancedjobs.salary"),
            Component.translatable("gui.advancedjobs.salary_pending_short_header", TextUtil.fmt2(totalPendingSalary())));

        int lineWidth = viewportWidth - 24;
        List<Component> lines = salaryLines();
        int visibleRows = salaryVisibleRows();
        int visible = Math.min(visibleRows, Math.max(0, lines.size() - salaryOffset));
        graphics.enableScissor(viewportX, viewportY, viewportX + viewportWidth, viewportY + viewportHeight);
        for (int i = 0; i < visible; i++) {
            int index = salaryOffset + i;
            drawClampedText(graphics, lines.get(index), viewportX + 8, viewportY + 6 + i * 12, lineWidth, salaryLineColor(index));
        }
        graphics.disableScissor();
        drawScrollbar(graphics, ScrollTarget.SALARY, viewportX + viewportWidth - 6, viewportY + 1, viewportHeight - 2,
            lines.size(), visibleRows, salaryOffset);
    }

    private void renderDaily(GuiGraphics graphics, int mouseX, int mouseY, List<Component> tooltip) {
        if (selectedJob == null) {
            drawClampedText(graphics, Component.translatable("gui.advancedjobs.daily"), 20, 54, contentWidth(), 0xFFFFFF);
            drawClampedText(graphics, Component.translatable("gui.advancedjobs.no_active_profession"), 20, 84, contentWidth(), 0xD0D0D0);
            return;
        }
        List<JsonObject> daily = visibleDailyTasks();
        int visibleRows = dailyVisibleRows();
        int visible = Math.min(visibleRows, Math.max(0, daily.size() - listOffset));
        int panelWidth = dailyWindowWidth();
        int panelX = (this.width - panelWidth) / 2;
        int panelHeight = dailyWindowHeight();
        int panelY = dailyWindowY();
        int viewportX = dailyViewportX();
        int viewportY = dailyViewportY();
        int viewportWidth = dailyViewportWidth();
        int viewportHeight = dailyViewportHeight();
        int cardWidth = viewportWidth - 14;
        int rowHeight = 34;
        int listY = viewportY + 4;
        drawVanillaAdvancementWindow(graphics, panelX, panelY, panelWidth, panelHeight);
        drawCompactWindowHeader(graphics, panelX, panelY, panelWidth,
            Component.translatable("gui.advancedjobs.daily"),
            Component.translatable("gui.advancedjobs.daily_tasks_short", daily.size()));
        drawSkillViewportBackground(graphics, viewportX, viewportY, viewportWidth, viewportHeight);
        if (daily.isEmpty()) {
            drawClampedText(graphics, Component.translatable("gui.advancedjobs.no_daily_tasks"),
                viewportX + 4, listY + 10, viewportWidth - 8, 0xD0D0D0);
            return;
        }
        graphics.enableScissor(viewportX, viewportY, viewportX + viewportWidth, viewportY + viewportHeight);
        for (int i = 0; i < visible; i++) {
            JsonObject task = daily.get(listOffset + i);
            int y = listY + i * rowHeight;
            boolean completed = task.get("completed").getAsBoolean();
            int rowX = viewportX + 4;
            boolean hovered = isHovering(rowX, y, cardWidth, 28, mouseX, mouseY);
            int accent = completed ? 0xFF7FE38A : 0xFF6BAF66;
            double salaryReward = task.has("salaryReward") ? task.get("salaryReward").getAsDouble() : 0.0D;
            double xpReward = task.has("xpReward") ? task.get("xpReward").getAsDouble() : 0.0D;
            renderVanillaTaskRow(graphics, rowX, y, cardWidth, 28,
                new ItemStack(completed ? Items.LIME_DYE : Items.WRITABLE_BOOK),
                dailyLabel(task),
                Component.translatable(completed ? "gui.advancedjobs.daily_card_status_done_short" : "gui.advancedjobs.daily_card_status_short",
                    task.get("progress").getAsInt(), task.get("target").getAsInt(),
                    TextUtil.fmt2(salaryReward), TextUtil.fmt2(xpReward)),
                accent,
                hovered);
            drawProgressBar(graphics, rowX + 28, y + 20, Math.max(120, Math.min(150, cardWidth / 3)), 5,
                task.get("progress").getAsDouble(), task.get("target").getAsDouble(), 0x334455, 0x4CAF50);
            if (hovered) {
                tooltip.add(dailyLabel(task));
                tooltip.add(Component.translatable("gui.advancedjobs.tooltip.progress", task.get("progress").getAsInt(), task.get("target").getAsInt()));
                if (task.has("salaryReward") && task.has("xpReward")) {
                    tooltip.add(Component.translatable("gui.advancedjobs.reward_line",
                        TextUtil.fmt2(task.get("salaryReward").getAsDouble()), TextUtil.fmt2(task.get("xpReward").getAsDouble())));
                }
                appendBonusTooltip(task, tooltip);
            }
        }
        graphics.disableScissor();
        drawScrollbar(graphics, ScrollTarget.DAILY, viewportX + viewportWidth - 6, viewportY + 1, viewportHeight - 2, daily.size(), visibleRows, listOffset);
    }

    private int dailyVisibleRows() {
        return 3;
    }

    private int dailyWindowWidth() {
        return 252;
    }

    private int dailyWindowHeight() {
        return 140;
    }

    private int dailyWindowY() {
        return Math.max(54, (this.height - dailyWindowHeight()) / 2);
    }

    private int dailyViewportX() {
        return dailyWindowX() + 9;
    }

    private int dailyViewportY() {
        return dailyWindowY() + 18;
    }

    private int dailyViewportWidth() {
        return dailyWindowWidth() - 18;
    }

    private int dailyViewportHeight() {
        return 113;
    }

    private int dailyWindowX() {
        return (this.width - dailyWindowWidth()) / 2;
    }

    private void renderContracts(GuiGraphics graphics, int mouseX, int mouseY, List<Component> tooltip) {
        if (selectedJob == null) {
            drawClampedText(graphics, Component.translatable("gui.advancedjobs.no_active_profession"), 20, 84, contentWidth(), 0xD0D0D0);
            return;
        }
        int panelX = contractsWindowX();
        int panelY = contractsWindowY();
        int panelWidth = contractsWindowWidth();
        int panelHeight = contractsWindowHeight();
        int viewportX = contractsViewportX();
        int viewportY = contractsViewportY();
        int viewportWidth = contractsViewportWidth();
        int viewportHeight = contractsViewportHeight();

        drawVanillaAdvancementWindow(graphics, panelX, panelY, panelWidth, panelHeight);
        drawSkillViewportBackground(graphics, viewportX, viewportY, viewportWidth, viewportHeight);
        List<JsonObject> contracts = visibleContracts();
        drawCompactWindowHeader(graphics, panelX, panelY, panelWidth,
            Component.translatable("gui.advancedjobs.contracts"),
            Component.translatable("gui.advancedjobs.contracts_tasks_short", contracts.size()));
        if (contracts.isEmpty()) {
                drawClampedText(graphics, Component.translatable("gui.advancedjobs.no_contract_tasks"), viewportX + 8, viewportY + 8, viewportWidth - 22, 0xD0D0D0);
                return;
            }
        int visibleRows = contractsVisibleRows();
        int visible = Math.min(visibleRows, Math.max(0, contracts.size() - listOffset));
        int cardWidth = viewportWidth - 14;
        int rowHeight = 34;
        graphics.enableScissor(viewportX, viewportY, viewportX + viewportWidth, viewportY + viewportHeight);
        for (int i = 0; i < visible; i++) {
            JsonObject contract = contracts.get(listOffset + i);
            int y = viewportY + 4 + i * rowHeight;
            String rarity = contract.get("rarity").getAsString();
            int rarityColor = rarityColor(rarity);
            boolean completed = contract.get("completed").getAsBoolean();
            boolean hovered = isHovering(viewportX + 4, y, cardWidth, 28, mouseX, mouseY);
            renderVanillaTaskRow(graphics, viewportX + 4, y, cardWidth, 28,
                contractIconStack(rarity, completed),
                contractLabel(contract),
                Component.translatable(completed ? "gui.advancedjobs.contract_card_status_done" : "gui.advancedjobs.contract_card_status_progress",
                    rarityLabel(rarity), contract.get("progress").getAsInt(), contract.get("target").getAsInt()),
                rarityColor,
                hovered);
            drawProgressBar(graphics, viewportX + 32, y + 20, Math.max(96, Math.min(130, (cardWidth - 40) / 3)), 5,
                contract.get("progress").getAsDouble(), contract.get("target").getAsDouble(), 0x334455, rarityColor);
            if (hovered) {
                tooltip.add(contractLabel(contract));
                tooltip.add(Component.translatable("gui.advancedjobs.tooltip.rarity", rarityLabel(rarity)));
                tooltip.add(Component.translatable("gui.advancedjobs.tooltip.progress", contract.get("progress").getAsInt(), contract.get("target").getAsInt()));
                if (contract.has("expiresAt")) {
                    tooltip.add(Component.translatable("gui.advancedjobs.tooltip.time_left", contractTimeLeft(contract)));
                }
                if (contract.has("salaryReward") && contract.has("xpReward")) {
                    tooltip.add(Component.translatable("gui.advancedjobs.reward_line",
                        TextUtil.fmt2(contract.get("salaryReward").getAsDouble()), TextUtil.fmt2(contract.get("xpReward").getAsDouble())));
                }
                  appendBonusTooltip(contract, tooltip);
              }
          }
        graphics.disableScissor();
        drawScrollbar(graphics, ScrollTarget.CONTRACTS, viewportX + viewportWidth - 6, viewportY + 1, viewportHeight - 2,
              contracts.size(), visibleRows, listOffset);
      }

    private int contractsVisibleRows() {
        return 3;
    }

    private int contractsWindowWidth() {
        return 252;
    }

    private int contractsWindowHeight() {
        return 140;
    }

    private int contractsWindowX() {
        return (this.width - contractsWindowWidth()) / 2;
    }

    private int contractsWindowY() {
        return Math.max(54, (this.height - contractsWindowHeight()) / 2);
    }

    private int contractsViewportX() {
        return contractsWindowX() + 9;
    }

    private int contractsViewportY() {
        return contractsWindowY() + 18;
    }

    private int contractsViewportWidth() {
        return contractsWindowWidth() - 18;
    }

    private int contractsViewportHeight() {
        return 113;
    }

    private void drawVanillaListPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        int right = x + width;
        int bottom = y + height;
        int headerBottom = Math.min(bottom - 5, y + 28);
        graphics.fill(x, y, right, bottom, 0xFF000000);
        graphics.fill(x + 1, y + 1, right - 1, bottom - 1, 0xFFC6C6C6);
        graphics.fill(x + 2, y + 2, right - 2, bottom - 2, 0xFF8B8B8B);
        graphics.fill(x + 3, y + 3, right - 3, bottom - 3, 0xFF2F2F2F);
        graphics.fill(x + 4, y + 4, right - 4, headerBottom, 0xFFDADADA);
        graphics.fill(x + 4, headerBottom, right - 4, headerBottom + 1, 0xFF555555);
        graphics.fill(x + 4, headerBottom + 1, right - 4, bottom - 4, 0xFF2A2A2A);
    }

    private void renderVanillaTaskRow(GuiGraphics graphics, int x, int y, int width, int height, ItemStack iconStack,
                                      Component title, Component status, int accentColor, boolean hovered) {
        graphics.fill(x, y, x + width, y + height, 0xFF8B8B8B);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF373737);
        graphics.fill(x + 2, y + 2, x + width - 2, y + 3, accentColor);
        graphics.fill(x + 2, y + height - 3, x + width - 2, y + height - 2, 0xFF1A1A1A);
        if (hovered) {
            graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0x22FFFFFF);
        }
        graphics.fill(x + 5, y + 5, x + 23, y + 23, 0xFF8B8B8B);
        graphics.fill(x + 6, y + 6, x + 22, y + 22, 0xFF1F1F1F);
        graphics.renderFakeItem(iconStack, x + 7, y + 7);
        drawClampedText(graphics, title, x + 28, y + 5, Math.max(80, width - 34), 0xFFFFFF);
        drawClampedText(graphics, status, x + 28, y + 15, Math.max(80, width - 34), 0xC8C8C8);
    }

    private ItemStack contractIconStack(String rarity, boolean completed) {
        if (completed) {
            return new ItemStack(Items.LIME_DYE);
        }
        return switch (rarity) {
            case "elite" -> new ItemStack(Items.ENCHANTED_GOLDEN_APPLE);
            case "rare" -> new ItemStack(Items.GOLD_INGOT);
            default -> new ItemStack(Items.PAPER);
        };
    }

    private void renderTop(GuiGraphics graphics, int mouseX, int mouseY, List<Component> tooltip) {
        List<JsonObject> jobs = ClientJobState.jobs();
        if (jobs.isEmpty()) {
            drawClampedText(graphics, Component.translatable("gui.advancedjobs.no_professions_loaded"), 20, 86, contentWidth(), 0xD0D0D0);
            return;
        }
        leaderboardJobIndex = clampOffset(leaderboardJobIndex, jobs.size());
        boolean overall = leaderboardJobIndex == jobs.size();
        int panelX = topWindowX();
        int panelY = topWindowY();
        int panelWidth = topWindowWidth();
        int panelHeight = topWindowHeight();
        int viewportX = topViewportX();
        int viewportY = topViewportY();
        int viewportWidth = topViewportWidth();
        int viewportHeight = topViewportHeight();

        drawVanillaAdvancementWindow(graphics, panelX, panelY, panelWidth, panelHeight);
        drawSkillViewportBackground(graphics, viewportX, viewportY, viewportWidth, viewportHeight);

        List<JsonObject> entries = leaderboardEntries();
        int page = (topOffset / TOP_PAGE_SIZE) + 1;
        int pages = Math.max(1, (int) Math.ceil(entries.size() / (double) TOP_PAGE_SIZE));
        Component professionTitle = overall
            ? Component.translatable("gui.advancedjobs.top_overall")
            : Component.translatable(jobs.get(leaderboardJobIndex).get("nameKey").getAsString());
        Component headerStatus = Component.translatable("gui.advancedjobs.page", page, pages);
        drawCompactWindowHeader(graphics, panelX, panelY, panelWidth,
            Component.translatable("gui.advancedjobs.top"),
            headerStatus);

        int lineWidth = viewportWidth - 16;
        graphics.enableScissor(viewportX, viewportY, viewportX + viewportWidth, viewportY + viewportHeight);
        drawClampedText(graphics,
            overall
                ? Component.translatable("gui.advancedjobs.top_header_overall", topSortLabel())
                : Component.translatable("gui.advancedjobs.top_header_job", professionTitle, topSortLabel()),
            viewportX + 8, viewportY + 6, lineWidth, 0xD0D0D0);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.top_summary",
            entries.size(),
            topLeaderName(entries),
            topLeaderLevel(entries)), viewportX + 8, viewportY + 18, lineWidth, 0xD0D0D0);
        int selfRank = currentPlayerRank(entries);
        JsonObject selfEntry = selfRank > 0 ? entries.get(selfRank - 1) : null;
        if (selfEntry != null) {
            Component summary = overall
                ? Component.translatable("gui.advancedjobs.top_self_overall",
                    selfRank,
                    selfEntry.get("level").getAsInt(),
                    TextUtil.fmt2(selfEntry.get("xp").getAsDouble()),
                    TextUtil.fmt2(selfEntry.get("earned").getAsDouble()))
                : Component.translatable("gui.advancedjobs.top_self_job",
                    selfRank,
                    selfEntry.get("level").getAsInt(),
                    rankLabel(selfEntry.get("level").getAsInt()),
                    TextUtil.fmt2(selfEntry.get("xp").getAsDouble()),
                    TextUtil.fmt2(selfEntry.get("earned").getAsDouble()));
            drawClampedText(graphics, summary, viewportX + 8, viewportY + 30, lineWidth, 0x9BE39B);
            if (isHovering(viewportX + 8, viewportY + 30, lineWidth, 12, mouseX, mouseY)) {
                tooltip.add(Component.translatable("gui.advancedjobs.tooltip.top_self"));
                tooltip.add(summary);
            }
            String gap = leaderboardGap(entries, selfRank, overall);
            if (gap != null) {
                drawClampedText(graphics, Component.translatable("gui.advancedjobs.top_gap", gap), viewportX + 8, viewportY + 42, lineWidth, 0xB8B8B8);
            }
        } else {
            drawClampedText(graphics, Component.translatable("gui.advancedjobs.top_self_missing"), viewportX + 8, viewportY + 30, lineWidth, 0xB8B8B8);
        }
        if (entries.isEmpty()) {
            graphics.disableScissor();
            drawClampedText(graphics, Component.translatable("gui.advancedjobs.no_players_for_job"), viewportX + 8, viewportY + 62, lineWidth, 0xD0D0D0);
            return;
        }
        int listStartY = viewportY + 62;
        int visible = Math.min(topVisibleRows(), Math.max(0, entries.size() - topOffset));
        int entryNameWidth = 94;
        int entryLineX = viewportX + 118;
        int entryLineWidth = viewportWidth - 126;
        for (int i = 0; i < visible; i++) {
            JsonObject row = entries.get(topOffset + i);
            int y = listStartY + i * 12;
            int rank = topOffset + i + 1;
            int rankColor = leaderboardRankColor(rank);
            boolean self = this.minecraft != null && this.minecraft.player != null
                && row.get("player").getAsString().equalsIgnoreCase(this.minecraft.player.getGameProfile().getName());
            int nameColor = self ? 0x7FE38A : 0xFFFFFF;
            graphics.drawString(this.font, rankPrefix(rank), viewportX + 8, y, rankColor);
            drawClampedText(graphics, Component.literal(row.get("player").getAsString()), viewportX + 32, y, entryNameWidth, nameColor);
            Component entryLine = overall
                ? Component.translatable("gui.advancedjobs.top_entry_overall",
                    row.get("level").getAsInt(),
                    TextUtil.fmt2(row.get("xp").getAsDouble()),
                    TextUtil.fmt2(row.get("earned").getAsDouble()))
                : Component.translatable("gui.advancedjobs.top_entry",
                    row.get("level").getAsInt(),
                    rankLabel(row.get("level").getAsInt()),
                    TextUtil.fmt2(row.get("xp").getAsDouble()),
                    TextUtil.fmt2(row.get("earned").getAsDouble()));
            drawClampedText(graphics, entryLine, entryLineX, y, entryLineWidth, self ? 0xD8F5D0 : 0xB8B8B8);
            if (isHovering(viewportX + 8, y, viewportWidth - 16, 12, mouseX, mouseY)) {
                tooltip.add(Component.literal(row.get("player").getAsString()));
                tooltip.add(overall
                    ? Component.translatable("gui.advancedjobs.tooltip.top_overall",
                        row.get("level").getAsInt(),
                        TextUtil.fmt2(row.get("xp").getAsDouble()),
                        TextUtil.fmt2(row.get("earned").getAsDouble()))
                    : Component.translatable("gui.advancedjobs.tooltip.top_job",
                        row.get("level").getAsInt(),
                        rankLabel(row.get("level").getAsInt()),
                        TextUtil.fmt2(row.get("xp").getAsDouble()),
                        TextUtil.fmt2(row.get("earned").getAsDouble())));
            }
        }
        graphics.disableScissor();
        drawScrollbar(graphics, ScrollTarget.TOP, viewportX + viewportWidth - 6, listStartY, topVisibleRows() * 12, entries.size(), topVisibleRows(), topOffset);
    }

    private void renderHelp(GuiGraphics graphics) {
        int lineWidth = contentWidth(220);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.help"), 20, 54, lineWidth, 0xFFFFFF);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.help.1"), 20, 84, lineWidth, 0xD0D0D0);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.help.2"), 20, 98, lineWidth, 0xD0D0D0);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.help.3"), 20, 112, lineWidth, 0xD0D0D0);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.help.4"), 20, 126, lineWidth, 0xD0D0D0);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.help.5"), 20, 140, lineWidth, 0xD0D0D0);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.help.change",
            TextUtil.fmt2(ClientJobState.jobChangePrice()), TimeUtil.formatRemainingSeconds(ClientJobState.jobChangeCooldownRemaining())), 20, 164, lineWidth, 0xB8B8B8);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.help.secondary",
            Component.translatable(ClientJobState.allowSecondaryJob() ? "gui.advancedjobs.common.enabled" : "gui.advancedjobs.common.disabled")), 20, 178, lineWidth, 0xB8B8B8);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.help.salary_mode",
            Component.translatable(ClientJobState.instantSalary() ? "gui.advancedjobs.salary_mode.instant" : "gui.advancedjobs.salary_mode.manual")), 20, 192, lineWidth, 0xB8B8B8);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.help.salary_claim_cooldown",
            TimeUtil.formatRemainingSeconds(ClientJobState.salaryClaimCooldownRemaining())), 20, 206, lineWidth, 0xB8B8B8);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.help.salary_tax_rate",
            TextUtil.fmt2(ClientJobState.salaryTaxRate() * 100.0D)), 20, 220, lineWidth, 0xB8B8B8);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.help.salary_claim_cap",
            TextUtil.fmt2(ClientJobState.maxSalaryPerClaim())), 20, 234, lineWidth, 0xB8B8B8);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.help.salary_claim_preview",
            TextUtil.fmt2(nextSalaryClaimGross()),
            TextUtil.fmt2(nextSalaryClaimNet())), 20, 248, lineWidth, 0x9AD0FF);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.help.salary_board_quick_action"), 20, 262, lineWidth, 0xB8B8B8);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.help.economy_context",
            ClientJobState.economyProvider(), ClientJobState.economyCurrency()), 20, 276, lineWidth, 0xB8B8B8);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.help.tax_sink_account",
            ClientJobState.taxSinkAccountUuid()), 20, 290, lineWidth, 0xB8B8B8);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.help.titles",
            ClientJobState.unlockedTitles().size(), latestUnlockedTitle()), 20, 304, lineWidth, 0xCFAF6A);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.help.daily_cycle",
            selectedJob != null ? dailyResetTime(selectedJob) : "--",
            selectedJob != null ? nextContractRotationTime(selectedJob) : "--"), 20, 318, lineWidth, 0xB8B8B8);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.help.daily_board_quick_action"), 20, 332, lineWidth, 0xB8B8B8);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.help.status_board_quick_action"), 20, 346, lineWidth, 0xB8B8B8);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.help.contract_board",
            TextUtil.fmt2(ClientJobState.contractRerollPrice()),
            TimeUtil.formatRemainingSeconds(ClientJobState.contractRerollCooldownRemaining())), 20, 360, lineWidth, 0xB8B8B8);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.help.contract_board_quick_action"), 20, 374, lineWidth, 0xB8B8B8);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.help.jobs_master_quick_action"), 20, 388, lineWidth, 0xB8B8B8);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.help.help_board_quick_action"), 20, 402, lineWidth, 0xB8B8B8);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.help.skills_board_quick_action"), 20, 416, lineWidth, 0xB8B8B8);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.help.leaderboard_board_quick_action"), 20, 430, lineWidth, 0xB8B8B8);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.help.wand_quick_action"), 20, 444, lineWidth, 0xB8B8B8);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.help.reward_context"), 20, 458, lineWidth, 0xFFFFFF);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.help.world_multiplier",
            targetLabel(ClientJobState.currentWorldId()), TextUtil.fmt2(ClientJobState.worldRewardMultiplier())), 20, 472, lineWidth, 0xD0D0D0);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.help.biome_multiplier",
            targetLabel(ClientJobState.currentBiomeId()), TextUtil.fmt2(ClientJobState.biomeRewardMultiplier())), 20, 486, lineWidth, 0xD0D0D0);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.help.event_multiplier",
            TextUtil.fmt2(ClientJobState.eventRewardMultiplier())), 20, 500, lineWidth, 0xD0D0D0);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.help.event_time_left",
            TimeUtil.formatRemainingSeconds(ClientJobState.eventRemainingSeconds())), 20, 514, lineWidth, 0xD0D0D0);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.help.vip_multiplier",
            TextUtil.fmt2(ClientJobState.vipRewardMultiplier())), 20, 528, lineWidth, 0xD0D0D0);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.help.total_multiplier",
            TextUtil.fmt2(ClientJobState.effectiveRewardMultiplier())), 20, 542, lineWidth, 0x9BE39B);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.help.mob_filters",
            yesNo(ClientJobState.blockArtificialMobRewards()),
            yesNo(ClientJobState.blockBabyMobRewards()),
            yesNo(ClientJobState.blockTamedMobRewards())), 20, 556, lineWidth, 0xB8B8B8);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.help.loot_chunk_filters",
            TimeUtil.formatRemainingSeconds(ClientJobState.lootContainerRewardCooldownSeconds()),
            TimeUtil.formatRemainingSeconds(ClientJobState.exploredChunkRewardCooldownSeconds())), 20, 570, lineWidth, 0xB8B8B8);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.help.commands"), 20, 584, lineWidth, 0xFFFFFF);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.help.command.1"), 20, 598, lineWidth, 0xD0D0D0);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.help.command.2"), 20, 612, lineWidth, 0xD0D0D0);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.help.command.3"), 20, 626, lineWidth, 0xD0D0D0);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.help.command.4"), 20, 640, lineWidth, 0xD0D0D0);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.help.command.5"), 20, 654, lineWidth, 0xD0D0D0);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.help.command.6"), 20, 668, lineWidth, 0xD0D0D0);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.help.secondary_commands"), 20, 682, lineWidth, 0xB8B8B8);
    }

    private Button claimSalaryButton(String key, int x, int y, int width) {
        Button button = Button.builder(Component.translatable(key), action ->
            PacketHandler.CHANNEL.sendToServer(new ClaimSalaryPacket()))
            .bounds(x, y, width, 20)
            .build();
        button.active = canClaimSalaryNow();
        return button;
    }

    private boolean canClaimSalaryNow() {
        return !ClientJobState.instantSalary()
            && ClientJobState.salaryClaimCooldownRemaining() <= 0L
            && totalPendingSalary() > 0.0D;
    }

    private double nextSalaryClaimGross() {
        return Math.min(totalPendingSalary(), ClientJobState.maxSalaryPerClaim());
    }

    private double nextSalaryClaimGross(String jobId) {
        double remaining = ClientJobState.maxSalaryPerClaim();
        if (ClientJobState.activeJobId() != null) {
            JsonObject active = jobById(ClientJobState.activeJobId());
            if (active != null) {
                double claimed = Math.min(active.get("pendingSalary").getAsDouble(), remaining);
                if (ClientJobState.activeJobId().equals(jobId)) {
                    return claimed;
                }
                remaining -= claimed;
            }
        }
        if (ClientJobState.secondaryJobId() != null) {
            JsonObject secondary = jobById(ClientJobState.secondaryJobId());
            if (secondary != null) {
                double claimed = Math.min(secondary.get("pendingSalary").getAsDouble(), remaining);
                if (ClientJobState.secondaryJobId().equals(jobId)) {
                    return claimed;
                }
            }
        }
        return 0.0D;
    }

    private double nextSalaryClaimTax() {
        return nextSalaryClaimGross() * ClientJobState.salaryTaxRate();
    }

    private double nextSalaryClaimNet() {
        return Math.max(0.0D, nextSalaryClaimGross() - nextSalaryClaimTax());
    }

    private Component yesNo(boolean value) {
        return Component.translatable(value ? "gui.advancedjobs.common.enabled" : "gui.advancedjobs.common.disabled");
    }

    private void drawListStatus(GuiGraphics graphics, int totalEntries, int pageSize, int offset, int x, int y) {
        int page = totalEntries <= 0 ? 1 : (offset / pageSize) + 1;
        int pages = Math.max(1, (int) Math.ceil(totalEntries / (double) pageSize));
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.list_status", totalEntries, page, pages), x, y, remainingWidth(x, 16, 80), 0xB8B8B8);
    }

    private int listStatusX() {
        return rightAlignedX(190, 220);
    }

    private int contentWidth() {
        return Math.max(180, this.width - 40);
    }

    private int contentWidth(int minWidth) {
        return Math.max(minWidth, this.width - 40);
    }

    private int rowHoverWidth(int rightPadding) {
        return Math.max(120, this.width - rightPadding);
    }

    private int remainingWidth(int startX, int rightPadding, int minWidth) {
        return Math.max(minWidth, this.width - startX - rightPadding);
    }

    private int hudRightX() {
        return rightAlignedX(16, 16);
    }

    private int rightIconX() {
        return rightAlignedX(52, 52);
    }

    private int actionButtonPrimaryX() {
        return rightAlignedX(210, 20);
    }

    private int actionButtonSecondaryX() {
        return rightAlignedX(118, 108);
    }

    private int wideActionButtonX() {
        return rightAlignedX(210, 20);
    }

    private int contractsButtonX() {
        return rightAlignedX(170, 20);
    }

    private int contractsActionButtonX() {
        return contractsWindowX() + (contractsWindowWidth() - 140) / 2;
    }

    private int contractsActionButtonY() {
        return contractsWindowY() + contractsWindowHeight() + 6;
    }

    private int claimButtonX() {
        return rightAlignedX(150, 20);
    }

    private int salaryActionButtonX() {
        return salaryWindowX() + (salaryWindowWidth() - 120) / 2;
    }

    private int salaryActionButtonY() {
        return salaryWindowY() + salaryWindowHeight() + 6;
    }

    private int salaryWindowWidth() {
        return 252;
    }

    private int salaryWindowHeight() {
        return 140;
    }

    private int salaryWindowX() {
        return (this.width - salaryWindowWidth()) / 2;
    }

    private int salaryWindowY() {
        return Math.max(54, (this.height - salaryWindowHeight()) / 2);
    }

    private int salaryViewportX() {
        return salaryWindowX() + 9;
    }

    private int salaryViewportY() {
        return salaryWindowY() + 18;
    }

    private int salaryViewportWidth() {
        return salaryWindowWidth() - 18;
    }

    private int salaryViewportHeight() {
        return 113;
    }

    private List<Component> salaryLines() {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("gui.advancedjobs.salary_pending_short", TextUtil.fmt2(totalPendingSalary())));
        lines.add(Component.translatable("gui.advancedjobs.salary_earned_short", TextUtil.fmt2(totalEarnedSalary())));
        lines.add(Component.translatable("gui.advancedjobs.salary_claim_cooldown_short",
            TimeUtil.formatRemainingSeconds(ClientJobState.salaryClaimCooldownRemaining())));
        if (!ClientJobState.instantSalary()) {
            lines.add(Component.translatable("gui.advancedjobs.salary_claim_preview_short",
                TextUtil.fmt2(nextSalaryClaimGross()),
                TextUtil.fmt2(nextSalaryClaimNet()),
                TextUtil.fmt2(nextSalaryClaimTax())));
        }
        List<JsonObject> assigned = assignedJobs();
        if (assigned.isEmpty()) {
            lines.add(Component.translatable("gui.advancedjobs.no_salary_jobs"));
        } else {
            for (JsonObject assignedJob : assigned) {
                lines.add(Component.translatable("gui.advancedjobs.salary_job_line_compact",
                    Component.translatable(assignedJob.get("nameKey").getAsString()),
                    TextUtil.fmt2(assignedJob.get("pendingSalary").getAsDouble()),
                    TextUtil.fmt2(nextSalaryClaimGross(assignedJob.get("id").getAsString()))));
            }
        }
        return lines;
    }

    private int salaryVisibleRows() {
        return 7;
    }

    private int salaryLineColor(int index) {
        if (index == 0) {
            return 0xFFD37F;
        }
        if (index == 1) {
            return 0x9BE39B;
        }
        if (index == 2) {
            return 0xC8C8C8;
        }
        if (!ClientJobState.instantSalary() && index == 3) {
            return 0x9AD0FF;
        }
        return 0xD0D0D0;
    }

    private int skillUnlockButtonX() {
        return rightAlignedX(120, 20);
    }

    private int selectedJobNextButtonX() {
        return rightAlignedX(30, 40);
    }

    private int topWindowWidth() {
        return 252;
    }

    private int topWindowHeight() {
        return 140;
    }

    private int topWindowX() {
        return (this.width - topWindowWidth()) / 2;
    }

    private int topWindowY() {
        return Math.max(54, (this.height - topWindowHeight()) / 2);
    }

    private int topViewportX() {
        return topWindowX() + 9;
    }

    private int topViewportY() {
        return topWindowY() + 18;
    }

    private int topViewportWidth() {
        return topWindowWidth() - 18;
    }

    private int topViewportHeight() {
        return topWindowHeight() - 27;
    }

    private int topVisibleRows() {
        return TOP_PAGE_SIZE;
    }

    private int topControlsY() {
        return topWindowY() + topWindowHeight() + 6;
    }

    private int topJobPrevButtonX() {
        return topWindowX();
    }

    private int topJobNextButtonX() {
        return topPageNextButtonX() + 54;
    }

    private int topPageNextButtonX() {
        return topSortButtonX() + 100;
    }

    private int topPagePrevButtonX() {
        return topSortButtonX() - 54;
    }

    private int topSortButtonX() {
        return topWindowX() + (topWindowWidth() - 96) / 2;
    }

    private int centeredHeaderWidth() {
        return Math.max(220, this.width - 280);
    }

    private int rightAlignedX(int rightPadding, int minX) {
        return Math.max(minX, this.width - rightPadding);
    }

    private void syncSelectedJob() {
        List<JsonObject> jobs = ClientJobState.jobs();
        if (selectedJob != null) {
            String selectedId = selectedJob.get("id").getAsString();
            for (JsonObject job : jobs) {
                if (job.get("id").getAsString().equals(selectedId)) {
                    selectedJob = job;
                    clampSelectedSkillBranch();
                    return;
                }
            }
        }
        if (preferredJobId != null && !preferredJobId.isBlank()) {
            for (JsonObject job : jobs) {
                if (job.get("id").getAsString().equals(preferredJobId)) {
                    selectedJob = job;
                    clampSelectedSkillBranch();
                    return;
                }
            }
        }
        String active = ClientJobState.activeJobId();
        if (active != null) {
            for (JsonObject job : jobs) {
                if (job.get("id").getAsString().equals(active)) {
                    selectedJob = job;
                    clampSelectedSkillBranch();
                    return;
                }
            }
        }
        selectedJob = jobs.isEmpty() ? null : jobs.get(0);
        clampSelectedSkillBranch();
    }

    private List<JsonObject> skillEntries() {
        List<JsonObject> list = new ArrayList<>();
        if (selectedJob == null || !selectedJob.has("skillBranches")) {
            return list;
        }
        JsonArray branches = selectedJob.getAsJsonArray("skillBranches");
        for (JsonElement branchElement : branches) {
            JsonObject branch = branchElement.getAsJsonObject();
            for (JsonElement nodeElement : branch.getAsJsonArray("nodes")) {
                JsonObject node = nodeElement.getAsJsonObject();
                JsonObject enriched = node.deepCopy();
                enriched.addProperty("branchKey", branch.get("translationKey").getAsString());
                enriched.addProperty("branchId", branch.get("id").getAsString());
                list.add(enriched);
            }
        }
        return list;
    }

    private List<JsonObject> unlockedSkillEntries() {
        List<JsonObject> unlocked = new ArrayList<>();
        for (JsonObject entry : skillEntries()) {
            if (entry.has("unlocked") && entry.get("unlocked").getAsBoolean()) {
                unlocked.add(entry);
            }
        }
        unlocked.sort(Comparator
            .comparingInt((JsonObject node) -> node.get("requiredLevel").getAsInt()).reversed()
            .thenComparing(node -> node.get("id").getAsString()));
        return unlocked;
    }

    private List<JsonObject> leaderboardEntries() {
        List<JsonObject> jobs = ClientJobState.jobs();
        if (jobs.isEmpty()) {
            return List.of();
        }
        leaderboardJobIndex = clampOffset(leaderboardJobIndex, jobs.size());
        List<JsonObject> entries = new ArrayList<>(ClientJobState.leaderboard(currentLeaderboardJobId()));
        entries.sort(topSort.comparator());
        return entries;
    }

    private void requestLeaderboard(String jobId) {
        if (jobId != null && !jobId.equals(lastRequestedLeaderboardJobId)) {
            lastRequestedLeaderboardJobId = jobId;
            PacketHandler.CHANNEL.sendToServer(new RequestLeaderboardPacket(jobId));
        }
    }

    private String currentLeaderboardJobId() {
        List<JsonObject> jobs = ClientJobState.jobs();
        if (jobs.isEmpty()) {
            return OVERALL_LEADERBOARD_ID;
        }
        leaderboardJobIndex = clampOffset(leaderboardJobIndex, jobs.size());
        return leaderboardJobIndex == jobs.size()
            ? OVERALL_LEADERBOARD_ID
            : jobs.get(leaderboardJobIndex).get("id").getAsString();
    }

    private boolean switchableJobTab() {
        return currentTab == Tab.MY_JOB
            || currentTab == Tab.SKILLS;
    }

    private void renderSelectedJobHeader(GuiGraphics graphics) {
        if (selectedJob == null
            || currentTab == Tab.SKILLS
            || currentTab == Tab.DAILY
            || currentTab == Tab.CONTRACTS
            || currentTab == Tab.SALARY
            || currentTab == Tab.TOP) {
            return;
        }
        drawCenteredClampedText(graphics,
            Component.translatable("gui.advancedjobs.viewing_job", Component.translatable(selectedJob.get("nameKey").getAsString())),
            this.width / 2, 60, centeredHeaderWidth(), 0xFFD37F);
    }

    private void shiftSelectedJob(int delta) {
        List<JsonObject> jobs = ClientJobState.jobs();
        if (jobs.isEmpty()) {
            selectedJob = null;
            return;
        }
        if (selectedJob == null) {
            selectedJob = jobs.get(0);
            return;
        }
        int index = 0;
        String selectedId = selectedJob.get("id").getAsString();
        for (int i = 0; i < jobs.size(); i++) {
            if (selectedId.equals(jobs.get(i).get("id").getAsString())) {
                index = i;
                break;
            }
        }
        selectedJob = jobs.get((index + delta + jobs.size()) % jobs.size());
        listOffset = 0;
        skillOffset = 0;
        selectedSkillBranch = 0;
        skillPanX = 0;
        skillPanY = 0;
    }

    private void clampSelectedSkillBranch() {
        if (selectedJob == null || !selectedJob.has("skillBranches")) {
            selectedSkillBranch = 0;
            return;
        }
        JsonArray branches = selectedJob.getAsJsonArray("skillBranches");
        selectedSkillBranch = branches.isEmpty() ? 0 : clampOffset(selectedSkillBranch, branches.size() - 1);
    }

    private List<JsonObject> visibleDailyTasks() {
        List<JsonObject> list = new ArrayList<>();
        if (selectedJob == null || !selectedJob.has("dailyTasks")) {
            return list;
        }
        JsonArray daily = selectedJob.getAsJsonArray("dailyTasks");
        for (JsonElement element : daily) {
            list.add(element.getAsJsonObject());
        }
        return list;
    }

    private List<JsonObject> visibleContracts() {
        List<JsonObject> list = new ArrayList<>();
        if (selectedJob == null || !selectedJob.has("contracts")) {
            return list;
        }
        JsonArray contracts = selectedJob.getAsJsonArray("contracts");
        for (JsonElement element : contracts) {
            list.add(element.getAsJsonObject());
        }
        return list;
    }

    private int clampOffset(int value, int max) {
        return Math.max(0, Math.min(max, value));
    }

    private double requiredXp(JsonObject job) {
        int nextLevel = Math.max(1, job.get("level").getAsInt());
        return XpFormulaUtil.requiredXpForLevel(nextLevel, ConfigManager.COMMON.baseXp.get(), ConfigManager.COMMON.growthFactor.get());
    }

    private Component jobName(String jobId) {
        for (JsonObject job : ClientJobState.jobs()) {
            if (job.get("id").getAsString().equals(jobId)) {
                return Component.translatable(job.get("nameKey").getAsString());
            }
        }
        return Component.literal(jobId);
    }

    private Component selectedJobSlotLabel() {
        if (selectedJob == null) {
            return Component.translatable("gui.advancedjobs.none");
        }
        return selectedJobSlotLabel(selectedJob.get("id").getAsString());
    }

    private Component selectedJobSlotLabel(String jobId) {
        boolean primary = jobId.equals(ClientJobState.activeJobId());
        boolean secondary = jobId.equals(ClientJobState.secondaryJobId());
        if (primary && secondary) {
            return Component.translatable("gui.advancedjobs.slot.both");
        }
        if (primary) {
            return Component.translatable("gui.advancedjobs.slot.primary");
        }
        if (secondary) {
            return Component.translatable("gui.advancedjobs.slot.secondary");
        }
        return Component.translatable("gui.advancedjobs.slot.available");
    }

    private Component professionSlotBadge(String jobId) {
        boolean primary = jobId.equals(ClientJobState.activeJobId());
        boolean secondary = jobId.equals(ClientJobState.secondaryJobId());
        if (primary && secondary) {
            return Component.translatable("gui.advancedjobs.slot.badge.both");
        }
        if (primary) {
            return Component.translatable("gui.advancedjobs.slot.badge.primary");
        }
        if (secondary) {
            return Component.translatable("gui.advancedjobs.slot.badge.secondary");
        }
        return Component.empty();
    }

    private int slotBadgeColor(String jobId) {
        boolean primary = jobId.equals(ClientJobState.activeJobId());
        boolean secondary = jobId.equals(ClientJobState.secondaryJobId());
        if (primary && secondary) {
            return 0xE3B341;
        }
        if (primary) {
            return 0x7FE38A;
        }
        if (secondary) {
            return 0x9AD0FF;
        }
        return 0xFFFFFF;
    }

    private int countEntries(JsonObject job, String key) {
        return job.has(key) ? job.getAsJsonArray(key).size() : 0;
    }

    private int completedEntries(JsonObject job, String key) {
        if (!job.has(key)) {
            return 0;
        }
        int count = 0;
        for (JsonElement element : job.getAsJsonArray(key)) {
            JsonObject entry = element.getAsJsonObject();
            if (entry.has("completed") && entry.get("completed").getAsBoolean()) {
                count++;
            }
        }
        return count;
    }

    private List<JsonObject> assignedJobs() {
        List<JsonObject> jobs = new ArrayList<>();
        for (JsonObject job : ClientJobState.jobs()) {
            String jobId = job.get("id").getAsString();
            if (jobId.equals(ClientJobState.activeJobId()) || jobId.equals(ClientJobState.secondaryJobId())) {
                jobs.add(job);
            }
        }
        return jobs;
    }

    private double totalPendingSalary() {
        return assignedJobs().stream().mapToDouble(job -> job.get("pendingSalary").getAsDouble()).sum();
    }

    private double totalEarnedSalary() {
        return assignedJobs().stream().mapToDouble(job -> job.get("earnedTotal").getAsDouble()).sum();
    }

    private JsonObject jobById(String jobId) {
        for (JsonObject job : ClientJobState.jobs()) {
            if (job.get("id").getAsString().equals(jobId)) {
                return job;
            }
        }
        return null;
    }

    private Component translatedOrFallback(String key, String fallback) {
        Component translated = Component.translatable(key);
        return translated.getString().equals(key) ? Component.literal(fallback) : translated;
    }

    private String fallbackNodeName(JsonObject node) {
        return effectLabel(node.get("effectType").getAsString()) + " " + node.get("effectValue").getAsDouble();
    }

    private String fallbackPassiveName(String key, int level) {
        String[] parts = key.split("\\.");
        if (parts.length >= 3) {
            String profession = humanizeId(parts[2]);
            return Component.translatable("gui.advancedjobs.default_passive", profession, level).getString();
        }
        return Component.translatable("gui.advancedjobs.default_passive_generic", level).getString();
    }

    private String humanizeId(String value) {
        String[] parts = value.split("[._]");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(part.substring(0, 1).toUpperCase(Locale.ROOT));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.toString();
    }

    private String effectLabel(String effectType) {
        if (effectType.contains(":")) {
            try {
                ResourceLocation id = new ResourceLocation(effectType);
                MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(id);
                if (effect != null) {
                    return effect.getDisplayName().getString();
                }
            } catch (Exception ignored) {
            }
        }
        String key = "gui.advancedjobs.effect." + effectType;
        Component translated = Component.translatable(key);
        return translated.getString().equals(key) ? humanizeId(effectType) : translated.getString();
    }

    private void drawProgressBar(GuiGraphics graphics, int x, int y, int width, int height, double value, double max, int backgroundColor, int fillColor) {
        graphics.fill(x, y, x + width, y + height, backgroundColor);
        if (max <= 0.0D) {
            return;
        }
        int fill = (int) Math.round(width * Math.max(0.0D, Math.min(1.0D, value / max)));
        graphics.fill(x + 1, y + 1, x + Math.max(1, fill) - 1, y + height - 1, fillColor);
    }

    private boolean isHovering(int x, int y, int width, int height, int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private int rarityColor(String rarity) {
        return switch (rarity.toLowerCase()) {
            case "rare", "elite", "epic" -> 0xE3B341;
            case "uncommon" -> 0x7FE38A;
            default -> 0x9AD0FF;
        };
    }

    private String contractTimeLeft(JsonObject contract) {
        if (!contract.has("expiresAt")) {
            return "--";
        }
        long seconds = contract.get("expiresAt").getAsLong() - com.example.advancedjobs.util.TimeUtil.now();
        return TimeUtil.formatRemainingSeconds(seconds);
    }

    private String dailyResetTime(JsonObject job) {
        if (job == null || !job.has("dailyResetAt")) {
            return "--";
        }
        return TimeUtil.formatRemainingSeconds(job.get("dailyResetAt").getAsLong() - TimeUtil.now());
    }

    private String nextContractRotationTime(JsonObject job) {
        if (job == null || !job.has("nextContractRotationAt")) {
            return "--";
        }
        return TimeUtil.formatRemainingSeconds(job.get("nextContractRotationAt").getAsLong() - TimeUtil.now());
    }

    private Component dailyLabel(JsonObject task) {
        if (!task.has("type") || !task.has("targetId")) {
            return Component.literal(task.get("id").getAsString());
        }
        return Component.translatable("gui.advancedjobs.task_line", actionLabel(task.get("type").getAsString()), targetLabel(task.get("targetId").getAsString()));
    }

    private Component contractLabel(JsonObject contract) {
        if (!contract.has("type") || !contract.has("targetId")) {
            return Component.literal(contract.get("id").getAsString());
        }
        return Component.translatable("gui.advancedjobs.task_line", actionLabel(contract.get("type").getAsString()), targetLabel(contract.get("targetId").getAsString()));
    }

    private String actionLabel(String actionType) {
        String key = "gui.advancedjobs.action." + actionType.toLowerCase(Locale.ROOT);
        Component translated = Component.translatable(key);
        return translated.getString().equals(key) ? humanizeId(actionType) : translated.getString();
    }

    private String targetLabel(String targetId) {
        try {
            ResourceLocation id = new ResourceLocation(targetId);
            if (ForgeRegistries.ITEMS.containsKey(id)) {
                Item item = ForgeRegistries.ITEMS.getValue(id);
                if (item != null) {
                    return item.getDescription().getString();
                }
            }
            if (ForgeRegistries.BLOCKS.containsKey(id)) {
                Block block = ForgeRegistries.BLOCKS.getValue(id);
                if (block != null) {
                    return block.getName().getString();
                }
            }
            if (ForgeRegistries.ENTITY_TYPES.containsKey(id)) {
                EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(id);
                if (entityType != null) {
                    return entityType.getDescription().getString();
                }
            }
        } catch (Exception ignored) {
        }
        int separator = targetId.indexOf(':');
        String value = separator >= 0 ? targetId.substring(separator + 1) : targetId;
        return humanizeId(value);
    }

    private String rarityLabel(String rarity) {
        String key = "gui.advancedjobs.rarity." + rarity.toLowerCase(Locale.ROOT);
        Component translated = Component.translatable(key);
        return translated.getString().equals(key) ? humanizeId(rarity) : translated.getString();
    }

    private void appendHistoryTooltip(JsonObject job, List<Component> tooltip) {
        JsonArray dailyHistory = job.has("dailyHistory") ? job.getAsJsonArray("dailyHistory") : new JsonArray();
        JsonArray contractHistory = job.has("contractHistory") ? job.getAsJsonArray("contractHistory") : new JsonArray();
        if (dailyHistory.isEmpty() && contractHistory.isEmpty()) {
            tooltip.add(Component.translatable("gui.advancedjobs.no_history_entries"));
            return;
        }
        if (!dailyHistory.isEmpty()) {
            JsonObject entry = dailyHistory.get(0).getAsJsonObject();
            tooltip.add(Component.translatable("gui.advancedjobs.history.daily",
                actionLabel(entry.has("type") ? entry.get("type").getAsString() : "break_block"),
                targetLabel(entry.has("targetId") ? entry.get("targetId").getAsString() : "minecraft:air"),
                TextUtil.fmt2(entry.has("salaryReward") ? entry.get("salaryReward").getAsDouble() : 0.0D),
                TextUtil.fmt2(entry.has("xpReward") ? entry.get("xpReward").getAsDouble() : 0.0D)));
            appendBonusTooltip(entry, tooltip);
        }
        if (!contractHistory.isEmpty()) {
            JsonObject entry = contractHistory.get(0).getAsJsonObject();
            tooltip.add(Component.translatable("gui.advancedjobs.history.contract",
                rarityLabel(entry.has("rarity") ? entry.get("rarity").getAsString() : "common"),
                actionLabel(entry.has("type") ? entry.get("type").getAsString() : "break_block"),
                targetLabel(entry.has("targetId") ? entry.get("targetId").getAsString() : "minecraft:air"),
                TextUtil.fmt2(entry.has("salaryReward") ? entry.get("salaryReward").getAsDouble() : 0.0D),
                TextUtil.fmt2(entry.has("xpReward") ? entry.get("xpReward").getAsDouble() : 0.0D)));
            appendBonusTooltip(entry, tooltip);
        }
    }

    private Component latestUnlockedTitle() {
        List<String> titles = ClientJobState.unlockedTitles();
        if (titles.isEmpty()) {
            return Component.translatable("gui.advancedjobs.none");
        }
        return Component.translatable("title.advancedjobs." + titles.get(titles.size() - 1));
    }

    private Component latestMilestone(JsonObject job) {
        if (job == null || !job.has("milestones")) {
            return Component.translatable("gui.advancedjobs.none");
        }
        JsonArray milestones = job.getAsJsonArray("milestones");
        if (milestones.isEmpty()) {
            return Component.translatable("gui.advancedjobs.none");
        }
        return Component.translatable("milestone.advancedjobs." + milestones.get(milestones.size() - 1).getAsString());
    }

    private void appendMilestoneTooltip(JsonObject job, List<Component> tooltip) {
        if (job == null || !job.has("milestones")) {
            return;
        }
        JsonArray milestones = job.getAsJsonArray("milestones");
        if (milestones.isEmpty()) {
            tooltip.add(Component.translatable("gui.advancedjobs.no_milestones"));
            return;
        }
        tooltip.add(Component.translatable("gui.advancedjobs.milestone_list"));
        for (int i = Math.max(0, milestones.size() - 4); i < milestones.size(); i++) {
            String milestoneId = milestones.get(i).getAsString();
            tooltip.add(Component.translatable("milestone.advancedjobs." + milestoneId));
            String rewardTitleId = milestoneTitleId(milestoneId);
            if (rewardTitleId != null) {
                tooltip.add(Component.translatable("gui.advancedjobs.tooltip.milestone_title",
                    Component.translatable("title.advancedjobs." + rewardTitleId)));
            }
        }
    }

    private String milestoneTitleId(String milestoneId) {
        return switch (milestoneId) {
            case "miner_diamond_25" -> "gem_cutter";
            case "miner_redstone_100" -> "redstone_vein";
            case "lumberjack_logs_500" -> "timber_champion";
            case "forester_cherry_100" -> "cherry_keeper";
            case "forester_birch_150" -> "birch_sentinel";
            case "farmer_harvest_500" -> "field_master";
            case "farmer_planter_250" -> "green_thumb";
            case "harvester_patch_128" -> "harvest_titan";
            case "animal_breeder_stock_150" -> "stock_breeder";
            case "animal_breeder_cattle_75" -> "cattle_baron";
            case "hunter_zombie_100" -> "undead_bane";
            case "boss_hunter_boss_5" -> "bossbreaker";
            case "builder_blocks_500" -> "stonehands";
            case "builder_decor_200" -> "artisan_builder";
            case "merchant_trade_100" -> "deal_maker";
            case "alchemist_brew_64" -> "potion_savant";
            case "enchanter_books_32" -> "rune_reader";
            case "explorer_chunks_64" -> "trailblazer";
            case "treasure_loot_25" -> "vault_seeker";
            case "engineer_redstone_250" -> "circuit_master";
            case "guard_patrol_150" -> "warden_of_roads";
            case "quarry_stone_1000" -> "heart_of_stone";
            case "digger_gravel_500" -> "graveborn_digger";
            case "sand_collector_sand_1000" -> "sea_of_sand";
            case "ice_harvester_blue_100" -> "blue_frost";
            case "shepherd_flock_100" -> "high_shepherd";
            case "shepherd_wool_256" -> "wool_archon";
            case "beekeeper_honey_128" -> "hive_warden";
            case "beekeeper_comb_192" -> "comb_lord";
            case "herbalist_wart_256" -> "wart_whisperer";
            case "herbalist_berries_192" -> "berry_sage";
            default -> null;
        };
    }

    private void appendTitleTooltip(List<Component> tooltip) {
        List<String> titles = ClientJobState.unlockedTitles();
        if (titles.isEmpty()) {
            tooltip.add(Component.translatable("gui.advancedjobs.no_titles"));
            return;
        }
        tooltip.add(Component.translatable("gui.advancedjobs.title_list"));
        for (int i = Math.max(0, titles.size() - 5); i < titles.size(); i++) {
            tooltip.add(Component.translatable("title.advancedjobs." + titles.get(i)));
        }
    }

    private void appendUnlockedPerkTooltip(List<Component> tooltip) {
        List<JsonObject> unlocked = unlockedSkillEntries();
        if (unlocked.isEmpty()) {
            tooltip.add(Component.translatable("gui.advancedjobs.no_unlocked_perks"));
            return;
        }
        tooltip.add(Component.translatable("gui.advancedjobs.active_perks"));
        for (int i = 0; i < Math.min(6, unlocked.size()); i++) {
            JsonObject node = unlocked.get(i);
            tooltip.add(Component.translatable("gui.advancedjobs.active_perk_tooltip",
                translatedOrFallback(node.get("translationKey").getAsString(), fallbackNodeName(node)),
                translatedOrFallback(node.get("branchKey").getAsString(), humanizeId(node.get("branchId").getAsString())),
                effectLabel(node.get("effectType").getAsString()),
                node.get("effectValue").getAsDouble()));
        }
    }

    private void appendUnlockedPerkCardTooltip(JsonObject node, List<Component> tooltip) {
        tooltip.add(translatedOrFallback(node.get("translationKey").getAsString(), fallbackNodeName(node)));
        tooltip.add(Component.translatable("gui.advancedjobs.active_perk_tooltip",
            translatedOrFallback(node.get("translationKey").getAsString(), fallbackNodeName(node)),
            translatedOrFallback(node.get("branchKey").getAsString(), humanizeId(node.get("branchId").getAsString())),
            effectLabel(node.get("effectType").getAsString()),
            node.get("effectValue").getAsDouble()));
    }

    private void appendPassiveCardTooltip(JsonObject passive, List<Component> tooltip) {
        JsonArray keys = passive.getAsJsonArray("keys");
        String key = keys.size() > 0 ? keys.get(0).getAsString() : "missing";
        tooltip.add(translatedOrFallback(key, fallbackPassiveName(key, passive.get("level").getAsInt())));
        tooltip.add(Component.translatable("gui.advancedjobs.achievement_passive_status", passive.get("level").getAsInt()));
    }

    private void appendRewardPreviewTooltip(JsonObject job, List<Component> tooltip) {
        if (job == null || !job.has("rewardPreview")) {
            return;
        }
        JsonArray rewards = job.getAsJsonArray("rewardPreview");
        if (rewards.isEmpty()) {
            return;
        }
        tooltip.add(Component.translatable("gui.advancedjobs.tooltip.reward_preview"));
        int limit = compactProfessionTooltip() ? 2 : rewards.size();
        for (int i = 0; i < Math.min(limit, rewards.size()); i++) {
            JsonObject reward = rewards.get(i).getAsJsonObject();
            tooltip.add(Component.translatable("gui.advancedjobs.tooltip.reward_preview_line",
                actionLabel(reward.get("type").getAsString()),
                targetLabel(reward.get("targetId").getAsString()),
                TextUtil.fmt2(reward.get("salary").getAsDouble()),
                TextUtil.fmt2(reward.get("xp").getAsDouble())));
        }
    }

    private void appendPassivePreviewTooltip(JsonObject job, List<Component> tooltip) {
        if (job == null || !job.has("passives")) {
            return;
        }
        JsonArray passives = job.getAsJsonArray("passives");
        if (passives.isEmpty()) {
            return;
        }
        tooltip.add(Component.translatable("gui.advancedjobs.tooltip.passive_preview"));
        int limit = compactProfessionTooltip() ? 2 : 3;
        for (int i = 0; i < Math.min(limit, passives.size()); i++) {
            JsonObject passive = passives.get(i).getAsJsonObject();
            JsonArray keys = passive.getAsJsonArray("keys");
            String key = keys.size() > 0 ? keys.get(0).getAsString() : "missing";
            tooltip.add(Component.translatable("gui.advancedjobs.tooltip.passive_preview_line",
                passive.get("level").getAsInt(),
                translatedOrFallback(key, fallbackPassiveName(key, passive.get("level").getAsInt()))));
        }
    }

    private void appendBranchPreviewTooltip(JsonObject job, List<Component> tooltip) {
        if (job == null || !job.has("skillBranches")) {
            return;
        }
        JsonArray branches = job.getAsJsonArray("skillBranches");
        if (branches.isEmpty()) {
            return;
        }
        tooltip.add(Component.translatable("gui.advancedjobs.tooltip.branch_preview"));
        for (int i = 0; i < Math.min(3, branches.size()); i++) {
            JsonObject branch = branches.get(i).getAsJsonObject();
            tooltip.add(Component.translatable("gui.advancedjobs.tooltip.branch_preview_line",
                translatedOrFallback(branch.get("translationKey").getAsString(), humanizeId(branch.get("id").getAsString())),
                branch.getAsJsonArray("nodes").size()));
        }
    }

    private void renderProfessionDetails(GuiGraphics graphics, JsonObject job) {
        int panelWidth = professionDetailsPanelWidth();
        int panelX = professionDetailsPanelX();
        int panelY = 86;
        int lineWidth = panelWidth - 12;
        int previewLimit = compactProfessionDetailsPanel() ? 1 : 2;
        List<Component> rewardLines = job == null ? List.of() : rewardPreviewLines(job, previewLimit);
        List<Component> passiveLines = job == null ? List.of() : passivePreviewLines(job, previewLimit);
        List<Component> branchLines = job == null ? List.of() : branchPreviewLines(job, previewLimit);
        int previewStartY = panelY + 298;
        int previewEndY = previewStartY;
        if (job != null) {
            previewEndY = professionDetailsSectionEnd(previewEndY, rewardLines);
            previewEndY = professionDetailsSectionEnd(previewEndY, passiveLines);
            previewEndY = professionDetailsSectionEnd(previewEndY, branchLines);
        }
        int contentBottom = job == null ? panelY + 44 : previewEndY;
        int panelHeight = Math.max(388, contentBottom - panelY + 12);
        graphics.fill(panelX - 8, panelY - 8, panelX + panelWidth, panelY + panelHeight, 0x66161B22);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.profession_details"), panelX, panelY - 2, lineWidth, 0xFFD37F);
        if (job == null) {
            drawClampedText(graphics, Component.translatable("gui.advancedjobs.select_profession_first"), panelX, panelY + 16, lineWidth, 0xB8B8B8);
            return;
        }
        renderJobIcon(graphics, job, panelX, panelY + 14);
        drawClampedText(graphics, Component.translatable(job.get("nameKey").getAsString()), panelX + 22, panelY + 16, lineWidth - 22, 0xFFFFFF);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.job_slot", selectedJobSlotLabel(job.get("id").getAsString())), panelX + 22, panelY + 28, lineWidth - 22, slotBadgeColor(job.get("id").getAsString()));
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.level_xp", job.get("level").getAsInt(), TextUtil.fmt2(job.get("xp").getAsDouble())), panelX, panelY + 44, lineWidth, 0x9AD0FF);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.rank", rankLabel(job.get("level").getAsInt())), panelX, panelY + 56, lineWidth, rankColor(job.get("level").getAsInt()));
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.total_earned", TextUtil.fmt2(job.get("earnedTotal").getAsDouble())), panelX, panelY + 68, lineWidth, 0x9BE39B);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.tooltip.content_summary",
            job.has("rewardCount") ? job.get("rewardCount").getAsInt() : 0,
            job.has("passives") ? job.getAsJsonArray("passives").size() : 0,
            job.has("skillBranches") ? job.getAsJsonArray("skillBranches").size() : 0), panelX, panelY + 80, lineWidth, 0xD0D0D0);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.pending_salary",
            TextUtil.fmt2(job.get("pendingSalary").getAsDouble())), panelX, panelY + 92, lineWidth, 0xFFD37F);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.skill_points",
            job.get("skillPoints").getAsInt()), panelX, panelY + 104, lineWidth, 0xFFBE7F);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.profession_claim_share",
            TextUtil.fmt2(nextSalaryClaimGross(job.get("id").getAsString()))), panelX, panelY + 116, lineWidth, 0x9AD0FF);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.next_passive_unlock",
            nextPassiveUnlockLabel(job)), panelX, panelY + 128, lineWidth, 0xC8C8C8);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.next_skill_point",
            nextSkillPointLevel(job)), panelX, panelY + 140, lineWidth, 0xC8C8C8);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.next_skill_node",
            nextSkillNodeLabel(job)), panelX, panelY + 152, lineWidth, 0xC8C8C8);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.profession_milestones",
            job.has("milestoneCount") ? job.get("milestoneCount").getAsInt() : 0,
            latestMilestone(job)), panelX, panelY + 164, lineWidth, 0x9AD0FF);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.profession_unlocked_nodes",
            job.has("unlockedNodes") ? job.getAsJsonArray("unlockedNodes").size() : 0), panelX, panelY + 176, lineWidth, 0xCFAF6A);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.my_job_summary",
            completedEntries(job, "dailyTasks"), countEntries(job, "dailyTasks"),
            completedEntries(job, "contracts"), countEntries(job, "contracts")), panelX, panelY + 188, lineWidth, 0xD0D0D0);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.unlocked_titles",
            ClientJobState.unlockedTitles().size(), latestUnlockedTitle()), panelX, panelY + 200, lineWidth, 0xCFAF6A);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.profession_daily_cycle",
            dailyResetTime(job)), panelX, panelY + 212, lineWidth, 0xC8C8C8);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.profession_contract_cycle",
            nextContractRotationTime(job)), panelX, panelY + 224, lineWidth, 0xC8C8C8);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.profession_contract_reroll",
            TextUtil.fmt2(ClientJobState.contractRerollPrice()),
            TimeUtil.formatRemainingSeconds(ClientJobState.contractRerollCooldownRemaining())), panelX, panelY + 236, lineWidth, 0xC8C8C8);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.help.salary_mode",
            Component.translatable(ClientJobState.instantSalary() ? "gui.advancedjobs.salary_mode.instant" : "gui.advancedjobs.salary_mode.manual")), panelX, panelY + 248, lineWidth, 0xC8C8C8);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.help.world_multiplier",
            targetLabel(ClientJobState.currentWorldId()), TextUtil.fmt2(ClientJobState.worldRewardMultiplier())), panelX, panelY + 260, lineWidth, 0xC8C8C8);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.help.biome_multiplier",
            targetLabel(ClientJobState.currentBiomeId()), TextUtil.fmt2(ClientJobState.biomeRewardMultiplier())), panelX, panelY + 272, lineWidth, 0xC8C8C8);
        drawClampedText(graphics, Component.translatable("gui.advancedjobs.profession_effective_multiplier",
            TextUtil.fmt2(ClientJobState.effectiveRewardMultiplier())), panelX, panelY + 284, lineWidth, 0x9BE39B);
        int previewY = previewStartY;
        previewY = drawProfessionDetailsSection(graphics,
            Component.translatable("gui.advancedjobs.tooltip.reward_preview"),
            rewardLines,
            panelX, previewY, lineWidth);
        previewY = drawProfessionDetailsSection(graphics,
            Component.translatable("gui.advancedjobs.tooltip.passive_preview"),
            passiveLines,
            panelX, previewY, lineWidth);
        drawProfessionDetailsSection(graphics,
            Component.translatable("gui.advancedjobs.tooltip.branch_preview"),
            branchLines,
            panelX, previewY, lineWidth);
    }

    private boolean compactProfessionDetailsPanel() {
        return this.width <= 1100 || this.height <= 760;
    }

    private int professionDetailsPanelWidth() {
        return Math.max(262, Math.min(320, this.width / 4));
    }

    private int professionDetailsPanelX() {
        return Math.max(360, this.width - (professionDetailsPanelWidth() + 18));
    }

    private int drawProfessionDetailsSection(GuiGraphics graphics, Component header, List<Component> lines,
                                             int panelX, int startY, int lineWidth) {
        drawClampedText(graphics, header, panelX, startY, lineWidth, 0xFFFFFF);
        for (int i = 0; i < lines.size(); i++) {
            drawClampedText(graphics, lines.get(i), panelX + 8, startY + 12 + i * 12, lineWidth - 8, 0xB8B8B8);
        }
        return startY + 16 + Math.max(1, lines.size()) * 12;
    }

    private int professionDetailsSectionEnd(int startY, List<Component> lines) {
        return startY + 16 + Math.max(1, lines.size()) * 12;
    }

    private List<Component> rewardPreviewLines(JsonObject job, int limit) {
        List<Component> lines = new ArrayList<>();
        JsonArray rewards = job.has("rewardPreview") ? job.getAsJsonArray("rewardPreview") : new JsonArray();
        for (int i = 0; i < Math.min(limit, rewards.size()); i++) {
            JsonObject reward = rewards.get(i).getAsJsonObject();
            lines.add(Component.translatable("gui.advancedjobs.profession_details_line",
                actionLabel(reward.get("type").getAsString()), targetLabel(reward.get("targetId").getAsString())));
        }
        appendMoreEntriesLine(lines, rewards.size(), limit);
        return lines;
    }

    private List<Component> passivePreviewLines(JsonObject job, int limit) {
        List<Component> lines = new ArrayList<>();
        JsonArray passives = job.has("passives") ? job.getAsJsonArray("passives") : new JsonArray();
        for (int i = 0; i < Math.min(limit, passives.size()); i++) {
            JsonObject passive = passives.get(i).getAsJsonObject();
            JsonArray keys = passive.getAsJsonArray("keys");
            String key = keys.size() > 0 ? keys.get(0).getAsString() : "missing";
            lines.add(Component.translatable("gui.advancedjobs.tooltip.passive_preview_line",
                passive.get("level").getAsInt(),
                translatedOrFallback(key, fallbackPassiveName(key, passive.get("level").getAsInt()))));
        }
        appendMoreEntriesLine(lines, passives.size(), limit);
        return lines;
    }

    private List<Component> branchPreviewLines(JsonObject job, int limit) {
        List<Component> lines = new ArrayList<>();
        JsonArray branches = job.has("skillBranches") ? job.getAsJsonArray("skillBranches") : new JsonArray();
        for (int i = 0; i < Math.min(limit, branches.size()); i++) {
            JsonObject branch = branches.get(i).getAsJsonObject();
            lines.add(Component.translatable("gui.advancedjobs.tooltip.branch_preview_line",
                translatedOrFallback(branch.get("translationKey").getAsString(), humanizeId(branch.get("id").getAsString())),
                branch.getAsJsonArray("nodes").size()));
        }
        appendMoreEntriesLine(lines, branches.size(), limit);
        return lines;
    }

    private void appendMoreEntriesLine(List<Component> lines, int total, int shown) {
        if (total > shown) {
            lines.add(Component.translatable("gui.advancedjobs.more_entries", total - shown));
        }
    }

    private Component nextPassiveUnlockLabel(JsonObject job) {
        int level = job.get("level").getAsInt();
        JsonArray passives = job.has("passives") ? job.getAsJsonArray("passives") : new JsonArray();
        for (JsonElement element : passives) {
            JsonObject passive = element.getAsJsonObject();
            int requiredLevel = passive.get("level").getAsInt();
            if (requiredLevel > level) {
                JsonArray keys = passive.getAsJsonArray("keys");
                String key = keys.size() > 0 ? keys.get(0).getAsString() : "missing";
                return Component.translatable("gui.advancedjobs.next_passive_unlock_line",
                    requiredLevel,
                    translatedOrFallback(key, fallbackPassiveName(key, requiredLevel)));
            }
        }
        return Component.translatable("gui.advancedjobs.none");
    }

    private int nextSkillPointLevel(JsonObject job) {
        int level = job.get("level").getAsInt();
        return ((level / 5) + 1) * 5;
    }

    private Component nextSkillNodeLabel(JsonObject job) {
        JsonObject node = nextUnlockableNode(job);
        if (node == null) {
            return Component.translatable("gui.advancedjobs.none");
        }
        return Component.translatable("gui.advancedjobs.next_skill_node_line",
            translatedOrFallback(node.get("translationKey").getAsString(), fallbackNodeName(node)),
            node.get("requiredLevel").getAsInt(),
            node.get("cost").getAsInt());
    }

    private JsonObject nextUnlockableNode(JsonObject job) {
        if (job == null || !job.has("skillBranches")) {
            return null;
        }
        JsonObject best = null;
        for (JsonElement branchElement : job.getAsJsonArray("skillBranches")) {
            JsonObject branch = branchElement.getAsJsonObject();
            for (JsonElement nodeElement : branch.getAsJsonArray("nodes")) {
                JsonObject node = nodeElement.getAsJsonObject();
                if (node.has("unlocked") && node.get("unlocked").getAsBoolean()) {
                    continue;
                }
                if (node.has("parentId") && !isUnlockedNode(job, node.get("parentId").getAsString())) {
                    continue;
                }
                if (best == null
                    || node.get("requiredLevel").getAsInt() < best.get("requiredLevel").getAsInt()
                    || (node.get("requiredLevel").getAsInt() == best.get("requiredLevel").getAsInt()
                    && node.get("cost").getAsInt() < best.get("cost").getAsInt())) {
                    best = node;
                }
            }
        }
        return best;
    }

    private boolean isUnlockedNode(JsonObject job, String nodeId) {
        if (job == null || !job.has("skillBranches")) {
            return false;
        }
        for (JsonElement branchElement : job.getAsJsonArray("skillBranches")) {
            JsonObject branch = branchElement.getAsJsonObject();
            for (JsonElement nodeElement : branch.getAsJsonArray("nodes")) {
                JsonObject node = nodeElement.getAsJsonObject();
                if (nodeId.equals(node.get("id").getAsString())) {
                    return node.has("unlocked") && node.get("unlocked").getAsBoolean();
                }
            }
        }
        return false;
    }

    private List<MobEffectInstance> activeEffects() {
        if (this.minecraft == null || this.minecraft.player == null) {
            return List.of();
        }
        List<MobEffectInstance> effects = new ArrayList<>(this.minecraft.player.getActiveEffects());
        effects.sort(Comparator
            .comparingInt(MobEffectInstance::getAmplifier).reversed()
            .thenComparingInt(MobEffectInstance::getDuration).reversed()
            .thenComparing(effect -> effect.getEffect().getDisplayName().getString()));
        return effects;
    }

    private void appendActiveEffectTooltip(List<Component> tooltip) {
        List<MobEffectInstance> effects = activeEffects();
        if (effects.isEmpty()) {
            tooltip.add(Component.translatable("gui.advancedjobs.no_active_effects"));
            return;
        }
        tooltip.add(Component.translatable("gui.advancedjobs.active_effects"));
        for (int i = 0; i < Math.min(6, effects.size()); i++) {
            MobEffectInstance effect = effects.get(i);
            tooltip.add(Component.translatable("gui.advancedjobs.active_effect_line",
                effect.getEffect().getDisplayName(),
                effect.getAmplifier() + 1,
                TimeUtil.formatRemainingSeconds(Math.max(0, effect.getDuration() / 20))));
        }
    }

    private void appendActiveEffectCardTooltip(MobEffectInstance effect, List<Component> tooltip) {
        tooltip.add(effect.getEffect().getDisplayName());
        tooltip.add(Component.translatable("gui.advancedjobs.active_effect_line",
            effect.getEffect().getDisplayName(),
            effect.getAmplifier() + 1,
            TimeUtil.formatRemainingSeconds(Math.max(0, effect.getDuration() / 20))));
    }

    private void appendBonusTooltip(JsonObject entry, List<Component> tooltip) {
        if (entry.has("bonusItem") && entry.has("bonusCount")) {
            tooltip.add(Component.translatable("gui.advancedjobs.reward_bonus_item",
                targetLabel(entry.get("bonusItem").getAsString()), entry.get("bonusCount").getAsInt()));
        }
        if (entry.has("buffEffect") && entry.has("buffDurationSeconds")) {
            tooltip.add(Component.translatable("gui.advancedjobs.reward_bonus_buff",
                effectLabel(entry.get("buffEffect").getAsString()),
                TimeUtil.formatRemainingSeconds(entry.get("buffDurationSeconds").getAsLong()),
                entry.has("buffAmplifier") ? entry.get("buffAmplifier").getAsInt() + 1 : 1));
        }
    }

    private String rankPrefix(int rank) {
        return switch (rank) {
            case 1 -> "#1";
            case 2 -> "#2";
            case 3 -> "#3";
            default -> rank + ".";
        };
    }

    private int currentPlayerRank(List<JsonObject> entries) {
        if (this.minecraft == null || this.minecraft.player == null) {
            return -1;
        }
        String playerName = this.minecraft.player.getGameProfile().getName();
        for (int i = 0; i < entries.size(); i++) {
            if (playerName.equalsIgnoreCase(entries.get(i).get("player").getAsString())) {
                return i + 1;
            }
        }
        return -1;
    }

    private String topLeaderName(List<JsonObject> entries) {
        return entries.isEmpty() ? "-" : entries.get(0).get("player").getAsString();
    }

    private int topLeaderLevel(List<JsonObject> entries) {
        return entries.isEmpty() ? 0 : entries.get(0).get("level").getAsInt();
    }

    private String leaderboardGap(List<JsonObject> entries, int selfRank, boolean overall) {
        if (selfRank <= 1 || selfRank > entries.size()) {
            return null;
        }
        JsonObject current = entries.get(selfRank - 1);
        JsonObject above = entries.get(selfRank - 2);
        double gapValue = topSort == TopSort.EARNED
            ? Math.max(0.0D, above.get("earned").getAsDouble() - current.get("earned").getAsDouble())
            : Math.max(0.0D, above.get("xp").getAsDouble() - current.get("xp").getAsDouble());
        String metric = topSort == TopSort.EARNED
            ? Component.translatable("gui.advancedjobs.top_gap_earned", TextUtil.fmt2(gapValue)).getString()
            : Component.translatable("gui.advancedjobs.top_gap_xp", TextUtil.fmt2(gapValue)).getString();
        return Component.translatable("gui.advancedjobs.top_gap_detail",
            above.get("player").getAsString(),
            metric).getString();
    }

    private int leaderboardRankColor(int rank) {
        return switch (rank) {
            case 1 -> 0xF7D35C;
            case 2 -> 0xC9D4E5;
            case 3 -> 0xD7A86E;
            default -> 0x9AD0FF;
        };
    }

    private Component topSortLabel() {
        return switch (topSort) {
            case LEVEL -> Component.translatable("gui.advancedjobs.top_sort.level");
            case XP -> Component.translatable("gui.advancedjobs.top_sort.xp");
            case EARNED -> Component.translatable("gui.advancedjobs.top_sort.earned");
        };
    }

    private Component rankLabel(int level) {
        String key = level >= 75 ? "gui.advancedjobs.rank.legend"
            : level >= 50 ? "gui.advancedjobs.rank.master"
            : level >= 25 ? "gui.advancedjobs.rank.specialist"
            : "gui.advancedjobs.rank.novice";
        return Component.translatable(key);
    }

    private int rankColor(int level) {
        if (level >= 75) {
            return 0xF7D35C;
        }
        if (level >= 50) {
            return 0xD9E6FF;
        }
        if (level >= 25) {
            return 0x9AD0FF;
        }
        return 0xC8C8C8;
    }

    private void renderJobIcon(GuiGraphics graphics, JsonObject job, int x, int y) {
        ItemStack stack = iconStack(job);
        if (!stack.isEmpty()) {
            graphics.renderItem(stack, x, y);
        }
    }

    private ItemStack iconStack(JsonObject job) {
        if (job == null || !job.has("icon")) {
            return ItemStack.EMPTY;
        }
        try {
            ResourceLocation id = new ResourceLocation(job.get("icon").getAsString());
            Item item = ForgeRegistries.ITEMS.getValue(id);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
        } catch (Exception ignored) {
            return ItemStack.EMPTY;
        }
    }

    private boolean compactProfessionTooltip() {
        return this.width <= 900;
    }

    private void drawClampedText(GuiGraphics graphics, Component text, int x, int y, int maxWidth, int color) {
        graphics.drawString(this.font, clampText(text, maxWidth), x, y, color);
    }

    private void drawRightAlignedClampedText(GuiGraphics graphics, Component text, int rightX, int y, int maxWidth, int color) {
        String clamped = clampText(text, maxWidth);
        graphics.drawString(this.font, clamped, rightX - this.font.width(clamped), y, color);
    }

    private void drawCenteredClampedText(GuiGraphics graphics, Component text, int centerX, int y, int maxWidth, int color) {
        String clamped = clampText(text, maxWidth);
        graphics.drawString(this.font, clamped, centerX - this.font.width(clamped) / 2, y, color);
    }

    private String clampText(Component text, int maxWidth) {
        String value = text.getString();
        if (maxWidth <= 0 || this.font.width(value) <= maxWidth) {
            return value;
        }
        int suffixWidth = this.font.width("...");
        return this.font.plainSubstrByWidth(value, Math.max(0, maxWidth - suffixWidth)) + "...";
    }

    protected enum Tab {
        PROFESSIONS("gui.advancedjobs.tab.jobs"),
        MY_JOB("gui.advancedjobs.tab.my_job"),
        SKILLS("gui.advancedjobs.tab.skills"),
        SALARY("gui.advancedjobs.tab.salary"),
        DAILY("gui.advancedjobs.tab.daily"),
        CONTRACTS("gui.advancedjobs.tab.contracts"),
        TOP("gui.advancedjobs.tab.top"),
        HELP("gui.advancedjobs.tab.help");

        private final String key;

        Tab(String key) {
            this.key = key;
        }
    }

    private enum ScrollTarget {
        NONE,
        PROFESSIONS,
        MY_JOB_PASSIVES,
        MY_JOB_PERKS,
        MY_JOB_EFFECTS,
        SALARY,
        DAILY,
        CONTRACTS,
        SKILLS,
        TOP
    }

    private enum TopSort {
        LEVEL("gui.advancedjobs.sort.level", Comparator
            .comparingInt((JsonObject row) -> row.get("level").getAsInt()).reversed()
            .thenComparingDouble(row -> row.get("xp").getAsDouble()).reversed()
            .thenComparingDouble(row -> row.get("earned").getAsDouble()).reversed()),
        XP("gui.advancedjobs.sort.xp", Comparator
            .comparingDouble((JsonObject row) -> row.get("xp").getAsDouble()).reversed()
            .thenComparingInt(row -> row.get("level").getAsInt()).reversed()
            .thenComparingDouble(row -> row.get("earned").getAsDouble()).reversed()),
        EARNED("gui.advancedjobs.sort.earned", Comparator
            .comparingDouble((JsonObject row) -> row.get("earned").getAsDouble()).reversed()
            .thenComparingInt(row -> row.get("level").getAsInt()).reversed()
            .thenComparingDouble(row -> row.get("xp").getAsDouble()).reversed());

        private final String key;
        private final Comparator<JsonObject> comparator;

        TopSort(String key, Comparator<JsonObject> comparator) {
            this.key = key;
            this.comparator = comparator;
        }

        private Comparator<JsonObject> comparator() {
            return comparator;
        }

        private TopSort next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }
}
