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
package com.mycollab.module.crm.view.campaign;

import com.mycollab.common.i18n.GenericI18Enum;
import com.mycollab.configuration.SiteConfiguration;
import com.mycollab.db.arguments.NumberSearchField;
import com.mycollab.db.arguments.SearchField;
import com.mycollab.module.crm.CrmLinkGenerator;
import com.mycollab.module.crm.CrmTypeConstants;
import com.mycollab.module.crm.domain.CampaignLead;
import com.mycollab.module.crm.domain.CampaignWithBLOBs;
import com.mycollab.module.crm.domain.SimpleLead;
import com.mycollab.module.crm.domain.criteria.LeadSearchCriteria;
import com.mycollab.module.crm.i18n.LeadI18nEnum;
import com.mycollab.module.crm.service.CampaignService;
import com.mycollab.module.crm.service.LeadService;
import com.mycollab.module.crm.ui.CrmAssetsManager;
import com.mycollab.module.crm.ui.components.RelatedListComp2;
import com.mycollab.security.RolePermissionCollections;
import com.mycollab.spring.AppContextUtil;
import com.mycollab.vaadin.AppContext;
import com.mycollab.vaadin.ui.ELabel;
import com.mycollab.vaadin.web.ui.ConfirmDialogExt;
import com.mycollab.vaadin.web.ui.OptionPopupContent;
import com.mycollab.vaadin.web.ui.SplitButton;
import com.mycollab.vaadin.web.ui.UIConstants;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.*;
import org.vaadin.viritin.button.MButton;
import org.vaadin.viritin.layouts.MHorizontalLayout;

/**
 * @author MyCollab Ltd.
 * @since 1.0
 */
public class CampaignLeadListComp extends RelatedListComp2<LeadService, LeadSearchCriteria, SimpleLead> {
    private static final long serialVersionUID = 1L;

    private CampaignWithBLOBs campaign;

    public CampaignLeadListComp() {
        super(AppContextUtil.getSpringBean(LeadService.class), 20);
        this.setBlockDisplayHandler(new CampaignLeadBlockDisplay());
    }

    @Override
    protected Component generateTopControls() {
        VerticalLayout controlsBtnWrap = new VerticalLayout();
        controlsBtnWrap.setWidth("100%");

        if (AppContext.canWrite(RolePermissionCollections.CRM_LEAD)) {
            final SplitButton controlsBtn = new SplitButton();
            controlsBtn.addStyleName(UIConstants.BUTTON_ACTION);
            controlsBtn.setCaption(AppContext.getMessage(LeadI18nEnum.NEW));
            controlsBtn.setIcon(FontAwesome.PLUS);
            controlsBtn.addClickListener(event -> fireNewRelatedItem(""));
            final Button selectBtn = new Button(AppContext.getMessage(GenericI18Enum.BUTTON_SELECT), clickEvent -> {
                CampaignLeadSelectionWindow leadsWindow = new CampaignLeadSelectionWindow(CampaignLeadListComp.this);
                LeadSearchCriteria criteria = new LeadSearchCriteria();
                criteria.setSaccountid(new NumberSearchField(AppContext.getAccountId()));
                UI.getCurrent().addWindow(leadsWindow);
                leadsWindow.setSearchCriteria(criteria);
                controlsBtn.setPopupVisible(false);
            });
            selectBtn.setIcon(CrmAssetsManager.getAsset(CrmTypeConstants.LEAD));
            OptionPopupContent buttonControlLayout = new OptionPopupContent();
            buttonControlLayout.addOption(selectBtn);
            controlsBtn.setContent(buttonControlLayout);

            controlsBtnWrap.addComponent(controlsBtn);
            controlsBtnWrap.setComponentAlignment(controlsBtn, Alignment.MIDDLE_RIGHT);
        }

        return controlsBtnWrap;
    }

    public void displayLeads(final CampaignWithBLOBs campaign) {
        this.campaign = campaign;
        loadLeads();
    }

