package table_dialog;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxViewerCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;

import table_skeleton.TableColumn;
import table_skeleton.TableColumnValue;
import table_skeleton.TableRow;
import xml_catalog_reader.Selection;
import xml_catalog_reader.SelectionList;

public class TableEditor extends EditingSupport {
	
	private TableColumn column;
	private TableView viewer;

	public TableEditor(TableView viewer, TableColumn column) {
		super(viewer.getViewer());
		this.column = column;
		this.viewer = viewer;
	}

	@Override
	protected boolean canEdit(Object arg0) {
		return column.isEditable();
	}
	
	private Selection getEmptySelection() {
		Selection selection = new Selection();
		selection.setCode("");
		selection.setDescription("");
		return selection;
	}

	@Override
	protected CellEditor getCellEditor(Object arg0) {
		
		TableRow row = (TableRow) arg0;
		CellEditor editor = null;
		
		switch(column.getType()) {
		
		case PICKLIST:
			ComboBoxViewerCellEditor combo = new ComboBoxViewerCellEditor(viewer.getTable());
			
			// get the list of possible values for the current column
			// filtering by the summarized information type (bse..)
			SelectionList list = column.getList(row);
			
			// if not mandatory column, add an empty value
			if (!column.isMandatory(row)) {
				Selection emptySel = getEmptySelection();
				if (!list.contains(emptySel))
					list.add(emptySel);
			}
			
			combo.setContentProvider(new ComboBoxContentProvider());
			combo.setLabelProvider(new ComboBoxLabelProvider());
			
			if (list != null)
				combo.setInput(list);
			
			editor = combo;
			break;
			
		case PASSWORD:
			editor = new TextCellEditor(viewer.getTable(), SWT.PASSWORD);
			break;
		default:
			editor = new TextCellEditor(viewer.getTable());
			break;
		}
		
		return editor;
	}

	@Override
	protected Object getValue(Object arg0) {
		
		TableRow row = (TableRow) arg0;
		Object value = null;
		
		switch(column.getType()) {
		
		case PICKLIST:
			value = row.get(column.getId());
			break;
			
		default:
			
			TableColumnValue selection = row.get(column.getId());
			
			if (selection != null)
				value = selection.getLabel();

			if (value == null)
				value = "";
			break;
		}
		
		return value;
	}

	@Override
	protected void setValue(Object arg0, Object value) {
		
		// avoid refreshing if same value
		Object oldValue = getValue(arg0);
		
		if (value == null || (oldValue != null && value.equals(oldValue)))
			return;
		
		TableRow row = (TableRow) arg0;

		switch(column.getType()) {
		
		// signed integer
		case INTEGER:
			
			String newValue = (String) value;
			
			// if change should be done, change
			if (isNumeric(newValue)) {
				row.put(column.getId(), newValue);
			}
			
			break;
			
		// unsigned integer
		case U_INTEGER:
			
			String unsignedInt = (String) value;
			
			// if change should be done, change
			if (isUnsignedNumeric(unsignedInt)) {
				row.put(column.getId(), unsignedInt);
			}
			
			break;
			
		case PICKLIST:
			
			Selection sel = (Selection) value;
			
			TableColumnValue newSelection = new TableColumnValue(sel);
			row.put(column.getId(), newSelection);
			break;
			
		case STRING:
		case PASSWORD:
			String newValue2 = (String) value;
			row.put(column.getId(), newValue2);
			break;
		default:
			break;
		}

		// update the row values
		row.updateFormulas();

		// save the row in the db
		row.save();

		viewer.refresh(row);
	}
	
	/**
	 * Check if numeric input
	 * @param newValue
	 * @return
	 */
	private boolean isNumeric (String newValue) {

		try {
			Integer.parseInt(newValue);
			return true;
		}
		catch (NumberFormatException e) {
			return false;
		}
	}
	
	/**
	 * Check if numeric input
	 * @param newValue
	 * @return
	 */
	private boolean isUnsignedNumeric (String newValue) {

		try {
			int value = Integer.parseInt(newValue);
			return value >= 0;
		}
		catch (NumberFormatException e) {
			return false;
		}
	}
	
	private class ComboBoxContentProvider implements IStructuredContentProvider {

		@Override
		public void dispose() {}

		@Override
		public void inputChanged(Viewer arg0, Object arg1, Object arg2) {}

		@Override
		public Object[] getElements(Object arg0) {
			SelectionList list = (SelectionList) arg0;
			return list.getSelections().toArray();
		}
	}
	
	private class ComboBoxLabelProvider implements ILabelProvider {

		@Override
		public void addListener(ILabelProviderListener arg0) {}

		@Override
		public void dispose() {}

		@Override
		public boolean isLabelProperty(Object arg0, String arg1) {
			return false;
		}

		@Override
		public void removeListener(ILabelProviderListener arg0) {}

		@Override
		public Image getImage(Object arg0) {
			return null;
		}

		@Override
		public String getText(Object arg0) {
			
			Selection selection = (Selection) arg0;
			return selection.getDescription();
		}
	}
}
