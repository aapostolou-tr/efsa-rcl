package table_skeleton;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import app_config.AppPaths;
import app_config.BooleanValue;
import duplicates_detector.Checkable;
import formula.Formula;
import formula.FormulaException;
import formula.FormulaSolver;
import report.Report;
import table_database.TableDao;
import xlsx_reader.TableHeaders.XlsxHeader;
import xlsx_reader.TableSchema;
import xml_catalog_reader.Selection;
import xml_catalog_reader.SelectionList;
import xml_catalog_reader.XmlContents;
import xml_catalog_reader.XmlLoader;

/**
 * Generic element of a {@link Report}.
 * @author avonva
 *
 */
public class TableRow implements Checkable {
	
	private static final Logger LOGGER = LogManager.getLogger(TableRow.class);
	
	public enum RowStatus {
		OK,
		MANDATORY_MISSING,
		ERROR
	};

	private HashMap<String, TableCell> values;
	private TableSchema schema;
	
	/**
	 * Careful use
	 */
	public TableRow() {
		this.values = new HashMap<>();
	}
	
	/**
	 * Careful use
	 */
	public void setSchema(TableSchema schema) {
		this.schema = schema;
	}
	
	/**
	 * Careful use
	 */
	public TableRow(HashMap<String, TableCell> values, TableSchema schema) {
		this.schema = schema;
		this.values = values;
	}
	
	/**
	 * Create a report row
	 * @param schema columns properties of the row
	 */
	public TableRow(TableSchema schema) {
		this.values = new HashMap<>();
		this.schema = schema;
	}
	
	/**
	 * Create a clone of a row
	 * @param row
	 */
	public TableRow(TableRow row) {
		this.values = new HashMap<>(row.values);
		this.schema = row.getSchema();
	}
	
	/**
	 * Initialize a row with an element already inserted in it
	 * @param schema
	 * @param initialColumnId
	 * @param initialValue
	 */
	public TableRow(TableSchema schema, String initialColumnId, TableCell initialValue) {
		this(schema);
		this.put(initialColumnId, initialValue);
	}
	
	/**
	 * Set the database id
	 * @param id
	 */
	public void setId(int id) {
		
		String index = String.valueOf(id);
		TableCell idValue = new TableCell();
		idValue.setCode(index);
		idValue.setLabel(index);
		
		// put directly into the values and not with this.put
		// to avoid to put the id into the changes hashmap
		this.values.put(schema.getTableIdField(), idValue);
	}
	
	/**
	 * Get the database id if present
	 * otherwise return -1
	 * @return
	 */
	public int getDatabaseId() {
		
		int id = -1;
		
		try {
			
			if (schema == null)
				return id;
			
			String idField = schema.getTableIdField();
			
			if (idField == null)
				return id;
			
			TableCell value = this.values.get(idField);
			
			if (value != null && value.getCode() != null)
				id = Integer.valueOf(value.getCode());
			
		} catch (NumberFormatException e) {}
		
		return id;
	}
	
	/**
	 * Get the parent of this table row
	 * @param parentSchema schema of the parent (also identifies the type of
	 * parent required)
	 * @return
	 */
	public TableRow getParent(TableSchema parentSchema) {
		
		// open the child dao
		TableDao dao = new TableDao();

		// get parent using the id contained in the row
		TableRow parent = dao.getById(parentSchema, this.getNumLabel(parentSchema.getTableIdField()));
		
		return parent;
	}
	
	public Collection<TableRow> getChildren(TableSchema childSchema) {
		return getChildren(childSchema, true);
	}
	
	/**
	 * Get the rows defined in the child table that are related to
	 * this parent row.
	 * @param childSchema the schema of the child table
	 * @return
	 */
	public Collection<TableRow> getChildren(TableSchema childSchema, boolean solveFormulas) {
		
		// open the child dao
		TableDao dao = new TableDao();
		
		// get parent table name using the relation
		String parentTable = this.getSchema().getSheetName();
		
		// get the rows of the children related to the parent
		Collection<TableRow> children = dao.getByParentId(childSchema, parentTable, 
				this.getDatabaseId(), solveFormulas);
		
		return children;
	}
	
	
	/**
	 * Get a string variable value from the data
	 * @param key
	 * @return
	 */
	public TableCell get(String key) {
		return values.get(key);
	}
	
	/**
	 * Get the label of an element of the row
	 * @param row
	 * @param field
	 * @return
	 */
	public String getLabel(String field) {
		return getField(field, true);
	}
	
