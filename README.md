# SkyHuntCore

A custom Minecraft Skyblock plugin featuring a unique HeadHunting progression system.

## Features

### Single-Owner Islands
- Each player gets their own island in a dedicated void world
- Customizable island generation using WorldEdit schematics
- Automatic placement with intelligent spiral spacing algorithm
- Safe void protection with automatic teleportation

### Mission System
- Three progressive mission categories: HeadHunting, Mining, and Farming
- 10 island levels with increasing difficulty
- Real-time progress tracking with visual GUIs
- Automatic mission reset on level-up

### HeadHunting Mechanic
- Collect custom-textured mob heads by completing missions
- Unlock heads through progression
- Sell heads for in-game currency
- Bulk selling support for efficient trading

### Economy Integration
- Full Vault support for economy transactions
- Configurable head prices per mob type
- Real-time balance display in scoreboard
- Compatible with all Vault-supported economy plugins

### Real-Time Scoreboard
- Displays island level and mission progress
- Shows player balance with comma formatting
- Configurable layout with custom placeholders
- Clean design with no distracting numbers

### Modern GUI System
- Clean, intuitive inventory-based interfaces
- Island management, mission tracking, and head collection
- Full navigation with back buttons
- Professional white glass pane backgrounds

## Requirements

### Server
- **Minecraft Version:** 1.21.x
- **Server Software:** Paper (recommended) or Spigot
- **Java Version:** 21 or higher

### Required Plugins
- **Vault** - Economy API integration
- **Economy Plugin** - EssentialsX, CMI, or any Vault-compatible economy plugin

### Optional Plugins
- **WorldEdit** or **FastAsyncWorldEdit** - For custom island schematics
- **Multiverse-Core** - Advanced world management

## Installation

1. Install required dependencies (Vault + Economy plugin)
2. Download SkyHuntCore JAR from [Releases](https://github.com/TheCodingLawyer/SkyHuntCore/releases)
3. Place in `plugins/` folder
4. Start server to generate configuration
5. Configure `config.yml` to your preferences
6. Restart server

For detailed installation instructions, see the [Installation Guide](https://github.com/TheCodingLawyer/SkyHuntCore/wiki/Installation).

## Quick Start

### For Players
```
/island create    - Create your island
/island missions  - View mission progress
/island heads     - View unlocked heads
/levelup          - Advance to next level
```

### For Admins
```
/skyhunt reload         - Reload configuration
/skyhunt setlevel       - Set player's island level
/skyhunt givexp         - Give mission progress
/skyhunt unlockhead     - Unlock heads for players
```

## Documentation

Comprehensive documentation is available in the [Wiki](https://github.com/TheCodingLawyer/SkyHuntCore/wiki):

- [Installation Guide](https://github.com/TheCodingLawyer/SkyHuntCore/wiki/Installation) - Step-by-step setup
- [Configuration](https://github.com/TheCodingLawyer/SkyHuntCore/wiki/Configuration) - Detailed config options
- [Commands](https://github.com/TheCodingLawyer/SkyHuntCore/wiki/Commands) - Complete command reference
- [Permissions](https://github.com/TheCodingLawyer/SkyHuntCore/wiki/Permissions) - Permission nodes and setup
- [Dependencies](https://github.com/TheCodingLawyer/SkyHuntCore/wiki/Dependencies) - Required and optional plugins
- [Missions System](https://github.com/TheCodingLawyer/SkyHuntCore/wiki/Missions) - Understanding progression
- [Head System](https://github.com/TheCodingLawyer/SkyHuntCore/wiki/Head-System) - HeadHunting mechanics
- [Island Generation](https://github.com/TheCodingLawyer/SkyHuntCore/wiki/Island-Generation) - Customizing islands
- [Database](https://github.com/TheCodingLawyer/SkyHuntCore/wiki/Database) - Database management
- [Troubleshooting](https://github.com/TheCodingLawyer/SkyHuntCore/wiki/Troubleshooting) - Common issues and solutions

## Configuration Example

```yaml
scoreboard:
  enabled: true
  update-interval: 20
  title: "&b&l✦ SkyHunt ✦"
  layout:
    - "&7┌──────────────"
    - "&fIsland &7» &b{level}"
    - "&fProgress &7» &a{overall}%"
    - "&7"
    - "&d⚔ &fHeads &7» &e{headhunting}%"
    - "&6⛏ &fMining &7» &e{mining}%"
    - "&a☘ &fFarming &7» &e{farming}%"
    - "&7"
    - "&f$ &a{balance}"
    - "&7└──────────────"

island:
  world-name: "skyhunt"
  spacing: 1000
  height: 64
  starting-level: 1
  max-level: 10

head-prices:
  PIG: 50.0
  COW: 75.0
  ZOMBIE: 100.0
  ENDERMAN: 500.0
```

## Building from Source

### Prerequisites
- Java 21 JDK
- Gradle 8.13 or higher

### Build Steps
```bash
git clone https://github.com/TheCodingLawyer/SkyHuntCore.git
cd SkyHuntCore
./gradlew clean shadowJar
```

Built JAR will be in `build/libs/SkyHuntCore-1.0.0.jar`

## Support

### Getting Help
1. Check the [Wiki](https://github.com/TheCodingLawyer/SkyHuntCore/wiki) for documentation
2. Review [Troubleshooting](https://github.com/TheCodingLawyer/SkyHuntCore/wiki/Troubleshooting) for common issues
3. Ensure all [Dependencies](https://github.com/TheCodingLawyer/SkyHuntCore/wiki/Dependencies) are installed

### Reporting Issues
When reporting issues, please include:
- Minecraft version
- Server software (Paper/Spigot) and version
- SkyHuntCore version
- Installed plugins
- Full error log from console
- Steps to reproduce

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Credits

### Dependencies
- [Vault](https://github.com/MilkBowl/Vault) - Economy API
- [WorldEdit](https://github.com/EngineHub/WorldEdit) - Schematic support
- [Paper](https://papermc.io/) - Server software

### Development
Created by KenjiStudios for Minecraft 1.21.x

---

**Version:** 1.0.0  
**Minecraft Compatibility:** 1.21.x  
**Last Updated:** December 2025

