/**
 * This file is part of mycollab-web.
 *
 * mycollab-web is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * mycollab-web is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with mycollab-web.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.mycollab.module.project.view.user;

import com.google.common.base.MoreObjects;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.hp.gagawa.java.elements.A;
import com.hp.gagawa.java.elements.Div;
import com.hp.gagawa.java.elements.Img;
import com.mycollab.common.i18n.GenericI18Enum;
import com.mycollab.common.i18n.OptionI18nEnum;
import com.mycollab.configuration.SiteConfiguration;
import com.mycollab.configuration.Storage;
import com.mycollab.configuration.StorageFactory;
import com.mycollab.core.utils.NumberUtils;
import com.mycollab.core.utils.StringUtils;
import com.mycollab.db.arguments.BooleanSearchField;
import com.mycollab.db.arguments.SetSearchField;
import com.mycollab.eventmanager.ApplicationEventListener;
import com.mycollab.eventmanager.EventBusFactory;
import com.mycollab.html.DivLessFormatter;
import com.mycollab.module.project.*;
import com.mycollab.module.project.domain.SimpleProject;
import com.mycollab.module.project.domain.criteria.ItemTimeLoggingSearchCriteria;
import com.mycollab.module.project.events.*;
import com.mycollab.module.project.i18n.*;
import com.mycollab.module.project.service.ItemTimeLoggingService;
import com.mycollab.module.project.service.ProjectService;
import com.mycollab.module.project.ui.ProjectAssetsManager;
import com.mycollab.module.project.ui.ProjectAssetsUtil;
import com.mycollab.module.project.view.ProjectView;
import com.mycollab.module.project.view.parameters.ProjectScreenData;
import com.mycollab.security.RolePermissionCollections;
import com.mycollab.shell.events.ShellEvent;
import com.mycollab.spring.AppContextUtil;
import com.mycollab.vaadin.AppContext;
import com.mycollab.vaadin.mvp.PageActionChain;
import com.mycollab.vaadin.ui.ELabel;
import com.mycollab.vaadin.ui.UIUtils;
import com.mycollab.vaadin.web.ui.ConfirmDialogExt;
import com.mycollab.vaadin.web.ui.OptionPopupContent;
import com.mycollab.vaadin.web.ui.SearchTextField;
import com.mycollab.vaadin.web.ui.UIConstants;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.themes.ValoTheme;
import org.vaadin.hene.popupbutton.PopupButton;
import org.vaadin.viritin.button.MButton;
import org.vaadin.viritin.layouts.MHorizontalLayout;
import org.vaadin.viritin.layouts.MVerticalLayout;

/**
 * @author MyCollab Ltd
 * @since 5.1.2
 */
public class ProjectInfoComponent extends MHorizontalLayout {

    private Label billableHoursLbl, nonBillableHoursLbl;

    private ApplicationEventListener<ProjectEvent.TimeLoggingChangedEvent>
            timeLoggingChangedEventApplicationEventListener = new ApplicationEventListener<ProjectEvent.TimeLoggingChangedEvent>() {
        @Subscribe
        @AllowConcurrentEvents
        @Override
        public void handle(ProjectEvent.TimeLoggingChangedEvent event) {
            ItemTimeLoggingSearchCriteria baseCriteria = new ItemTimeLoggingSearchCriteria();
            baseCriteria.setProjectIds(new SetSearchField<>(CurrentProjectVariables.getProjectId()));

            //get Billable hours
            baseCriteria.setIsBillable(new BooleanSearchField(true));
            ItemTimeLoggingService loggingService = AppContextUtil.getSpringBean(ItemTimeLoggingService.class);
            Double billableHours = loggingService.getTotalHoursByCriteria(baseCriteria);
            billableHoursLbl.setValue(FontAwesome.MONEY.getHtml() + " " + billableHours);

            // Get Non billable hours
            baseCriteria.setIsBillable(new BooleanSearchField(false));
            Double nonBillableHours = loggingService.getTotalHoursByCriteria(baseCriteria);
            nonBillableHoursLbl.setValue(FontAwesome.GIFT.getHtml() + " " + nonBillableHours);
        }
    };

