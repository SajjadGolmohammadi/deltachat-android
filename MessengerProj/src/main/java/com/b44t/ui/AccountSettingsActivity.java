/*******************************************************************************
 *
 *                          Messenger Android Frontend
 *                           (C) 2017 Björn Petersen
 *                    Contact: r10s@b44t.com, http://b44t.com
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program.  If not, see http://www.gnu.org/licenses/ .
 *
 *******************************************************************************
 *
 * File:    MrAccoutSettingsActivity.java
 * Purpose: Let the user configure his name
 *
 ******************************************************************************/

package com.b44t.ui;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;

import com.b44t.messenger.AndroidUtilities;
import com.b44t.messenger.ApplicationLoader;
import com.b44t.messenger.LocaleController;
import com.b44t.messenger.MrMailbox;
import com.b44t.messenger.NotificationCenter;
import com.b44t.messenger.R;
import com.b44t.messenger.Utilities;
import com.b44t.ui.ActionBar.ActionBar;
import com.b44t.ui.ActionBar.ActionBarMenu;
import com.b44t.ui.ActionBar.BaseFragment;
import com.b44t.ui.Adapters.BaseFragmentAdapter;
import com.b44t.ui.Cells.HeaderCell;
import com.b44t.ui.Cells.EditTextCell;
import com.b44t.ui.Cells.ShadowSectionCell;
import com.b44t.ui.Cells.TextInfoCell;
import com.b44t.ui.Components.LayoutHelper;


