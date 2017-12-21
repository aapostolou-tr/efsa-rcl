package global_utils;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import acknowledge.OpResError;
import app_config.PropertiesReader;
import i18n_messages.Messages;
import webservice.MySOAPException;
import webservice.SOAPError;

public class Warnings {

	/**
	 * Warn the user with a message box with custom style
	 * @param title
	 * @param message
	 * @param icon
	 */
	public static int warnUser(Shell shell, String title, String message, int style) {
		MessageBox mb = new MessageBox(shell, style);
		mb.setText(title);
		mb.setMessage(message);
		return mb.open();
	}
	
	/**
	 * Warn the user with an ERROR message box
	 * @param title
	 * @param message
	 */
	public static int warnUser(Shell shell, String title, String message) {
		return warnUser(shell, title, message, SWT.ICON_ERROR);
	}
	
	public static String getStackTrace(Exception e) {
		
		String message = e.getMessage();
		
		StringBuilder sb = new StringBuilder();
		for (StackTraceElement ste : e.getStackTrace()) {
	        sb.append("\n\tat ");
	        sb.append(ste);
	    }
	    String trace = message + " " + sb.toString();
	    
	    return trace;
	}
	
	public static String[] getAckOperationWarning(OpResError error) {

		String title = Messages.get("error.title");
		String message = null;
		
		switch(error) {
		case NOT_EXISTING_DC:
			message = Messages.get("dc.not.valid", 
					PropertiesReader.getDataCollectionCode(),
					PropertiesReader.getSupportEmail());
			break;
		case USER_NOT_AUTHORIZED:
			message = Messages.get("account.unauthorized", 
					PropertiesReader.getDataCollectionCode(),
					PropertiesReader.getSupportEmail());
			break;
		default:
			message = Messages.get("ack.general.error", 
					error.toString(),
					PropertiesReader.getSupportEmail());
			break;
		}
			
		return new String[] {title, message};
	}
	
	public static String[] getSOAPWarning(MySOAPException e) {
		
		String title = null;
		String message = null;
		SOAPError error = e.getError();
		switch(error) {
		case NO_CONNECTION:
			title = Messages.get("error.title");
			String trace = Warnings.getStackTrace(e);
			message = Messages.get("no.connection", trace);
			break;
		case UNAUTHORIZED:
		case FORBIDDEN:
			title = Messages.get("error.title");
			message = Messages.get("wrong.credentials");
			break;
		}
		
		return new String[] {title, message};
	}
	
	
	public static void showSOAPWarning(Shell shell, MySOAPException error) {
		String[] warning = Warnings.getSOAPWarning(error);
		Warnings.warnUser(shell, warning[0], warning[1]);
	}
}
