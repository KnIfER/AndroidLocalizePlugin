/*
 * Copyright 2018 Airsaid. https://github.com/airsaid
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ui;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBCheckBox;
import constant.Constants;
import logic.LanguageHelper;
import org.apache.http.util.TextUtils;
import org.jetbrains.annotations.Nullable;
import translate.lang.LANG;
import translate.trans.impl.GoogleTranslator;
import translate.util.Util;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Select the language dialog you want to convert.
 *
 * @author airsaid
 */
public class SelectLanguageDialog extends DialogWrapper {

    private Project mProject;
    private OnClickListener mOnClickListener;
    private List<LANG> mSelectLanguages = new ArrayList<>();

    public interface OnClickListener {
        void onClickListener(List<LANG> selectedLanguage);
    }

    public SelectLanguageDialog(@Nullable Project project) {
        super(project, false);
        this.mProject = project;
        setTitle("Select Convert Languages");
        setResizable(true);
        init();
    }

    @Override
    protected void doOKAction() {
        Util.stopped=false;
        Util.noWrite=false;
        if (mSelectLanguages.size() <= 0) {
            Messages.showErrorDialog("Please select the language you need to translate!", "Error");
            return;
        }
        if (mOnClickListener != null) {
            mOnClickListener.onClickListener(mSelectLanguages);
        }
        super.doOKAction();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return doCreateCenterPanel();
    }

