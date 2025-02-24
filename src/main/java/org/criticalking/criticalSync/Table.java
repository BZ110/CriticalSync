package org.criticalking.criticalSync;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Table {
    private ArrayList<ArrayList<String>> columns = new ArrayList<>();
    private String tableName;
    private String tableType;
    private Map<String, String> data = new HashMap<>();

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public void setTableType(String tableType) {
        this.tableType = tableType;
    }

    public void addColumn(String columnName, String columnType, String data){
        ArrayList<String> addr = new ArrayList<>();
        addr.add(columnName);
        addr.add(columnType);
        this.data.put(columnName, data);
        columns.add(addr);
    }

    public String getInitCommand() {
        String initCommand = "CREATE TABLE IF NOT EXISTS " + tableName + " (\n";

        // But why would you do this?
        if(columns.isEmpty()) {
            return "CREATE TABLE IF NOT EXISTS " + tableName + "();";
        }

        for(int i = 0; i < columns.size(); i++){
            ArrayList<String> addr = columns.get(i);
            String toConcat = addr.get(0) + " " + addr.get(1) + ", ";
            initCommand = initCommand.concat(toConcat);
        }
        return initCommand.substring(0, initCommand.length() - 2).concat(");");
    }

    public Map<String, String> getData() {
        return data;
    }

    public String getTableName() {
        return tableName;
    }

    public String getTableType() {
        return tableType;
    }

    public String getColumnType(String columnName) {
        for(ArrayList<String> col : this.columns) {
            if(col.get(0).equalsIgnoreCase(columnName)) {
                return col.get(1);
            }
        }
        return null;
    }


}