public class AccountSettingsActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    // the list
    private ListAdapter listAdapter;

    private int         rowAddrHeadline;
    private int         rowAddr;
    private int         rowMailPwHeadline;
    private int         rowMailPw;
    private int         rowOpenAdvOpions;

    private int         rowSectionMail;
    private int         rowMailServer;
    private int         rowMailPort;
    private int         rowMailUser;
    private int         rowBreak2;

    private int         rowSectionSend;
    private int         rowSendServer;
    private int         rowSendPort;
    private int         rowSendUser;
    private int         rowSendPw;
    private int         rowInfoBelowSendPw;
    private int         rowCount;

    private final int ROWTYPE_INFO         = 0; // no gaps here!
    private final int ROWTYPE_TEXT_ENTRY   = 1;
    private final int ROWTYPE_SHADOW_BREAK = 2;
    private final int ROWTYPE_HEADLINE     = 3;

    private EditTextCell addrCell;  // warning all these objects may be null!
    private EditTextCell mailPwCell;
    private EditTextCell mailServerCell;
    private EditTextCell mailPortCell;
    private EditTextCell mailUserCell;
    private EditTextCell sendPwCell;
    private EditTextCell sendServerCell;
    private EditTextCell sendPortCell;
    private EditTextCell sendUserCell;

    // misc.
    private final int        ID_DONE_BUTTON = 1;
    private ProgressDialog   progressDialog = null;
    private boolean          fromIntro;
    private boolean          m_expanded = false;

    public AccountSettingsActivity(Bundle args) {
        super();
        if( args!=null ) {
            fromIntro = args.getBoolean("fromIntro", false);
        }
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();

        NotificationCenter.getInstance().addObserver(this, NotificationCenter.connectionStateChanged);

        m_expanded = false;
        if( !MrMailbox.getConfig("mail_user", "").isEmpty()
         || !MrMailbox.getConfig("mail_server", "").isEmpty()
         || !MrMailbox.getConfig("mail_port", "").isEmpty()
         || !MrMailbox.getConfig("send_user", "").isEmpty()
         || !MrMailbox.getConfig("send_pw", "").isEmpty()
         || !MrMailbox.getConfig("send_server", "").isEmpty()
         || !MrMailbox.getConfig("send_port", "").isEmpty() ) {
            m_expanded = true;
        }


        calculateRows();

        return true;
    }

    private void calculateRows()
    {
        rowCount = 0;

        rowAddrHeadline  = rowCount++;
        rowAddr          = rowCount++;
        rowMailPwHeadline= rowCount++;
        rowMailPw        = rowCount++;
        rowOpenAdvOpions = rowCount++;

        if( m_expanded ) {
            rowSectionMail = rowCount++;
            rowMailUser    = rowCount++; // should be the first additional option, the loginname is the component, that cannot be configured automatically (if not derivable from the address)
            rowMailServer  = rowCount++;
            rowMailPort    = rowCount++;
            rowBreak2      = rowCount++;

            rowSectionSend = rowCount++;
            rowSendUser    = rowCount++;
            rowSendPw      = rowCount++;
            rowSendServer  = rowCount++;
            rowSendPort    = rowCount++;
        }
        else {
            rowSectionMail = -1;
            rowMailUser    = -1;
            rowMailServer  = -1;
            rowMailPort    = -1;
            rowBreak2      = -1;

            rowSectionSend = -1;
            rowSendUser    = -1;
            rowSendPw      = -1;
            rowSendServer  = -1;
            rowSendPort    = -1;
        }

        rowInfoBelowSendPw = rowCount++;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.connectionStateChanged);
    }

    @Override
    public View createView(Context context) {

        // create action bar
        if( !fromIntro ) {
            actionBar.setBackButtonImage(R.drawable.ic_close_white);
        }

        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("AccountSettings", R.string.AccountSettings));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1 && !fromIntro ) {
                    if( isModified() ) { // as we use "close/ok" buttons instead of a "back" button it is more clear what happens, however, an additional question does not disturb here
                        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                        builder.setMessage(LocaleController.getString("DiscardChanges", R.string.DiscardChanges));
                        builder.setPositiveButton(LocaleController.getString("Yes", R.string.Yes), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                finishFragment();
                            }
                        });
                        builder.setNegativeButton(LocaleController.getString("No", R.string.No), null);
                        showDialog(builder.create());
                    }
                    else {
                        finishFragment();
                    }
                } else if (id == ID_DONE_BUTTON) {
                    saveData();
                }
            }
        });

        ActionBarMenu menu = actionBar.createMenu();
        menu.addItemWithWidth(ID_DONE_BUTTON, R.drawable.ic_done, AndroidUtilities.dp(56));

        // create object to hold the whole view
        fragmentView = new FrameLayout(context);
        FrameLayout frameLayout = (FrameLayout) fragmentView;
        frameLayout.setBackgroundColor(0xfff0f0f0);

        // create the main layout list
        listAdapter = new ListAdapter(context);

        ListView listView = new ListView(context);
        listView.setDivider(null);
        listView.setDividerHeight(0);
        listView.setVerticalScrollBarEnabled(false);
        listView.setDrawSelectorOnTop(true);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        listView.setAdapter(listAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(final AdapterView<?> adapterView, View view, final int i, long l) {
                if( i==rowOpenAdvOpions ) {
                    m_expanded = !m_expanded;
                    calculateRows();
                    listAdapter.notifyDataSetChanged();
                }
            }
        });

        return fragmentView;
    }

    private void saveData() {
        // Warning: the widgets are created as needed and may not be present!
        String v;

        if( addrCell!=null) {
            v = addrCell.getValue().trim();
            MrMailbox.setConfig("addr", v.isEmpty() ? null : v);
        }

        if( mailPwCell!=null) {
            v = mailPwCell.getValue().trim();
            MrMailbox.setConfig("mail_pw", v.isEmpty() ? null : v);
        }

        if( mailServerCell!=null) {
            v = mailServerCell.getValue().trim();
            MrMailbox.setConfig("mail_server", v.isEmpty() ? null : v);
        }

        if( mailPortCell!=null ) {
            v = mailPortCell.getValue().trim();
            MrMailbox.setConfig("mail_port", v.isEmpty() ? null : v);
        }

        if( mailUserCell!=null) {
            v = mailUserCell.getValue().trim();
            MrMailbox.setConfig("mail_user", v.isEmpty() ? null : v);
        }

        if( sendServerCell!=null ) {
            v = sendServerCell.getValue().trim();
            MrMailbox.setConfig("send_server", v.isEmpty() ? null : v);
        }

        if( sendPortCell!=null ) {
            v = sendPortCell.getValue().trim();
            MrMailbox.setConfig("send_port", v.isEmpty() ? null : v);
        }

        if(sendUserCell!=null) {
            v = sendUserCell.getValue().trim();
            MrMailbox.setConfig("send_user", v.isEmpty() ? null : v);
        }

        if( sendPwCell!=null ) {
            v = sendPwCell.getValue().trim();
            MrMailbox.setConfig("send_pw", v.isEmpty() ? null : v);
        }

        // show dialog
        if( progressDialog!=null ) {
            progressDialog.dismiss();
            progressDialog = null;
        }

        progressDialog = new ProgressDialog(getParentActivity());
        progressDialog.setMessage(LocaleController.getString("ConfiguringAccount", R.string.ConfiguringAccount));
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setCancelable(false);
        progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, LocaleController.getString("Cancel", R.string.Cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                progressDialog = null;
            }
        });
        progressDialog.show();

        synchronized (MrMailbox.m_lastErrorLock) {
            MrMailbox.m_showNextErrorAsToast = false;
        }

        // try to connect
        // (for the future, we may put all this togehter in a single command, that is executed
        // asynchronously by the backend; then we can skip creating a runnable here)
        Utilities.searchQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                MrMailbox.disconnect();
                MrMailbox.configure();
                MrMailbox.connect();
            }
        });
    }

    @Override
    public void didReceivedNotification(int id, Object... args) {
        if (id == NotificationCenter.connectionStateChanged )
        {
            final String errorString;

            synchronized (MrMailbox.m_lastErrorLock) {
                MrMailbox.m_showNextErrorAsToast = true;
                errorString = MrMailbox.m_lastErrorString;
            }

            if( progressDialog!=null ) {
                progressDialog.dismiss();
                progressDialog = null;
            }

            if( (int)args[0]==1 ) {
                if( fromIntro ) {
                    presentFragment(new DialogsActivity(null), true);
                    LaunchActivity la = ((LaunchActivity)getParentActivity());
                    if( la != null ) {
                        la.drawerLayoutContainer.setAllowOpenDrawer(true, false);
                    }
                }
                else {
                    finishFragment();
                }
                NotificationCenter.getInstance().postNotificationName(NotificationCenter.mainUserInfoChanged);
                AndroidUtilities.showDoneHint(ApplicationLoader.applicationContext);
            }
            else {
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                builder.setMessage(errorString);
                builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        ;
                    }
                });
                showDialog(builder.create());
            }
        }
    }

    private boolean isModified(){
        // Warning: the widgets are created as needed and may not be present!
        if( addrCell!=null && addrCell.isModified()) { return true; }
        if( mailPwCell!=null && mailPwCell.isModified()) { return true; }

        if( mailServerCell!=null && mailServerCell.isModified()) { return true; }
        if( mailPortCell!=null && mailPortCell.isModified()) { return true; }
        if( mailUserCell!=null && mailUserCell.isModified()) { return true; }

        if( sendServerCell!=null && sendServerCell.isModified()) { return true; }
        if( sendPortCell!=null && sendPortCell.isModified()) { return true; }
        if( sendUserCell!=null && sendUserCell.isModified()) { return true; }
        if( sendPwCell!=null && sendPwCell.isModified()) { return true; }

        return false;
    }

    @Override
    public void onTransitionAnimationEnd(boolean isOpen, boolean backward) {
        if (isOpen && addrCell!=null) {
            if(addrCell.getValue().isEmpty()) {
                addrCell.getEditTextView().requestFocus();
                AndroidUtilities.showKeyboard(addrCell.getEditTextView());
            }
        }
    }

    private class ListAdapter extends BaseFragmentAdapter {
        private Context mContext;

        public ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int i) {
            return !(i==rowAddrHeadline || i==rowMailPwHeadline || i==rowSectionMail || i==rowBreak2 || i==rowSectionSend || i==rowInfoBelowSendPw);
        }

        @Override
        public int getCount() {
            return rowCount;
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            int type = getItemViewType__(i);
            if (type == ROWTYPE_TEXT_ENTRY) {
                if (i == rowAddr) {
                    if( addrCell==null) {
                        addrCell = new EditTextCell(mContext, false);
                        addrCell.setValueHintAndLabel(MrMailbox.getConfig("addr", ""),
                                "", "", false);
                        addrCell.getEditTextView().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
                    }
                    view = addrCell;
                }
                else if (i == rowMailPw) {
                    if( mailPwCell==null) {
                        mailPwCell = new EditTextCell(mContext, false);
                        mailPwCell.setValueHintAndLabel(MrMailbox.getConfig("mail_pw", ""),
                                "", "", false);
                        mailPwCell.getEditTextView().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    }
                    view = mailPwCell;
                }
                else if (i == rowMailServer) {
                    if( mailServerCell==null) {
                        mailServerCell = new EditTextCell(mContext);
                        mailServerCell.setValueHintAndLabel(MrMailbox.getConfig("mail_server", ""),
                                LocaleController.getString("Automatic", R.string.Automatic), LocaleController.getString("ImapServer", R.string.ImapServer), false);
                    }
                    view = mailServerCell;
                }
                else if (i == rowMailPort) {
                    if( mailPortCell==null) {
                        mailPortCell = new EditTextCell(mContext);
                        mailPortCell.setValueHintAndLabel(MrMailbox.getConfig("mail_port", ""),
                                LocaleController.getString("Default", R.string.Default), LocaleController.getString("ImapPort", R.string.ImapPort), false);
                    }
                    view = mailPortCell;
                }
                else if (i == rowMailUser) {
                    if( mailUserCell==null) {
                        mailUserCell = new EditTextCell(mContext);
                        mailUserCell.setValueHintAndLabel(MrMailbox.getConfig("mail_user", ""),
                                LocaleController.getString("FromAbove", R.string.FromAbove), LocaleController.getString("ImapLoginname", R.string.ImapLoginname), false);
                    }
                    view = mailUserCell;
                }
                else if (i == rowSendServer) {
                    if( sendServerCell==null) {
                        sendServerCell = new EditTextCell(mContext);
                        sendServerCell.setValueHintAndLabel(MrMailbox.getConfig("send_server", ""),
                                LocaleController.getString("Automatic", R.string.Automatic), LocaleController.getString("SmtpServer", R.string.SmtpServer), false);
                    }
                    view = sendServerCell;
                }
                else if (i == rowSendPort) {
                    if( sendPortCell==null) {
                        sendPortCell = new EditTextCell(mContext);
                        sendPortCell.setValueHintAndLabel(MrMailbox.getConfig("send_port", ""),
                                LocaleController.getString("Default", R.string.Default), LocaleController.getString("SmtpPort", R.string.SmtpPort), false);
                    }
                    view = sendPortCell;
                }
                else if (i == rowSendUser) {
                    if( sendUserCell==null) {
                        sendUserCell = new EditTextCell(mContext);
                        sendUserCell.setValueHintAndLabel(MrMailbox.getConfig("send_user", ""),
                                LocaleController.getString("FromAbove", R.string.FromAbove), LocaleController.getString("SmtpLoginname", R.string.SmtpLoginname), false);
                    }
                    view = sendUserCell;
                }
                else if (i == rowSendPw) {
                    if( sendPwCell==null) {
                        sendPwCell = new EditTextCell(mContext);
                        sendPwCell.setValueHintAndLabel(MrMailbox.getConfig("send_pw", ""),
                                LocaleController.getString("FromAbove", R.string.FromAbove), LocaleController.getString("SmtpPassword", R.string.SmtpPassword), false);
                        sendPwCell.getEditTextView().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    }
                    view = sendPwCell;
                }
            }
            else if (type == ROWTYPE_HEADLINE) {
                if (view == null) {
                    view = new HeaderCell(mContext);
                    view.setBackgroundColor(0xffffffff);
                }
                if (i == rowAddrHeadline) {
                    ((HeaderCell) view).setText(ApplicationLoader.applicationContext.getString(R.string.EmailAddress));
                } else if (i == rowMailPwHeadline) {
                    ((HeaderCell) view).setText(ApplicationLoader.applicationContext.getString(R.string.Password));
                } else if (i == rowSectionMail) {
                    ((HeaderCell) view).setText(LocaleController.getString("InboxHeadline", R.string.InboxHeadline));
                } else if (i == rowSectionSend) {
                    ((HeaderCell) view).setText(LocaleController.getString("OutboxHeadline", R.string.OutboxHeadline));
                }
            }
            else if (type == ROWTYPE_SHADOW_BREAK) {
                if (view == null) {
                    view = new ShadowSectionCell(mContext);
                }
            }
            else if (type == ROWTYPE_INFO) {
                if (view == null) {
                    view = new TextInfoCell(mContext);
                }
                if( i== rowOpenAdvOpions) {
                    ((TextInfoCell) view).setText(LocaleController.getString("MyAccoutExplain", R.string.MyAccountExplain),
                            m_expanded? " \u2212" /*minus-sign*/ : "+", m_expanded /*draw bottom border?*/);
                    view.setBackgroundResource(m_expanded? R.drawable.greydivider : R.drawable.greydivider_bottom); // has shadow top+bottom
                }
                else if( i==rowInfoBelowSendPw) {
                    ((TextInfoCell) view).setText(LocaleController.getString("MyAccountExplain2", R.string.MyAccountExplain2));
                    if( m_expanded ) {
                        view.setBackgroundResource(R.drawable.greydivider_bottom);
                    }
                }
            }
            return view;
        }

        @Override
        public int getItemViewType(int i) {
            return IGNORE_ITEM_VIEW_TYPE;
        }

        private int getItemViewType__(int i) {
            if (i == rowAddr || i==rowMailPw || i==rowMailServer || i==rowMailPort|| i==rowMailUser
                     || i==rowSendServer || i==rowSendPort || i==rowSendUser || i== rowSendPw ) {
                return ROWTYPE_TEXT_ENTRY;
            }
            else if( i==rowAddrHeadline || i==rowMailPwHeadline || i==rowSectionMail || i==rowSectionSend ) {
                return ROWTYPE_HEADLINE;
            }
            else if( i== rowBreak2) {
                return ROWTYPE_SHADOW_BREAK;
            }
            return ROWTYPE_INFO;
        }

        @Override
        public int getViewTypeCount() {
            return 1; /* SIC! internally, we ingnore the type, each row has its own type--otherwise text entry stuff would not work */
        }

        @Override
        public boolean isEmpty() {
            return false;
        }
    }
}