    private JComponent doCreateCenterPanel() {
        final JPanel panel = new JPanel(new BorderLayout(16, 6));
        final Container container = new Container();
        // + top panel
        Container topPanel = new Container();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));
        JTextField etSearch = new JTextField("top message bar");
        Button btnPaste = new Button("Paste");
        Button btnSerialize = new Button("Serialize");
        Button btnInitialize = new Button("Initialize");
        topPanel.add(etSearch);
        topPanel.add(btnPaste);
        topPanel.add(btnSerialize);
        topPanel.add(btnInitialize);
        panel.add(topPanel, BorderLayout.NORTH);

        // sorted languages
        List<LANG> supportLanguages = new GoogleTranslator().getSupportLang();
        supportLanguages.sort(new CountryCodeComparator());
        ItemListener itemListener= e -> {
            String name = ((JBCheckBox)e.getItem()).getName();
            //Log(name);
            int id = Integer.parseInt(name);
            LANG language = supportLanguages.get(id);
            int state = e.getStateChange();
            if (state == ItemEvent.SELECTED) {
                mSelectLanguages.add(language);
            } else {
                mSelectLanguages.remove(language);
            }
        };
        btnPaste.addActionListener(e -> {// so,ml,mi,th,ny,hmn
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            Transferable content = clipboard.getContents(null);
            if (content.isDataFlavorSupported(DataFlavor.stringFlavor)) {//文本数据
                try {
                    String text = (String) content.getTransferData(DataFlavor.stringFlavor);
                    if (!TextUtils.isEmpty(text)) {
                        etSearch.setText(text);
                        btnInitialize.getActionListeners()[0].actionPerformed(null);
                    }
                } catch (Exception ignored) { }
            }
        });
        btnSerialize.addActionListener(e -> {//序列化并复制
            //mSelectLanguages.sort(new CountryCodeComparator());// 有 bug
            List<LANG> _mSelectLanguages = new ArrayList<>(mSelectLanguages.size());
            for (int i = 0; i < supportLanguages.size(); i++) {
                if(((JBCheckBox) container.getComponent(i)).isSelected())
                    _mSelectLanguages.add(supportLanguages.get(i));
            }
            String content = LanguageHelper.getLanguageCodeString(_mSelectLanguages);
            etSearch.setText(content);
            if(content.trim().length()>0){
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                StringSelection selection = new StringSelection(content);
                clipboard.setContents(selection, null);
            }
        });
        btnInitialize.addActionListener(e -> {
            List<String> newSelectedLanuages = LanguageHelper.parseSelectedLanguageCodes(etSearch.getText());
            mSelectLanguages.clear();
            for (int i = 0; i < supportLanguages.size(); i++) {
                JBCheckBox checkBoxLanguage = (JBCheckBox) container.getComponent(i);
                String code = supportLanguages.get(i).getCode();
                checkBoxLanguage.removeItemListener(itemListener);
                checkBoxLanguage.setSelected(false);
                checkBoxLanguage.addItemListener(itemListener);
                if (newSelectedLanuages!=null && newSelectedLanuages.contains(code)) {
                    checkBoxLanguage.setSelected(true);
                }
            }
        });

        // + overwrite existing string
        Container centerpanel = new Container();
        centerpanel.setLayout(new BoxLayout(centerpanel, BoxLayout.X_AXIS));
        panel.add(centerpanel, BorderLayout.CENTER);
        final JBCheckBox overwriteExistingString = new JBCheckBox("Overwrite Existing String");
        centerpanel.add(overwriteExistingString);
        overwriteExistingString.addItemListener(e -> {
            int state = e.getStateChange();
            PropertiesComponent.getInstance(mProject)
                    .setValue(Constants.KEY_IS_OVERWRITE_EXISTING_STRING, state == ItemEvent.SELECTED);
        });
        boolean isOverwriteExistingString = PropertiesComponent.getInstance(mProject)
                .getBoolean(Constants.KEY_IS_OVERWRITE_EXISTING_STRING);
        overwriteExistingString.setSelected(isOverwriteExistingString);
        // + proxy
        JTextField pxSearch = new JTextField("");
        pxSearch.setText("127.0.0.1:10434");
        Button btnProxy = new Button("Proxy");
        centerpanel.add(pxSearch);
        centerpanel.add(btnProxy);
        btnProxy.addActionListener(e -> {
            String[] arr = pxSearch.getText().split(":");
            if(arr.length==2){
                try {
                    Util.hostPort=Integer.parseInt(arr[1]);
                    Util.hostName=arr[0];
                    return;
                } catch (Exception ignored) {  }
            }
            Util.hostName=null;
        });
        // + stop
        Button btnStop = new Button("Stop");
        centerpanel.add(btnStop);
        btnStop.addActionListener(e -> {
            Util.Stop();//yes it's useful
        });
        // + stop2
        Button btnStop2 = new Button("StopNoWrite");
        centerpanel.add(btnStop2);
        btnStop2.addActionListener(e -> {
            Util.StopNoWrite();//yes it's useful
        });
        // + languages
        mSelectLanguages.clear();
        String SelectedLanguageCodes = LanguageHelper.getSelectedLanguageCodes(mProject);
        etSearch.setText(SelectedLanguageCodes);
        List<String> selectedLanguageCodes = LanguageHelper.parseSelectedLanguageCodes(SelectedLanguageCodes);
        container.setLayout(new GridLayout(supportLanguages.size() / 4, 4));
        for (int i = 0; i < supportLanguages.size(); i++) {
            JBCheckBox cblI = new JBCheckBox();
            LANG language = supportLanguages.get(i);
            String code = language.getCode();
            cblI.setText(language.getEnglishName().concat("(").concat(code).concat(")"));
            cblI.setName(""+i);
            container.add(cblI);
            cblI.addItemListener(itemListener);
            if (selectedLanguageCodes != null && selectedLanguageCodes.contains(code)) {
                cblI.setSelected(true);
            }
        }
        panel.add(container, BorderLayout.SOUTH);
        return panel;
    }

    public void setOnClickListener(OnClickListener listener) {
        mOnClickListener = listener;
    }

    static class CountryCodeComparator implements Comparator<LANG> {
        @Override
        public int compare(LANG o1, LANG o2) {
            return o1.getCode().compareTo(o2.getCode());
        }
    }
}
