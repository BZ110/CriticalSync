package org.criticalking.criticalSync;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.ConfigurationSection;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Logger;
import java.util.List;

public final class CriticalSync extends JavaPlugin {

    private Logger log;
    private SQLInstance instance;
    private ArrayList<Table> tables;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        log = getLogger();

        log.info("-----------");
        log.info("CriticalSync - Syncing servers to databases more efficiently.");
        log.info("By: Critical <3");
        log.info("-----------");

        // Use the correct key from config.
        String jdbcString = getConfig().getString("jdbc_string");
        assert jdbcString != null;
        if(jdbcString.equals("DEFAULT")){
            log.warning("It appears that this is your first time running this plugin!");
            log.warning("This plugin must have an SQL JDBC String.");
            log.warning("Go to the config.yml file, and change it accordingly.");
            log.warning("If you need any help, contact criticalking. on discord.");
            Bukkit.getPluginManager().disablePlugin(this);
        } else {
            instance = new SQLInstance(jdbcString, this);
            // Initialize the SQL connection upfront.
            instance.init();
            tables = registerTables();
            for (Table table : tables) {
                if(getConfig().getStringList("exclude_creation").contains(table.getTableName())) continue;
                log.info("Putting Table (" + table.getTableName() + ") inside the database. (If not exists)");
                instance.addTable(table);
            }

            startAsyncTimer();
        }

    }

    public void startAsyncTimer() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, this::updateTables, 0L, 6000L); // 6000 ticks = 5 minutes
    }

    public void updateTables(){
        log.info("Updating all tables!");
        for(Table table : tables) {
            Map<String, String> data = table.getData();
            // USER table branch
            if(table.getTableType().equals("USER")){
                for(Player player : Bukkit.getOnlinePlayers()) {
                    StringBuilder updateSet = new StringBuilder();
                    StringBuilder insertColumns = new StringBuilder();
                    StringBuilder insertValues = new StringBuilder();
                    String whereClause = "";

                    for(Map.Entry<String, String> entry : data.entrySet()) {
                        String columnName = entry.getKey();
                        String placeholder = entry.getValue();
                        String columnType = table.getColumnType(columnName);
                        String parsedValue;

                        if(columnName.equalsIgnoreCase("id")){
                            // Parse the ID for this player.
                            parsedValue = parseWithPAPI(player, placeholder);
                            if(isNumericType(columnType)) {
                                whereClause = " WHERE id = " + parsedValue;
                            } else {
                                whereClause = " WHERE id = '" + parsedValue + "'";
                            }
                            // Also add the ID to the insert columns/values.
                            insertColumns.append(columnName).append(", ");
                            if(isNumericType(columnType)) {
                                insertValues.append(parsedValue).append(", ");
                            } else {
                                insertValues.append("'").append(parsedValue).append("', ");
                            }
                        } else {
                            parsedValue = parseWithPAPI(player, placeholder);
                            // Append to UPDATE clause.
                            if(isNumericType(columnType)) {
                                updateSet.append(columnName).append(" = ").append(parsedValue).append(", ");
                            } else {
                                updateSet.append(columnName).append(" = '").append(parsedValue).append("', ");
                            }
                            // Also include these in the INSERT clause.
                            insertColumns.append(columnName).append(", ");
                            if(isNumericType(columnType)) {
                                insertValues.append(parsedValue).append(", ");
                            } else {
                                insertValues.append("'").append(parsedValue).append("', ");
                            }
                        }
                    }

                    // Remove trailing commas and spaces.
                    if(updateSet.toString().endsWith(", ")) {
                        updateSet.setLength(updateSet.length() - 2);
                    }
                    if(insertColumns.toString().endsWith(", ")) {
                        insertColumns.setLength(insertColumns.length() - 2);
                    }
                    if(insertValues.toString().endsWith(", ")) {
                        insertValues.setLength(insertValues.length() - 2);
                    }

                    // Check if row exists.
                    boolean exists = instance.rowExists(table.getTableName(), whereClause);
                    if(exists) {
                        // Build UPDATE query.
                        String updateQuery = "UPDATE " + table.getTableName() + " SET " + updateSet.toString() + whereClause + ";";
                        instance.executeUpdate(updateQuery);
                    } else {
                        // Build INSERT query.
                        String insertQuery = "INSERT INTO " + table.getTableName() + " (" + insertColumns.toString() +
                                ") VALUES (" + insertValues.toString() + ");";
                        instance.executeUpdate(insertQuery);
                    }
                }
            }
            // GAME table branch
            else if(table.getTableType().equals("GAME")){
                StringBuilder updateSet = new StringBuilder();
                StringBuilder insertColumns = new StringBuilder();
                StringBuilder insertValues = new StringBuilder();
                String whereClause = "";

                for(Map.Entry<String, String> entry : data.entrySet()) {
                    String columnName = entry.getKey();
                    String placeholder = entry.getValue();
                    String columnType = table.getColumnType(columnName);
                    String parsedValue;

                    if(columnName.equalsIgnoreCase("id")){
                        // Parse ID for GAME tables via console context.
                        parsedValue = parseConsole(placeholder);
                        if(isNumericType(columnType)) {
                            whereClause = " WHERE id = " + parsedValue;
                        } else {
                            whereClause = " WHERE id = '" + parsedValue + "'";
                        }
                        insertColumns.append(columnName).append(", ");
                        if(isNumericType(columnType)) {
                            insertValues.append(parsedValue).append(", ");
                        } else {
                            insertValues.append("'").append(parsedValue).append("', ");
                        }
                    } else {
                        parsedValue = parseConsole(placeholder);
                        if(isNumericType(columnType)) {
                            updateSet.append(columnName).append(" = ").append(parsedValue).append(", ");
                        } else {
                            updateSet.append(columnName).append(" = '").append(parsedValue).append("', ");
                        }
                        insertColumns.append(columnName).append(", ");
                        if(isNumericType(columnType)) {
                            insertValues.append(parsedValue).append(", ");
                        } else {
                            insertValues.append("'").append(parsedValue).append("', ");
                        }
                    }
                }

                if(updateSet.toString().endsWith(", ")) {
                    updateSet.setLength(updateSet.length() - 2);
                }
                if(insertColumns.toString().endsWith(", ")) {
                    insertColumns.setLength(insertColumns.length() - 2);
                }
                if(insertValues.toString().endsWith(", ")) {
                    insertValues.setLength(insertValues.length() - 2);
                }

                boolean exists = instance.rowExists(table.getTableName(), whereClause);
                if(exists) {
                    String updateQuery = "UPDATE " + table.getTableName() + " SET " + updateSet.toString() + whereClause + ";";
                    instance.executeUpdate(updateQuery);
                } else {
                    String insertQuery = "INSERT INTO " + table.getTableName() + " (" + insertColumns.toString() +
                            ") VALUES (" + insertValues.toString() + ");";
                    instance.executeUpdate(insertQuery);
                }
            }
        }
    }

    public String parseConsole(String text) {
        return PlaceholderParser.parse(null, text);
    }
    public String parseWithPAPI(Player player, String text) {
        return PlaceholderParser.parse(player, text);
    }

    public ArrayList<Table> registerTables() {
        ArrayList<Table> tables = new ArrayList<>();
        log.info("Registering Tables!");
        ConfigurationSection tablesSection = getConfig().getConfigurationSection("tables");

        if(tablesSection != null) {
            for (String tableName : tablesSection.getKeys(false)) {

                log.info("-----");
                log.info("Table: " + tableName);

                ConfigurationSection tableSection = tablesSection.getConfigurationSection(tableName);
                if (tableSection == null) continue;

                Table table = new Table();
                table.setTableName(tableName);
                // Set the table type from configuration using the table_type_identifier.
                String typeIdentifier = getConfig().getString("table_type_identifier");
                assert typeIdentifier != null;
                if (tableSection.contains(typeIdentifier)) {
                    table.setTableType(tableSection.getString(typeIdentifier));
                } else {
                    table.setTableType("USER"); // default fallback if not specified
                }

                for (String columnName : tableSection.getKeys(false)) {
                    if (columnName.equalsIgnoreCase(getConfig().getString("table_type_identifier"))) continue; // Skip tableType key

                    List<String> columnData = tableSection.getStringList(columnName);
                    table.addColumn(columnName, columnData.get(0), columnData.get(1));
                    log.info("- " + columnName + ": " + columnData.get(0));
                }

                tables.add(table);
            }
        }
        return tables;
    }

    private boolean isNumericType(String columnType) {
        if (columnType == null) return false;
        String type = columnType.toUpperCase();
        return type.contains("INT") || type.contains("DOUBLE") || type.contains("FLOAT") || type.contains("DECIMAL");
    }

    @Override
    public void onDisable() {
        log = getLogger();
        log.info("Shutting down!");
        try {
            if(!(instance.getConnection().isClosed())) instance.close();
        } catch (Exception e) {
            log.severe("Something is wrong, but you don't have to worry about it.");
        }
    }

}
