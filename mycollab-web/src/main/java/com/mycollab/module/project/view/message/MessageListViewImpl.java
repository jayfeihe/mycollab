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
package com.mycollab.module.project.view.message;

import com.hp.gagawa.java.elements.A;
import com.hp.gagawa.java.elements.Text;
import com.mycollab.common.i18n.GenericI18Enum;
import com.mycollab.db.arguments.SetSearchField;
import com.mycollab.db.arguments.StringSearchField;
import com.mycollab.module.ecm.domain.Content;
import com.mycollab.module.ecm.service.ResourceService;
import com.mycollab.module.file.AttachmentUtils;
import com.mycollab.module.project.CurrentProjectVariables;
import com.mycollab.module.project.ProjectLinkBuilder;
import com.mycollab.module.project.ProjectRolePermissionCollections;
import com.mycollab.module.project.ProjectTypeConstants;
import com.mycollab.module.project.domain.Message;
import com.mycollab.module.project.domain.SimpleMessage;
import com.mycollab.module.project.domain.criteria.MessageSearchCriteria;
import com.mycollab.module.project.i18n.MessageI18nEnum;
import com.mycollab.module.project.service.MessageService;
import com.mycollab.module.project.ui.ProjectAssetsManager;
import com.mycollab.module.project.ui.components.ProjectListNoItemView;
import com.mycollab.module.project.ui.components.ProjectMemberBlock;
import com.mycollab.spring.AppContextUtil;
import com.mycollab.vaadin.AppContext;
import com.mycollab.vaadin.events.HasEditFormHandlers;
import com.mycollab.vaadin.events.HasSearchHandlers;
import com.mycollab.vaadin.events.IEditFormHandler;
import com.mycollab.vaadin.mvp.AbstractPageView;
import com.mycollab.vaadin.mvp.ViewComponent;
import com.mycollab.vaadin.ui.ELabel;
import com.mycollab.vaadin.ui.NotificationUtil;
import com.mycollab.vaadin.ui.SafeHtmlLabel;
import com.mycollab.vaadin.web.ui.*;
import com.mycollab.vaadin.web.ui.AbstractBeanPagedList.RowDisplayHandler;
import com.vaadin.event.ShortcutAction;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import org.apache.commons.collections.CollectionUtils;
import org.vaadin.easyuploads.MultiFileUploadExt;
import org.vaadin.viritin.button.MButton;
import org.vaadin.viritin.fields.MTextField;
import org.vaadin.viritin.layouts.MHorizontalLayout;
import org.vaadin.viritin.layouts.MVerticalLayout;

import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author MyCollab Ltd.
 * @since 1.0
 */
@ViewComponent
public class MessageListViewImpl extends AbstractPageView implements MessageListView, HasEditFormHandlers<Message> {
    private static final long serialVersionUID = 8433776359091397422L;

    private DefaultBeanPagedList<MessageService, MessageSearchCriteria, SimpleMessage> messageList;
    private Set<IEditFormHandler<Message>> editFormHandlers;
    private MessageSearchCriteria searchCriteria;
    private TopMessagePanel topMessagePanel;
    private boolean isEmpty;

    public MessageListViewImpl() {
        super();
        this.withSpacing(true).withMargin(true).withFullWidth();

        topMessagePanel = new TopMessagePanel();
        topMessagePanel.getSearchHandlers().addSearchHandler(criteria -> messageList.setSearchCriteria(criteria));
        messageList = new DefaultBeanPagedList<>(AppContextUtil.getSpringBean(MessageService.class), new MessageRowDisplayHandler());
        messageList.setControlStyle("borderlessControl");
    }

    @Override
    public void addFormHandler(final IEditFormHandler<Message> handler) {
        if (editFormHandlers == null) {
            editFormHandlers = new HashSet<>();
        }
        editFormHandlers.add(handler);
    }

    private void fireSaveItem(final Message message) {
        if (editFormHandlers != null) {
            for (IEditFormHandler<Message> handler : editFormHandlers) {
                handler.onSave(message);
            }
        }
    }

    @Override
    public HasEditFormHandlers<Message> getEditFormHandlers() {
        return this;
    }

    @Override
    public void setCriteria(final MessageSearchCriteria criteria) {
        this.removeAllComponents();
        this.searchCriteria = criteria;
        MessageService messageService = AppContextUtil.getSpringBean(MessageService.class);
        Integer totalCount = messageService.getTotalCount(searchCriteria);

        this.isEmpty = !(totalCount > 0);
        topMessagePanel.createBasicLayout();
        this.addComponent(topMessagePanel);

        if (this.isEmpty) {
            addComponent(new MessageListNoItemView());
        } else {
            messageList.setSearchCriteria(searchCriteria);
            addComponent(messageList);
        }

    }