    public ProjectInfoComponent(final SimpleProject project) {
        this.withMargin(true).withStyleName("project-info").withFullWidth();
        Component projectIcon = ProjectAssetsUtil.buildProjectLogo(project.getShortname(), project.getId(), project.getAvatarid(), 64);
        this.with(projectIcon).withAlign(projectIcon, Alignment.TOP_LEFT);
        ELabel headerLbl = ELabel.h2(project.getName());
        headerLbl.setDescription(ProjectTooltipGenerator.generateToolTipProject(AppContext.getUserLocale(), AppContext.getDateFormat(),
                project, AppContext.getSiteUrl(), AppContext.getUserTimeZone()));
        headerLbl.addStyleName(UIConstants.TEXT_ELLIPSIS);
        MVerticalLayout headerLayout = new MVerticalLayout().withMargin(new MarginInfo(false, true, false, true));

        MHorizontalLayout footer = new MHorizontalLayout();
        footer.setDefaultComponentAlignment(Alignment.MIDDLE_LEFT);
        footer.addStyleName(UIConstants.META_INFO);
        footer.addStyleName(UIConstants.FLEX_DISPLAY);

        ELabel createdTimeLbl = new ELabel(FontAwesome.CLOCK_O.getHtml() + " " + AppContext.formatPrettyTime(project
                .getCreatedtime()), ContentMode.HTML).withDescription(AppContext.getMessage(GenericI18Enum.FORM_CREATED_TIME))
                .withStyleName(ValoTheme.LABEL_SMALL).withWidthUndefined();
        footer.addComponents(createdTimeLbl);

        billableHoursLbl = new ELabel(FontAwesome.MONEY.getHtml() + " " + NumberUtils.roundDouble(2, project.getTotalBillableHours()),
                ContentMode.HTML).withDescription(AppContext.getMessage(TimeTrackingI18nEnum.OPT_BILLABLE_HOURS))
                .withStyleName(ValoTheme.LABEL_SMALL).withWidthUndefined();
        footer.addComponents(billableHoursLbl);

        nonBillableHoursLbl = new ELabel(FontAwesome.GIFT.getHtml() + " " + project.getTotalNonBillableHours(),
                ContentMode.HTML).withDescription(AppContext.getMessage(TimeTrackingI18nEnum.OPT_NON_BILLABLE_HOURS))
                .withStyleName(ValoTheme.LABEL_SMALL).withWidthUndefined();
        footer.addComponents(nonBillableHoursLbl);

        if (project.getLead() != null) {
            Div leadAvatar = new DivLessFormatter().appendChild(new Img("", StorageFactory.getAvatarPath
                    (project.getLeadAvatarId(), 16)), DivLessFormatter.EMPTY_SPACE(), new A(ProjectLinkBuilder
                    .generateProjectMemberFullLink(project.getId(),
                            project.getLead())).appendText(StringUtils.trim(project.getLeadFullName(), 30, true)))
                    .setTitle(project.getLeadFullName());
            ELabel leadLbl = new ELabel("Lead: " + leadAvatar.write(), ContentMode.HTML).withWidthUndefined();
            footer.addComponents(leadLbl);
        }
        if (project.getHomepage() != null) {
            ELabel homepageLbl = new ELabel(FontAwesome.WECHAT.getHtml() + " " + new A(project.getHomepage())
                    .appendText(project.getHomepage()).setTarget("_blank").write(), ContentMode.HTML)
                    .withStyleName(ValoTheme.LABEL_SMALL).withWidthUndefined();
            homepageLbl.setDescription(AppContext.getMessage(ProjectI18nEnum.FORM_HOME_PAGE));
        }

        if (project.getNumActiveMembers() > 0) {
            ELabel activeMembersLbl = new ELabel(FontAwesome.USERS.getHtml() + " " + project.getNumActiveMembers(),
                    ContentMode.HTML).withDescription("Active members").withStyleName(ValoTheme.LABEL_SMALL).withWidthUndefined();
            footer.addComponents(activeMembersLbl);
        }

        if (project.getAccountid() != null && !SiteConfiguration.isCommunityEdition()) {
            Div clientDiv = new Div();
            if (project.getClientAvatarId() == null) {
                clientDiv.appendText(FontAwesome.INSTITUTION.getHtml() + " ");
            } else {
                Img clientImg = new Img("", StorageFactory.getEntityLogoPath(AppContext.getAccountId(), project.getClientAvatarId(), 16))
                        .setCSSClass(UIConstants.CIRCLE_BOX);
                clientDiv.appendChild(clientImg).appendChild(DivLessFormatter.EMPTY_SPACE());
            }
            clientDiv.appendChild(new A(ProjectLinkBuilder.generateClientPreviewFullLink(project.getAccountid()))
                    .appendText(project.getClientName()));
            ELabel accountBtn = ELabel.html(clientDiv.write()).withStyleName(UIConstants.BUTTON_BLOCK)
                    .withWidthUndefined();
            footer.addComponents(accountBtn);
        }

        if (!SiteConfiguration.isCommunityEdition()) {
            MButton tagBtn = new MButton(AppContext.getMessage(ProjectCommonI18nEnum.VIEW_TAG), clickEvent -> EventBusFactory.getInstance().post(new ProjectEvent.GotoTagListView(this, null)))
                    .withIcon(FontAwesome.TAGS).withStyleName(UIConstants.BUTTON_SMALL_PADDING, UIConstants.BUTTON_ACTION);
            tagBtn.setDescription("Tag management");
            footer.addComponents(tagBtn);

            MButton favoriteBtn = new MButton(AppContext.getMessage(ProjectCommonI18nEnum.VIEW_FAVORITES),
                    clickEvent -> EventBusFactory.getInstance().post(new ProjectEvent.GotoFavoriteView(this, null)))
                    .withIcon(FontAwesome.STAR).withStyleName(UIConstants.BUTTON_SMALL_PADDING, UIConstants.BUTTON_ACTION);
            favoriteBtn.setDescription("Your favorite list");
            footer.addComponents(favoriteBtn);

            MButton eventBtn = new MButton(AppContext.getMessage(ProjectCommonI18nEnum.VIEW_CALENDAR),
                    clickEvent -> EventBusFactory.getInstance().post(new ProjectEvent.GotoCalendarView(this)))
                    .withIcon(FontAwesome.CALENDAR).withStyleName(UIConstants.BUTTON_SMALL_PADDING, UIConstants.BUTTON_ACTION);
            eventBtn.setDescription("Calendar");
            footer.addComponents(eventBtn);

            MButton ganttChartBtn = new MButton(AppContext.getMessage(ProjectCommonI18nEnum.VIEW_GANTT_CHART),
                    clickEvent -> EventBusFactory.getInstance().post(new ProjectEvent.GotoGanttChart(this, null)))
                    .withIcon(FontAwesome.BAR_CHART_O).withStyleName(UIConstants.BUTTON_SMALL_PADDING, UIConstants.BUTTON_ACTION);
            ganttChartBtn.setDescription("Gantt chart");
            footer.addComponents(ganttChartBtn);
        }

        headerLayout.with(headerLbl, footer);

        MHorizontalLayout topPanel = new MHorizontalLayout().withMargin(false);
        this.with(headerLayout, topPanel).expand(headerLayout).withAlign(topPanel, Alignment.TOP_RIGHT);

        if (project.isProjectArchived()) {
            MButton activeProjectBtn = new MButton(AppContext.getMessage(ProjectCommonI18nEnum.BUTTON_ACTIVE_PROJECT), clickEvent -> {
                ProjectService projectService = AppContextUtil.getSpringBean(ProjectService.class);
                project.setProjectstatus(OptionI18nEnum.StatusI18nEnum.Open.name());
                projectService.updateSelectiveWithSession(project, AppContext.getUsername());

                PageActionChain chain = new PageActionChain(new ProjectScreenData.Goto(CurrentProjectVariables.getProjectId()));
                EventBusFactory.getInstance().post(new ProjectEvent.GotoMyProject(this, chain));
            }).withStyleName(UIConstants.BUTTON_ACTION);
            topPanel.with(activeProjectBtn).withAlign(activeProjectBtn, Alignment.MIDDLE_RIGHT);
        } else {
            SearchTextField searchField = new SearchTextField() {
                public void doSearch(String value) {
                    ProjectView prjView = UIUtils.getRoot(this, ProjectView.class);
                    if (prjView != null) {
                        prjView.displaySearchResult(value);
                    }
                }

                @Override
                public void emptySearch() {

                }
            };

            final PopupButton controlsBtn = new PopupButton();
            controlsBtn.addStyleName(UIConstants.BOX);
            controlsBtn.setIcon(FontAwesome.ELLIPSIS_H);

            OptionPopupContent popupButtonsControl = new OptionPopupContent();

            if (CurrentProjectVariables.canWrite(ProjectRolePermissionCollections.MILESTONES)) {
                MButton createPhaseBtn = new MButton(AppContext.getMessage(MilestoneI18nEnum.NEW), clickEvent -> {
                    controlsBtn.setPopupVisible(false);
                    EventBusFactory.getInstance().post(new MilestoneEvent.GotoAdd(ProjectInfoComponent.this, null));
                }).withIcon(ProjectAssetsManager.getAsset(ProjectTypeConstants.MILESTONE));
                popupButtonsControl.addOption(createPhaseBtn);
            }

            if (CurrentProjectVariables.canWrite(ProjectRolePermissionCollections.TASKS)) {
                MButton createTaskBtn = new MButton(AppContext.getMessage(TaskI18nEnum.NEW), clickEvent -> {
                    controlsBtn.setPopupVisible(false);
                    EventBusFactory.getInstance().post(new TaskEvent.GotoAdd(ProjectInfoComponent.this, null));
                }).withIcon(ProjectAssetsManager.getAsset(ProjectTypeConstants.TASK));
                popupButtonsControl.addOption(createTaskBtn);
            }

            if (CurrentProjectVariables.canWrite(ProjectRolePermissionCollections.BUGS)) {
                MButton createBugBtn = new MButton(AppContext.getMessage(BugI18nEnum.NEW), clickEvent -> {
                    controlsBtn.setPopupVisible(false);
                    EventBusFactory.getInstance().post(new BugEvent.GotoAdd(this, null));
                }).withIcon(ProjectAssetsManager.getAsset(ProjectTypeConstants.BUG));
                popupButtonsControl.addOption(createBugBtn);
            }

            if (CurrentProjectVariables.canWrite(ProjectRolePermissionCollections.COMPONENTS)) {
                MButton createComponentBtn = new MButton(AppContext.getMessage(ComponentI18nEnum.NEW), clickEvent -> {
                    controlsBtn.setPopupVisible(false);
                    EventBusFactory.getInstance().post(new BugComponentEvent.GotoAdd(this, null));
                }).withIcon(ProjectAssetsManager.getAsset(ProjectTypeConstants.BUG_COMPONENT));
                popupButtonsControl.addOption(createComponentBtn);
            }

            if (CurrentProjectVariables.canWrite(ProjectRolePermissionCollections.VERSIONS)) {
                MButton createVersionBtn = new MButton(AppContext.getMessage(VersionI18nEnum.NEW), clickEvent -> {
                    controlsBtn.setPopupVisible(false);
                    EventBusFactory.getInstance().post(new BugVersionEvent.GotoAdd(this, null));
                }).withIcon(ProjectAssetsManager.getAsset(ProjectTypeConstants.BUG_VERSION));
                popupButtonsControl.addOption(createVersionBtn);
            }

            if (!SiteConfiguration.isCommunityEdition() && CurrentProjectVariables.canWrite(ProjectRolePermissionCollections.RISKS)) {
                MButton createRiskBtn = new MButton(AppContext.getMessage(RiskI18nEnum.NEW), clickEvent -> {
                    controlsBtn.setPopupVisible(false);
                    EventBusFactory.getInstance().post(new RiskEvent.GotoAdd(this, null));
                }).withIcon(ProjectAssetsManager.getAsset(ProjectTypeConstants.RISK));
                popupButtonsControl.addOption(createRiskBtn);
            }

            popupButtonsControl.addSeparator();

            if (CurrentProjectVariables.canWrite(ProjectRolePermissionCollections.USERS)) {
                MButton inviteMemberBtn = new MButton(AppContext.getMessage(ProjectMemberI18nEnum.BUTTON_NEW_INVITEES), clickEvent -> {
                    controlsBtn.setPopupVisible(false);
                    EventBusFactory.getInstance().post(new ProjectMemberEvent.GotoInviteMembers(this, null));
                }).withIcon(FontAwesome.SEND);
                popupButtonsControl.addOption(inviteMemberBtn);
            }

            MButton settingBtn = new MButton(AppContext.getMessage(ProjectCommonI18nEnum.VIEW_SETTINGS), clickEvent -> {
                controlsBtn.setPopupVisible(false);
                EventBusFactory.getInstance().post(new ProjectNotificationEvent.GotoList(this, null));
            }).withIcon(FontAwesome.COG);
            popupButtonsControl.addOption(settingBtn);

            popupButtonsControl.addSeparator();

            if (AppContext.canAccess(RolePermissionCollections.CREATE_NEW_PROJECT)) {
                final MButton markProjectTemplateBtn = new MButton().withIcon(FontAwesome.STICKY_NOTE);
                markProjectTemplateBtn.addClickListener(clickEvent -> {
                    Boolean isTemplate = !MoreObjects.firstNonNull(project.getIstemplate(), Boolean.FALSE);
                    project.setIstemplate(isTemplate);
                    ProjectService prjService = AppContextUtil.getSpringBean(ProjectService.class);
                    prjService.updateWithSession(project, AppContext.getUsername());
                    if (project.getIstemplate()) {
                        markProjectTemplateBtn.setCaption(AppContext.getMessage(ProjectI18nEnum.ACTION_UNMARK_TEMPLATE));
                    } else {
                        markProjectTemplateBtn.setCaption(AppContext.getMessage(ProjectI18nEnum.ACTION_MARK_TEMPLATE));
                    }
                });

                Boolean isTemplate = MoreObjects.firstNonNull(project.getIstemplate(), Boolean.FALSE);
                if (isTemplate) {
                    markProjectTemplateBtn.setCaption(AppContext.getMessage(ProjectI18nEnum.ACTION_UNMARK_TEMPLATE));
                } else {
                    markProjectTemplateBtn.setCaption(AppContext.getMessage(ProjectI18nEnum.ACTION_MARK_TEMPLATE));
                }
                popupButtonsControl.addOption(markProjectTemplateBtn);
            }

            if (CurrentProjectVariables.canWrite(ProjectRolePermissionCollections.PROJECT)) {
                MButton editProjectBtn = new MButton(AppContext.getMessage(ProjectI18nEnum.EDIT), clickEvent -> {
                    controlsBtn.setPopupVisible(false);
                    EventBusFactory.getInstance().post(new ProjectEvent.GotoEdit(ProjectInfoComponent.this, project));
                }).withIcon(FontAwesome.EDIT);
                popupButtonsControl.addOption(editProjectBtn);
            }

            if (CurrentProjectVariables.canAccess(ProjectRolePermissionCollections.PROJECT)) {
                MButton archiveProjectBtn = new MButton(AppContext.getMessage(ProjectCommonI18nEnum.BUTTON_ARCHIVE_PROJECT), clickEvent -> {
                    controlsBtn.setPopupVisible(false);
                    ConfirmDialogExt.show(UI.getCurrent(),
                            AppContext.getMessage(GenericI18Enum.WINDOW_WARNING_TITLE, AppContext.getSiteName()),
                            AppContext.getMessage(ProjectCommonI18nEnum.DIALOG_CONFIRM_PROJECT_ARCHIVE_MESSAGE),
                            AppContext.getMessage(GenericI18Enum.BUTTON_YES),
                            AppContext.getMessage(GenericI18Enum.BUTTON_NO),
                            confirmDialog -> {
                                if (confirmDialog.isConfirmed()) {
                                    ProjectService projectService = AppContextUtil.getSpringBean(ProjectService.class);
                                    project.setProjectstatus(OptionI18nEnum.StatusI18nEnum.Archived.name());
                                    projectService.updateSelectiveWithSession(project, AppContext.getUsername());

                                    PageActionChain chain = new PageActionChain(new ProjectScreenData.Goto(CurrentProjectVariables.getProjectId()));
                                    EventBusFactory.getInstance().post(new ProjectEvent.GotoMyProject(this, chain));
                                }
                            });
                }).withIcon(FontAwesome.ARCHIVE);
                popupButtonsControl.addOption(archiveProjectBtn);
            }

            if (CurrentProjectVariables.canAccess(ProjectRolePermissionCollections.PROJECT)) {
                popupButtonsControl.addSeparator();
                MButton deleteProjectBtn = new MButton(AppContext.getMessage(ProjectCommonI18nEnum.BUTTON_DELETE_PROJECT), clickEvent -> {
                    controlsBtn.setPopupVisible(false);
                    ConfirmDialogExt.show(UI.getCurrent(),
                            AppContext.getMessage(GenericI18Enum.DIALOG_DELETE_TITLE, AppContext.getSiteName()),
                            AppContext.getMessage(ProjectCommonI18nEnum.DIALOG_CONFIRM_PROJECT_DELETE_MESSAGE),
                            AppContext.getMessage(GenericI18Enum.BUTTON_YES),
                            AppContext.getMessage(GenericI18Enum.BUTTON_NO),
                            confirmDialog -> {
                                if (confirmDialog.isConfirmed()) {
                                    ProjectService projectService = AppContextUtil.getSpringBean(ProjectService.class);
                                    projectService.removeWithSession(CurrentProjectVariables.getProject(),
                                            AppContext.getUsername(), AppContext.getAccountId());
                                    EventBusFactory.getInstance().post(new ShellEvent.GotoProjectModule(this, null));
                                }
                            });
                }).withIcon(FontAwesome.TRASH_O);
                popupButtonsControl.addDangerOption(deleteProjectBtn);
            }

            controlsBtn.setContent(popupButtonsControl);
            controlsBtn.setWidthUndefined();

            topPanel.with(searchField, controlsBtn).withAlign(searchField, Alignment.TOP_RIGHT).withAlign(controlsBtn,
                    Alignment.TOP_RIGHT);
        }
    }

    @Override
    public void attach() {
        EventBusFactory.getInstance().register(timeLoggingChangedEventApplicationEventListener);
        super.attach();
    }

    @Override
    public void detach() {
        EventBusFactory.getInstance().unregister(timeLoggingChangedEventApplicationEventListener);
        super.detach();
    }
}