	/**
	 * Get the label in numeric format
	 * @param field
	 * @return
	 */
	public int getNumLabel(String field) {
		return getNumField(field, true);
	}
	
	public int getNumCode(String field) {
		return getNumField(field, false);
	}
	
	public int getNumField(String field, boolean label) {
		
		String value = getField(field, label);
		
		int numValue = Integer.MIN_VALUE;
		try {
			numValue = Integer.valueOf(value);
		}
		catch (NumberFormatException e) {
			LOGGER.error("Cannot get number for value:" + value, e);
		}
		
		return numValue;
	}
	
	/**
	 * Get the code of an element of the row
	 * @param row
	 * @param field
	 * @return
	 */
	public String getCode(String field) {
		return getField(field, false);
	}
	
	private String getField(String field, boolean label) {
		
		TableCell value = this.get(field);
		
		if (value == null || value.isEmpty())
			return "";
		
		if (label)
			return value.getLabel();
		else
			return value.getCode();
	}
	
	public void setChildrenError() {
		this.put(AppPaths.CHILDREN_CONTAIN_ERRORS_COL, BooleanValue.getTrueValue());
	}
	public void removeChildrenError() {
		this.put(AppPaths.CHILDREN_CONTAIN_ERRORS_COL, BooleanValue.getFalseValue());
	}
	public boolean hasChildrenError() {
		TableCell value = this.get(AppPaths.CHILDREN_CONTAIN_ERRORS_COL);
		return value != null && value.getCode() != null && BooleanValue.isTrue(value.getCode());
	}
	
	/**
	 * Put a selection into the data
	 * @param key
	 * @param value
	 */
	public void put(String key, TableCell value) {
		values.put(key, value);
	}
	
	/**
	 * Put a string into the data, only for raw columns not picklists
	 * If it is a picklist please provide instead of the label the code
	 * and the label will be automatically retrieved
	 * @param key
	 * @param value
	 */
	public void put(String key, String value) {
		
		TableCell row;
		
		if (schema != null && schema.getById(key) != null && schema.getById(key).isPicklist()) {
			
			String picklist = schema.getById(key).getPicklistKey();
			String picklistFilter = schema.getById(key).getPicklistFilter(this);
			XmlContents contents = XmlLoader.getByPicklistKey(picklist);
			
			if (contents == null) {
				LOGGER.warn("Cannot retrieve the label of " + value + " for the picklist " + picklist);
				return;
			}
			
			SelectionList list = null;
			if (picklistFilter != null) {
				list = contents.getListById(picklistFilter);
			}

			Selection selection = null;
			
			// use list if possible
			if (list != null)
				selection = list.getSelectionByCode(value);
			else
				selection = contents.getElementByCode(value);
			
			if (selection == null) {

				// track where the method was called
				try {
					
					String error = "Cannot find the selection " + value 
							+ " in the picklist " + picklist + ". Using empty value instead.";
					
					if (picklistFilter != null) {
						error = error + "The search was performed with filter=" + picklistFilter; 
					}
					throw new IOException(error);
				}
				catch (IOException e) {
					e.printStackTrace();
					LOGGER.error("Cannot put key=" + key + ", value=" + value + " in row", e);
				}
				
				row = new TableCell();
			}
			else {
				row = new TableCell(selection);
			}
		}
		else {
			row = new TableCell();
			row.setCode(value);
			row.setLabel(value);
		}

		this.put(key, row);
	}
	
	/**
	 * Remove a value from the row
	 * @param key
	 */
	public void remove(String key) {
		this.values.remove(key);
	}
	
	public void initialize(String colId) {
		
		TableColumn col = schema.getById(colId);
		
		// skip foreign keys
		if (col.isForeignKey())
			return;
		
		TableCell sel = new TableCell();
		FormulaSolver solver = new FormulaSolver(this);

		try {
			Formula label = solver.solve(col, XlsxHeader.DEFAULT_VALUE.getHeaderName());
			sel.setLabel(label.getSolvedFormula());
		} catch (FormulaException e) {
			e.printStackTrace();
			LOGGER.error("Cannot solve formula for column=" + colId, e);
		}

		try {
			Formula code = solver.solve(col, XlsxHeader.DEFAULT_CODE.getHeaderName());
			
			if (col.getPicklistKey() == null || col.getPicklistKey().isEmpty())
				sel.setCode(code.getSolvedFormula());
			else
				sel = getTableColumnValue(code.getSolvedFormula(), col.getPicklistKey());
			
		} catch (FormulaException e) {
			e.printStackTrace();
			LOGGER.error("Cannot solve formula for column=" + colId, e);
		}

		this.put(col.getId(), sel);
	}
	