    private class MessageRowDisplayHandler implements RowDisplayHandler<SimpleMessage> {
        @Override
        public Component generateRow(AbstractBeanPagedList host, final SimpleMessage message, int rowIndex) {
            final MHorizontalLayout messageLayout = new MHorizontalLayout().withMargin(new MarginInfo(true, false,
                    true, false)).withStyleName("message").withFullWidth();
            if (message.getIsstick() != null && message.getIsstick()) {
                messageLayout.addStyleName("important-message");
            }

            ProjectMemberBlock userBlock = new ProjectMemberBlock(message.getPosteduser(), message.getPostedUserAvatarId(),
                    message.getFullPostedUserName());
            messageLayout.addComponent(userBlock);

            MVerticalLayout rowLayout = new MVerticalLayout().withFullWidth().withStyleName("message-container");

            A labelLink = new A(ProjectLinkBuilder.generateMessagePreviewFullLink(message.getProjectid(), message.getId()),
                    new Text(message.getTitle()));

            MHorizontalLayout messageHeader = new MHorizontalLayout().withMargin(new MarginInfo(false, true,
                    false, false));
            messageHeader.setDefaultComponentAlignment(Alignment.TOP_LEFT);
            CssLayout leftHeader = new CssLayout();
            leftHeader.addComponent(new ELabel(labelLink.write(), ContentMode.HTML).withStyleName(ValoTheme.LABEL_H3
                    + " " + ValoTheme.LABEL_NO_MARGIN));
            ELabel timePostLbl = new ELabel().prettyDateTime(message.getPosteddate());
            timePostLbl.setStyleName(UIConstants.META_INFO);

            MButton deleteBtn = new MButton("", clickEvent -> ConfirmDialogExt.show(UI.getCurrent(),
                    AppContext.getMessage(GenericI18Enum.DIALOG_DELETE_TITLE, AppContext.getSiteName()),
                    AppContext.getMessage(GenericI18Enum.DIALOG_DELETE_SINGLE_ITEM_MESSAGE),
                    AppContext.getMessage(GenericI18Enum.BUTTON_YES),
                    AppContext.getMessage(GenericI18Enum.BUTTON_NO),
                    confirmDialog -> {
                        if (confirmDialog.isConfirmed()) {
                            MessageService messageService = AppContextUtil.getSpringBean(MessageService.class);
                            messageService.removeWithSession(message, AppContext.getUsername(), AppContext.getAccountId());
                            messageList.setSearchCriteria(searchCriteria);
                        }
                    })).withIcon(FontAwesome.TRASH_O).withStyleName(UIConstants.BUTTON_ICON_ONLY);
            deleteBtn.setVisible(CurrentProjectVariables.canAccess(ProjectRolePermissionCollections.MESSAGES));

            MHorizontalLayout rightHeader = new MHorizontalLayout();
            rightHeader.setDefaultComponentAlignment(Alignment.MIDDLE_RIGHT);
            rightHeader.with(timePostLbl, deleteBtn);

            messageHeader.with(leftHeader, rightHeader).expand(leftHeader);

            rowLayout.addComponent(messageHeader);

            SafeHtmlLabel messageContent = new SafeHtmlLabel(message.getMessage());
            rowLayout.addComponent(messageContent);

            MHorizontalLayout notification = new MHorizontalLayout().withStyleName("notification");
            notification.setSizeUndefined();
            if (message.getCommentsCount() > 0) {
                MHorizontalLayout commentNotification = new MHorizontalLayout();
                Label commentCountLbl = new Label(Integer.toString(message.getCommentsCount()) + " " + FontAwesome.COMMENTS
                        .getHtml(), ContentMode.HTML);
                commentCountLbl.setSizeUndefined();
                commentNotification.addComponent(commentCountLbl);
                notification.addComponent(commentNotification);
            }
            ResourceService attachmentService = AppContextUtil.getSpringBean(ResourceService.class);
            List<Content> attachments = attachmentService.getContents(AttachmentUtils
                    .getProjectEntityAttachmentPath(AppContext.getAccountId(),
                            message.getProjectid(), ProjectTypeConstants.MESSAGE, "" + message.getId()));
            if (CollectionUtils.isNotEmpty(attachments)) {
                HorizontalLayout attachmentNotification = new HorizontalLayout();
                Label attachmentCountLbl = new Label(Integer.toString(attachments.size()));
                attachmentCountLbl.setSizeUndefined();
                attachmentNotification.addComponent(attachmentCountLbl);
                Button attachmentIcon = new Button(FontAwesome.PAPERCLIP);
                attachmentIcon.addStyleName(UIConstants.BUTTON_ICON_ONLY);
                attachmentNotification.addComponent(attachmentIcon);
                notification.addComponent(attachmentNotification);
            }

            if (notification.getComponentCount() > 0) {
                MVerticalLayout messageFooter = new MVerticalLayout().withSpacing(false).withFullWidth()
                        .with(notification).withAlign(notification, Alignment.MIDDLE_RIGHT);
                rowLayout.addComponent(messageFooter);
            }

            messageLayout.with(rowLayout).expand(rowLayout);
            return messageLayout;
        }
    }

