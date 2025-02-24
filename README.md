# CriticalSync

CriticalSync is a Minecraft server plugin designed to easily sync server data to SQL databases. It dynamically creates and updates tables based on a configurable YAML file, allowing you to store both player-specific and game-wide data in your database. The plugin also creates a full SQL dump backup when the server shuts down.

## Features

- **Dynamic Table Creation:**  
  Automatically creates tables based on your configuration file if they do not already exist.

- **Periodic Data Sync:**  
  Every 5 minutes, CriticalSync updates your SQL tables with current data using PlaceholderAPI for dynamic placeholder parsing.

- **Dual Mode Data Handling:**  
  Supports two table types:
  - **USER:** Data for each online player (using their UUID as the identifier).
  - **GAME:** Global server data (using a fixed ID).

- **Automatic Backup:**  
  On server shutdown, the plugin creates an SQL dump file in a `/backups/` directory, ensuring you have a backup of your database.

## Installation

1. **Download and Install CriticalSync:**
   - Place the generated JAR file (preferably the shaded version) into your server's `plugins` folder.
   - Restart or reload your server.

2. **Prerequisites:**
   - Make sure PlaceholderAPI is installed on your server.
   - Ensure MySQL (or MariaDB) and mysqldump are installed and accessible on your system.
   - If using Paper, Spigot, or Bukkit, the necessary server version is required.

## Configuration

The plugin uses a `config.yml` file located in `plugins/CriticalSync/`. Here’s an example configuration:

```yaml
# Go to your pterodactyl database settings.
# 1. Create a new database, call it anything, it doesn't matter.
# 2. Click on the settings icon next to the database you created.
# 3. Copy the JDBC String and paste it here.
#
# You're ready! <3

jdbc_string: "jdbc:mysql://username:password@host:port/database"

# ADVANCED: if you don't know what you're doing, then get someone else who does.
table_type_identifier: "TABLE_TYPE"
tables:

  # EXAMPLE TABLE: user_info
  user_info:
    TABLE_TYPE: "USER"
    id: ["TEXT", "%player_uuid%"]
    name: ["TEXT", "%player_name%"]
    kills: ["INT", "%statistic_player_kills%"]
    deaths: ["INT", "%player_deaths%"]

  # EXAMPLE TABLE: game_info
  game_info:
    TABLE_TYPE: "GAME"
    id: ["INT", "1"]
    online: ["TEXT", "%server_online%/%server_max_players%"]
    uptime: ["TEXT", "%server_uptime%"]
    tps: ["TEXT", "%server_tps%/20"]

# Please don't change this. (<3)
credits:
  author: "Critical <3"
  website: "https://critical.lol/"
  version: 1.0
```

### Explanation of Config Options

- **jdbc_string:**  
  The JDBC connection string to your MySQL database. This must follow the format:  
  `jdbc:mysql://username:password@host:port/database`

- **table_type_identifier:**  
  A key used within each table configuration to denote its type. It should match the key used inside each table (e.g., `TABLE_TYPE`).

- **tables:**  
  Defines the tables that CriticalSync will manage. Each table has:
  - A table type (`USER` or `GAME`).
  - A special `id` column used as the primary identifier.
  - Other columns with their SQL data types and associated PlaceholderAPI strings.

- **credits:**  
  Developer information. Do not modify these values.

## How It Works

1. **On Server Startup:**
   - The plugin saves a default configuration if none exists.
   - It reads your `config.yml` and establishes a connection to the specified SQL database.
   - Tables are registered and created (if they do not already exist) based on your configuration.

2. **Data Synchronization:**
   - Every 5 minutes (using an asynchronous scheduler), CriticalSync updates the database.
   - For USER tables, the plugin loops through every online player and updates their respective rows.
   - For GAME tables, it updates a single row representing global server data.
   - The plugin checks if a row exists for each update; if not, it inserts a new row.

3. **On Server Shutdown:**
   - The plugin creates an SQL dump of your database in the `/backups/` folder. The file is named using the current UNIX timestamp (e.g., `1617891234-backup.sql`).
     
## FYI (For Your Information)

- **Database Connection & Backup:**  
  CriticalSync uses raw JDBC for database operations and the system’s `mysqldump` tool for backups. Ensure your server environment meets these requirements.

- **PlaceholderAPI Dependency:**  
  Make sure PlaceholderAPI is installed; otherwise, placeholder values will not be parsed correctly.

- **Asynchronous Operations:**  
  All database updates are executed asynchronously to avoid blocking the main server thread. However, ensure your database can handle the load.

- **Customization:**  
  You can customize table structures and placeholder strings as needed. The plugin is designed to be as flexible as possible.

- **Support & Feedback:**  
  If you encounter issues or need assistance, feel free to reach out on the developer’s Discord or visit the website provided in the credits.

---

## License

This plugin is provided as-is by Critical <3. Use it at your own risk.

---

Happy syncing!