	/**
	 * Initialize the row with the default values
	 * note that this will override all the values of the row
	 * with their default values!
	 */
	public void initialize() {
		
		// create a slot for each column of the table
		for (TableColumn col : schema) {
			initialize(col.getId());
		}
	}

	/**
	 * Update the code or the value of the row
	 * using a solved formula
	 * @param f
	 * @param fieldHeader
	 */
	public void update(Formula f, String fieldHeader) {
		
		// skip editable columns
		if (f.getColumn().isEditable(this))
			return;
		
		XlsxHeader h = XlsxHeader.fromString(fieldHeader);
		
		if (h == null)
			return;
		
		TableCell colVal = this.get(f.getColumn().getId());
		
		// if no value found, initialize
		if (colVal == null)
			colVal = new TableCell();

		if (h == XlsxHeader.CODE_FORMULA && !f.getSolvedFormula().isEmpty()) {
			colVal.setCode(f.getSolvedFormula());
		}
		else if (h == XlsxHeader.LABEL_FORMULA && !f.getSolvedFormula().isEmpty()) {
			colVal.setLabel(f.getSolvedFormula());
			
			// if the field has not a code formula and it is not a pick-list, then
			// update the code using the label
			if (!f.getColumn().isPicklist() && f.getColumn().getCodeFormula().isEmpty())
				colVal.setCode(f.getSolvedFormula());
		}
		else // else do nothing
			return;

		this.put(f.getColumn().getId(), colVal);
	}
	
	/**
	 * Update the values of the rows applying the columns formulas
	 * (Compute all the automatic values)
	 */
	public void updateFormulas() {

		// solve the formula for default code and default value
		FormulaSolver solver = new FormulaSolver(this);
		
		// note that this automatically updates the row
		// while solving formulas
		try {
			solver.solveAll(XlsxHeader.CODE_FORMULA.getHeaderName());
		} catch (FormulaException e) {
			e.printStackTrace();
			LOGGER.error("Cannot solve row formulas", e);
		}
		
		try {
			solver.solveAll(XlsxHeader.LABEL_FORMULA.getHeaderName());
		} catch (FormulaException e) {
			e.printStackTrace();
			LOGGER.error("Cannot solve row formulas", e);
		}
	}
	
	/**
	 * Save the current row into the database
	 * (this is an insert operation! Multiple calls
	 * create multiple rows). Return the new id
	 * of the database
	 */
	public int save() {
		
		TableDao dao = new TableDao();
		int id = dao.add(this);
		this.setId(id);
		
		return id;
	}
	
	/**
	 * Update the row in the database
	 */
	public void update() {
		TableDao dao = new TableDao();
		dao.update(this);
	}
	
	/**
	 * Delete permanently the row from the database
	 */
	public void delete() {
		TableDao dao = new TableDao();
		dao.delete(this.schema, this.getDatabaseId());
	}

	/**
	 * Get the schema of the row
	 * @return
	 */
	public TableSchema getSchema() {
		return schema;
	}
	
	/**
	 * Get the status of the row
	 * @return
	 */
	public RowStatus getRowStatus() {
		
		RowStatus status = RowStatus.OK;
		
		if (!areMandatoryFilled())
			status = RowStatus.MANDATORY_MISSING;
		
		return status;
	}
	
	/**
	 * 
	 * @param code
	 * @param picklistKey
	 * @return
	 */
	public TableCell getTableColumnValue(String code, String picklistKey) {
		
		XmlContents picklist = XmlLoader.getByPicklistKey(picklistKey);
		
		if (picklist == null) {
			LOGGER.warn("Cannot pick the value " + code + " from list " + picklistKey 
					+ ". The picklist does not exist.");
			return new TableCell();
		}
		
		Selection sel = picklist.getElementByCode(code);
		
		if (sel == null) {
			
			if (code != null && !code.trim().isEmpty()) {
				LOGGER.warn("Cannot pick the value " + code + " from list " + picklistKey 
						+ ". Either the list or the element do not exist. Empty element returned instead.");
			}
			
			return new TableCell();
		}
		
		return new TableCell(sel);
	}
	
	/**
	 * Check if all the mandatory fields are filled
	 * @return
	 */
	public boolean areMandatoryFilled() {
		return getMandatoryFieldNotFilled().isEmpty();
	}
	