    private static class MessageSearchPanel extends GenericSearchPanel<MessageSearchCriteria> {
        private MessageSearchCriteria messageSearchCriteria;
        private TextField nameField;

        MessageSearchPanel() {
            createBasicSearchLayout();
        }

        @Override
        public void setTotalCountNumber(int totalCountNumber) {

        }

        private void createBasicSearchLayout() {
            final MHorizontalLayout basicSearchBody = new MHorizontalLayout().withWidthUndefined();

            nameField = new MTextField().withInputPrompt(AppContext.getMessage(GenericI18Enum.ACTION_QUERY_BY_TEXT))
                    .withWidth(UIConstants.DEFAULT_CONTROL_WIDTH);

            MButton searchBtn = new MButton(AppContext.getMessage(GenericI18Enum.BUTTON_SEARCH), clickEvent -> doSearch())
                    .withStyleName(UIConstants.BUTTON_ACTION).withIcon(FontAwesome.SEARCH)
                    .withClickShortcut(ShortcutAction.KeyCode.ENTER);
            basicSearchBody.with(nameField, searchBtn).withAlign(nameField, Alignment.MIDDLE_LEFT);
            this.setCompositionRoot(basicSearchBody);
        }

        private void doSearch() {
            messageSearchCriteria = new MessageSearchCriteria();
            messageSearchCriteria.setProjectids(new SetSearchField<>(CurrentProjectVariables.getProjectId()));
            messageSearchCriteria.setMessage(StringSearchField.and(nameField.getValue()));
            notifySearchHandler(messageSearchCriteria);
        }
    }

    private final class TopMessagePanel extends MVerticalLayout {
        private static final long serialVersionUID = 1L;
        private MessageSearchPanel messageSearchPanel;
        private MHorizontalLayout messagePanelBody;

        TopMessagePanel() {
            this.withFullWidth().withStyleName("message-toppanel");
            messagePanelBody = new MHorizontalLayout().withSpacing(false).withStyleName("message-toppanel-body").withFullWidth();
            messagePanelBody.setDefaultComponentAlignment(Alignment.MIDDLE_LEFT);

            messageSearchPanel = new MessageSearchPanel();
            messageSearchPanel.setWidth("400px");
            this.addComponent(messagePanelBody);

            this.createBasicLayout();
        }