    private void loadLeads() {
        final LeadSearchCriteria criteria = new LeadSearchCriteria();
        criteria.setSaccountid(new NumberSearchField(SearchField.AND, AppContext.getAccountId()));
        criteria.setCampaignId(new NumberSearchField(SearchField.AND, campaign.getId()));
        setSearchCriteria(criteria);
    }

    @Override
    public void refresh() {
        loadLeads();
    }

    public class CampaignLeadBlockDisplay implements BlockDisplayHandler<SimpleLead> {

        @Override
        public Component generateBlock(final SimpleLead lead, int blockIndex) {
            CssLayout beanBlock = new CssLayout();
            beanBlock.addStyleName("bean-block");
            beanBlock.setWidth("350px");

            VerticalLayout blockContent = new VerticalLayout();
            MHorizontalLayout blockTop = new MHorizontalLayout();
            CssLayout iconWrap = new CssLayout();
            iconWrap.setStyleName("icon-wrap");
            ELabel leadAvatar = ELabel.fontIcon(CrmAssetsManager.getAsset(CrmTypeConstants.LEAD));
            leadAvatar.addStyleName("icon-48px");
            iconWrap.addComponent(leadAvatar);
            blockTop.addComponent(iconWrap);

            VerticalLayout leadInfo = new VerticalLayout();
            leadInfo.setSpacing(true);

            MButton btnDelete = new MButton("", clickEvent -> {
                ConfirmDialogExt.show(UI.getCurrent(),
                        AppContext.getMessage(GenericI18Enum.DIALOG_DELETE_TITLE, AppContext.getSiteName()),
                        AppContext.getMessage(GenericI18Enum.DIALOG_DELETE_SINGLE_ITEM_MESSAGE),
                        AppContext.getMessage(GenericI18Enum.BUTTON_YES),
                        AppContext.getMessage(GenericI18Enum.BUTTON_NO),
                        confirmDialog -> {
                            if (confirmDialog.isConfirmed()) {
                                final CampaignService accountService = AppContextUtil.getSpringBean(CampaignService.class);
                                final CampaignLead associateLead = new CampaignLead();
                                associateLead.setCampaignid(campaign.getId());
                                associateLead.setLeadid(lead.getId());
                                accountService.removeCampaignLeadRelationship(associateLead, AppContext.getAccountId());
                                CampaignLeadListComp.this.refresh();
                            }
                        });
            }).withIcon(FontAwesome.TRASH_O).withStyleName(UIConstants.BUTTON_ICON_ONLY);

            blockContent.addComponent(btnDelete);
            blockContent.setComponentAlignment(btnDelete, Alignment.TOP_RIGHT);

            Label leadName = new Label("Name: <a href='"
                    + SiteConfiguration.getSiteUrl(AppContext.getUser()
                    .getSubdomain())
                    + CrmLinkGenerator.generateCrmItemLink(
                    CrmTypeConstants.LEAD, lead.getId()) + "'>"
                    + lead.getLeadName() + "</a>", ContentMode.HTML);

            leadInfo.addComponent(leadName);

            Label leadStatus = new Label("Status: "
                    + (lead.getStatus() != null ? lead.getStatus() : ""));
            leadInfo.addComponent(leadStatus);

            Label leadEmail = new Label("Email: "
                    + (lead.getEmail() != null ? "<a href='mailto:"
                    + lead.getEmail() + "'>" + lead.getEmail() + "</a>"
                    : ""), ContentMode.HTML);
            leadInfo.addComponent(leadEmail);

            Label leadOfficePhone = new Label("Office Phone: "
                    + (lead.getOfficephone() != null ? lead.getOfficephone()
                    : ""));
            leadInfo.addComponent(leadOfficePhone);

            blockTop.addComponent(leadInfo);
            blockTop.setExpandRatio(leadInfo, 1.0f);
            blockTop.setWidth("100%");
            blockContent.addComponent(blockTop);

            blockContent.setWidth("100%");

            beanBlock.addComponent(blockContent);
            return beanBlock;
        }

    }
}
