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
package com.mycollab.module.crm.view.contact;

import com.mycollab.module.crm.domain.Contact;
import com.mycollab.module.crm.domain.SimpleContact;
import com.mycollab.module.crm.service.ContactService;
import com.mycollab.spring.AppContextUtil;
import com.mycollab.vaadin.AppContext;
import com.mycollab.vaadin.ui.FieldSelection;
import com.mycollab.vaadin.web.ui.UIConstants;
import com.vaadin.data.Property;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.*;
import org.vaadin.viritin.button.MButton;
import org.vaadin.viritin.layouts.MHorizontalLayout;

/**
 * @author MyCollab Ltd.
 * @since 1.0
 */
public class ContactSelectionField extends CustomField<Integer> implements FieldSelection<Contact> {
    private static final long serialVersionUID = 1L;

    private TextField contactName;
    private MButton browseBtn, clearBtn;

    private SimpleContact contact;

    public ContactSelectionField() {
        contactName = new TextField();
        contactName.setNullRepresentation("");
        contactName.setWidth("100%");
        browseBtn = new MButton("", clickEvent -> {
            ContactSelectionWindow contactWindow = new ContactSelectionWindow(ContactSelectionField.this);
            UI.getCurrent().addWindow(contactWindow);
        }).withIcon(FontAwesome.ELLIPSIS_H).withStyleName(UIConstants.BUTTON_OPTION, UIConstants.BUTTON_SMALL_PADDING);

        clearBtn = new MButton("", clickEvent -> {
            contactName.setValue("");
            contact = null;
        }).withIcon(FontAwesome.TRASH_O).withStyleName(UIConstants.BUTTON_OPTION, UIConstants.BUTTON_SMALL_PADDING);
    }

    @Override
    public void fireValueChange(Contact data) {
        contact = (SimpleContact) data;
        if (contact != null) {
            contactName.setValue(contact.getContactName());
            setInternalValue(contact.getId());
        }

    }

    @Override
    public void setPropertyDataSource(Property newDataSource) {
        final Object value = newDataSource.getValue();
        if (value instanceof Integer) {
            setContactByVal((Integer) value);
            super.setPropertyDataSource(newDataSource);
        } else {
            super.setPropertyDataSource(newDataSource);
        }
    }

    @Override
    public void setValue(Integer value) {
        this.setContactByVal(value);
        super.setValue(value);
    }

    private void setContactByVal(Integer contactId) {
        ContactService contactService = AppContextUtil.getSpringBean(ContactService.class);
        SimpleContact contactVal = contactService.findById(contactId, AppContext.getAccountId());
        if (contactVal != null) {
            setInternalContact(contactVal);
        }
    }

    private void setInternalContact(SimpleContact contact) {
        this.contact = contact;
        contactName.setValue(contact.getContactName());
    }

    public SimpleContact getContact() {
        return this.contact;
    }

    @Override
    protected Component initContent() {
        MHorizontalLayout layout = new MHorizontalLayout().withFullWidth();
        layout.setDefaultComponentAlignment(Alignment.MIDDLE_LEFT);
        layout.with(contactName, browseBtn, clearBtn).expand(contactName);
        return layout;
    }

    @Override
    public Class<Integer> getType() {
        return Integer.class;
    }
}