        private void createAddMessageLayout() {
            messagePanelBody.removeAllComponents();
            MVerticalLayout addMessageWrapper = new MVerticalLayout().withWidth("700px");

            final RichTextArea ckEditorTextField = new RichTextArea();
            ckEditorTextField.setWidth("100%");
            ckEditorTextField.setHeight("200px");

            final AttachmentPanel attachments = new AttachmentPanel();
            final TextField titleField = new TextField();

            MHorizontalLayout titleLayout = new MHorizontalLayout().withFullWidth();
            Label titleLbl = new Label(AppContext.getMessage(MessageI18nEnum.FORM_TITLE));
            titleField.setWidth("100%");
            titleField.setNullRepresentation("");
            titleField.setRequired(true);
            titleField.setRequiredError(AppContext.getMessage(MessageI18nEnum.FORM_TITLE_REQUIRED_ERROR));

            titleLayout.with(titleLbl, titleField).expand(titleField);

            addMessageWrapper.with(titleLayout, ckEditorTextField).withAlign(titleLayout, Alignment.MIDDLE_LEFT)
                    .withAlign(ckEditorTextField, Alignment.MIDDLE_CENTER).expand(ckEditorTextField);

            MHorizontalLayout controls = new MHorizontalLayout().withFullWidth();

            MultiFileUploadExt uploadExt = new MultiFileUploadExt(attachments);
            uploadExt.addComponent(attachments);
            controls.with(uploadExt).withAlign(uploadExt, Alignment.TOP_LEFT).expand(uploadExt);

            final CheckBox chkIsStick = new CheckBox(AppContext.getMessage(MessageI18nEnum.FORM_IS_STICK));
            controls.with(chkIsStick).withAlign(chkIsStick, Alignment.TOP_RIGHT);

            MButton cancelBtn = new MButton(AppContext.getMessage(GenericI18Enum.BUTTON_CANCEL),
                    clickEvent -> MessageListViewImpl.this.setCriteria(searchCriteria))
                    .withStyleName(UIConstants.BUTTON_OPTION);
            controls.with(cancelBtn).withAlign(cancelBtn, Alignment.TOP_RIGHT);

            MButton saveBtn = new MButton(AppContext.getMessage(GenericI18Enum.BUTTON_POST), clickEvent -> {
                Message message = new Message();
                message.setProjectid(CurrentProjectVariables.getProjectId());
                message.setPosteddate(new GregorianCalendar().getTime());
                if (!titleField.getValue().trim().equals("")) {
                    message.setTitle(titleField.getValue());
                    message.setMessage(ckEditorTextField.getValue());
                    message.setPosteduser(AppContext.getUsername());
                    message.setSaccountid(AppContext.getAccountId());
                    message.setIsstick(chkIsStick.getValue());
                    MessageListViewImpl.this.fireSaveItem(message);

                    String attachmentPath = AttachmentUtils.getProjectEntityAttachmentPath(
                            AppContext.getAccountId(), message.getProjectid(),
                            ProjectTypeConstants.MESSAGE, "" + message.getId());
                    attachments.saveContentsToRepo(attachmentPath);
                } else {
                    titleField.addStyleName("errorField");
                    NotificationUtil.showErrorNotification(AppContext.getMessage(MessageI18nEnum.FORM_TITLE_REQUIRED_ERROR));
                }
            }).withIcon(FontAwesome.SAVE).withStyleName(UIConstants.BUTTON_ACTION);

            controls.with(saveBtn).withAlign(saveBtn, Alignment.TOP_RIGHT);

            addMessageWrapper.with(controls).withAlign(controls, Alignment.MIDDLE_CENTER);
            messagePanelBody.addComponent(addMessageWrapper);
        }

        void createBasicLayout() {
            messagePanelBody.removeAllComponents();
            messagePanelBody.addComponent(messageSearchPanel);

            if (!isEmpty) {
                MButton createMessageBtn = new MButton(AppContext.getMessage(MessageI18nEnum.NEW),
                        clickEvent -> createAddMessageLayout())
                        .withIcon(FontAwesome.PLUS).withStyleName(UIConstants.BUTTON_ACTION)
                        .withVisible(CurrentProjectVariables.canWrite(ProjectRolePermissionCollections.MESSAGES));

                messagePanelBody.addComponent(createMessageBtn);
                messagePanelBody.setComponentAlignment(createMessageBtn, Alignment.MIDDLE_RIGHT);
            }
        }

        public HasSearchHandlers<MessageSearchCriteria> getSearchHandlers() {
            return messageSearchPanel;
        }
    }

    private void createAddMessageLayout() {
        removeAllComponents();
        topMessagePanel.createAddMessageLayout();
        addComponent(topMessagePanel);
    }

    private class MessageListNoItemView extends ProjectListNoItemView {
        private static final long serialVersionUID = 6711716775690122182L;

        @Override
        protected FontAwesome viewIcon() {
            return ProjectAssetsManager.getAsset(ProjectTypeConstants.MESSAGE);
        }

        @Override
        protected String viewTitle() {
            return AppContext.getMessage(GenericI18Enum.VIEW_NO_ITEM_TITLE);
        }

        @Override
        protected String viewHint() {
            return AppContext.getMessage(GenericI18Enum.VIEW_NO_ITEM_HINT);
        }

        @Override
        protected String actionMessage() {
            return AppContext.getMessage(MessageI18nEnum.NEW);
        }

        @Override
        protected Button.ClickListener actionListener() {
            return clickEvent -> createAddMessageLayout();
        }

        @Override
        protected boolean hasPermission() {
            return CurrentProjectVariables.canWrite(ProjectRolePermissionCollections.MESSAGES);
        }
    }
}
