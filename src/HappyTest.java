import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.components.JBCheckBox;
import constant.Constants;
import logic.LanguageHelper;
import org.apache.http.util.TextUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import translate.lang.LANG;
import translate.trans.impl.GoogleTranslator;
import translate.util.Util;
import ui.SelectLanguageDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


public class HappyTest {
	public static List<LANG> mSelectLanguages = new ArrayList<>();


	static class SelectLanguageDialog extends DialogWrapper {

		private Project mProject;
		private ui.SelectLanguageDialog.OnClickListener mOnClickListener;
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
			// add overwrite existing string
			EditorTextField etSearch = new EditorTextField("asdasd");
			panel.add(etSearch, BorderLayout.NORTH);

			final JBCheckBox overwriteExistingString = new JBCheckBox("Overwrite Existing String");
			panel.add(overwriteExistingString, BorderLayout.CENTER);
			overwriteExistingString.addItemListener(e -> {
				int state = e.getStateChange();
				PropertiesComponent.getInstance(mProject)
						.setValue(Constants.KEY_IS_OVERWRITE_EXISTING_STRING, state == ItemEvent.SELECTED);
			});
			boolean isOverwriteExistingString = PropertiesComponent.getInstance(mProject)
					.getBoolean(Constants.KEY_IS_OVERWRITE_EXISTING_STRING);
			overwriteExistingString.setSelected(isOverwriteExistingString);
			// add language
			mSelectLanguages.clear();
			List<LANG> supportLanguages = new GoogleTranslator().getSupportLang();
			List<String> selectedLanguageCodes = null;//LanguageHelper.getSelectedLanguageCodes(mProject);
			// sort by country code, easy to find
			supportLanguages.sort(new CountryCodeComparator());
			container.setLayout(new GridLayout(supportLanguages.size() / 4, 4));
			for (LANG language : supportLanguages) {
				String code = language.getCode();
				JBCheckBox checkBoxLanguage = new JBCheckBox();
				checkBoxLanguage.setText(language.getEnglishName()
						.concat("(").concat(code).concat(")"));
				container.add(checkBoxLanguage);
				checkBoxLanguage.addItemListener(e -> {
					int state = e.getStateChange();
					if (state == ItemEvent.SELECTED) {
						mSelectLanguages.add(language);
					} else {
						mSelectLanguages.remove(language);
					}
				});
				if (selectedLanguageCodes != null && selectedLanguageCodes.contains(code)) {
					checkBoxLanguage.setSelected(true);
				}
			}
			panel.add(container, BorderLayout.SOUTH);
			return panel;
		}

		public void setOnClickListener(ui.SelectLanguageDialog.OnClickListener listener) {
			mOnClickListener = listener;
		}


	}

	static class CountryCodeComparator implements Comparator<LANG> {
		@Override
		public int compare(LANG o1, LANG o2) {
			return o1.getCode().compareTo(o2.getCode());
		}
	}




	public static void main(String[] args) {

		new AnAction(){
			@Override
			public void actionPerformed( AnActionEvent anActionEvent) {

				SelectLanguageDialog dialog = new SelectLanguageDialog(null);
				dialog.show();
			}
		};


		Project mProject=null;

		final JPanel panel = new JPanel(new BorderLayout(16, 6));
		final Container container = new Container();
		// add overwrite existing string
		Container toppanel = new Container();
		toppanel.setLayout(new BoxLayout(toppanel, BoxLayout.X_AXIS));
		JTextField etSearch = new JTextField("top message bar");
		Button btnPaste = new Button("Paste");
		Button btnSerialize = new Button("Serialize");
		Button btnInitialize = new Button("Initialize");
		toppanel.add(etSearch);
		toppanel.add(btnPaste);
		toppanel.add(btnSerialize);
		toppanel.add(btnInitialize);
		panel.add(toppanel, BorderLayout.NORTH);

		List<LANG> supportLanguages = new GoogleTranslator().getSupportLang();
		ItemListener itemListener= e -> {
			String name = ((JBCheckBox)e.getItem()).getName();
			Log(name);
			int id = Integer.parseInt(name);
			LANG language = supportLanguages.get(id);
			int state = e.getStateChange();
			if (state == ItemEvent.SELECTED) {
				mSelectLanguages.add(language);
			} else {
				mSelectLanguages.remove(language);
			}
		};
		btnPaste.addActionListener(e -> {
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
		//so,ml,mi,th,ny,hmn
		btnSerialize.addActionListener(e -> {//序列化并复制
			List<LANG> _mSelectLanguages = new ArrayList<>(mSelectLanguages.size());
			for (int i = 0; i < supportLanguages.size(); i++) {
				JBCheckBox checkBoxLanguage = (JBCheckBox) container.getComponent(i);
				String code = supportLanguages.get(i).getCode();
				if(checkBoxLanguage.isSelected()){
					_mSelectLanguages.add(supportLanguages.get(i));
				}
			}
			String content = LanguageHelper.getLanguageCodeString(_mSelectLanguages);
			etSearch.setText(content);
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			StringSelection selection = new StringSelection(content);
			clipboard.setContents(selection, null);
		});
		btnInitialize.addActionListener(e -> {
			List<String> newSelectedLanuages = LanguageHelper.parseSelectedLanguageCodes(etSearch.getText());
			mSelectLanguages.clear();
			for (int i = 0; i < supportLanguages.size(); i++) {
				JBCheckBox checkBoxLanguage = (JBCheckBox) container.getComponent(i);
				String code = supportLanguages.get(i).getCode();
				if (newSelectedLanuages!=null && newSelectedLanuages.contains(code)) {
					checkBoxLanguage.setSelected(true);
				}else{
					checkBoxLanguage.removeItemListener(itemListener);
					checkBoxLanguage.setSelected(false);
					checkBoxLanguage.addItemListener(itemListener);
				}
			}
		});

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
		boolean isOverwriteExistingString = false;
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
		// add language
		mSelectLanguages.clear();
		String SelectedLanguageCodes="";
		List<String> selectedLanguageCodes = new ArrayList<>();//LanguageHelper.getSelectedLanguageCodes(mProject);
		etSearch.setText(SelectedLanguageCodes);
		// sort by country code, easy to find
		supportLanguages.sort(new CountryCodeComparator());
		container.setLayout(new GridLayout(supportLanguages.size() / 4, 4));
		for (int i = 0; i < supportLanguages.size(); i++) {
			LANG language = supportLanguages.get(i);
			String code = language.getCode();
			JBCheckBox checkBoxLanguage = new JBCheckBox();
			checkBoxLanguage.setText(language.getEnglishName().concat("(").concat(code).concat(")"));
			checkBoxLanguage.setName(""+i);
			container.add(checkBoxLanguage);
			checkBoxLanguage.addItemListener(itemListener);
			if (selectedLanguageCodes != null && selectedLanguageCodes.contains(code)) {
				checkBoxLanguage.setSelected(true);
			}
		}
		panel.add(container, BorderLayout.SOUTH);

		JFrame mainFrame = new JFrame("第一个程序");

		mainFrame.setSize(500,500);
		mainFrame.add(panel);

		mainFrame.setVisible(true);
	}

	private static void Log(String name) {
		System.out.println(name);
	}
}