	/**
	 * Get all the mandatory fields that are not filled
	 * @return
	 */
	public Collection<TableColumn> getMandatoryFieldNotFilled() {
		
		Collection<TableColumn> notFilled = new ArrayList<>();
		
		for (TableColumn column : schema) {
			
			if (column.isMandatory(this)) {
				
				TableCell value = this.get(column.getId());
				
				if (value == null || value.isEmpty())
					notFilled.add(column);
			}
		}
		
		return notFilled;
	}
	
	/**
	 * Check if equal
	 */
	public boolean sameAs(Object arg0) {
		
		if (!(arg0 instanceof TableRow))
			return false;
		
		TableRow other = (TableRow) arg0;
		
		// cannot compare rows with different schema
		if (!this.schema.equals(other.schema))
			return false;
		
		// for each column of the row
		for (String key : this.values.keySet()) {
			
			// skip the id of the table since we do
			// not have a column for that in the schema
			if (key.equals(schema.getTableIdField()))
				continue;
			
			// get the current column object
			TableColumn col = this.schema.getById(key);
			
			// continue searching if we have not a natural key field
			if (!col.isNaturalKey())
				continue;
			
			// here we are comparing a part of the natural key
			TableCell value1 = this.get(key);
			TableCell value2 = other.get(key);
			
			// cannot compare two empty values (it would return
			// equal but actually they simply have a missing value)
			if (value1.isEmpty() && value2.isEmpty())
				continue;
			
			// if a field of the natural key is
			// different then the two rows are different
			if (!value1.equals(value2))
				return false;
		}
		
		// if we have arrived here, all the natural
		// keys are equal, therefore we have the same row
		return true;
	}
	
	/**
	 * Convert the row into an xml
	 * @return
	 */
	public String toXml(boolean addRoot) {
		
		StringBuilder sb = new StringBuilder();
		
		for (TableColumn column : schema) {
			
			String rowValue = this.getCode(column.getId());

			// skip non output columns and non mandatory fields which are empty
			if (!column.isPutInOutput(this) || (!column.isMandatory(this) && rowValue.isEmpty()))
				continue;

			TableCell value = this.get(column.getId());
			
			if (value == null)
				continue;
			
			String node = getXmlNode(column.getXmlTag(), value.getCode());

			// write the node
			sb.append(node);
		}
		
		// if add root return the wrapped version
		if (addRoot) {
			return getXmlNode("result", sb.toString());
		}
		
		return sb.toString();
	}
	
	/**
	 * Create a single xml node with the text content
	 * @param nodeName
	 * @param textContent
	 * @return
	 */
	private String getXmlNode(String nodeName, String textContent) {
		
		StringBuilder node = new StringBuilder();
		
		// create the node
		node.append("<")
			.append(nodeName)
			.append(">")
			.append(textContent)
			.append("</")
			.append(nodeName)
			.append(">");
		
		return node.toString();
	}
	
	/**
	 * get a row with only the visible fields in it
	 * (it is lighter for visualization purposes)
	 * @return
	 */
	public TableRow getVisibleFields() {
		
		// copy all the values of the row
		TableRow row = new TableRow(this);
		
		for (TableColumn col: schema) {
			
			// remove invisible fields (not fk and id)
			if (!col.isVisible(row) && !col.isForeignKey() 
					&& !col.getId().equals(AppPaths.CHILDREN_CONTAIN_ERRORS_COL)) {
				row.values.remove(col.getId());
			}
		}
		
		return row;
	}
	
	/**
	 * Update the values using another row object
	 * (only the values contained in the row are updated,
	 * the other ones are untouched)
	 * @param row
	 */
	public void copyValues(TableRow row) {
		for (String key: row.values.keySet()) {
			
			TableCell cell = row.get(key);
			TableCell copy = new TableCell(cell.getCode(), cell.getLabel());
			
			this.put(key, copy);
		}
	}
	
	@Override
	public boolean equals(Object arg0) {
		
		if (arg0 instanceof TableRow) {
			
			TableRow other = (TableRow) arg0;
			boolean sameSchema = this.schema.equals(other.schema);
			boolean sameId = this.getDatabaseId() == other.getDatabaseId();
			return sameSchema && sameId;
		}
		else 
			return super.equals(arg0);
	}
	
	@Override
	public String toString() {
		
		StringBuilder print = new StringBuilder();
		
		print.append("ID: " + getDatabaseId() + "\n");
		
		for (String key : this.values.keySet()) {
			
			print.append("Column: " + key);

			if (values.get(key) != null) {
				print.append(" code=" + values.get(key).getCode());
				print.append(";value=" + values.get(key).getLabel());
				print.append("\n");
			}
		}
		return print.toString();
	}
}
