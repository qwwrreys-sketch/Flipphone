# Flip Phone (Fabric 1.20.1)

A Japanese flip phone (Docomo style) for roleplay servers, rebuilt from the reference video:
the phone slides up on the right side of the screen (upper half = LCD, lower half = d-pad and number keys).

It is a **separate, standalone mod** (`flipphone`). It does not touch Server Ops or any other mod.

## What is inside

| App | What it does |
|---|---|
| **Home** | clock + date (from the Minecraft world clock, RP calendar), flowers, MENU button |
| **MY PHONE** | your permanent number, yen balance, contacts / messages sent / calls / call time, messages received |
| **LINE** | list of conversations, live chat (type, send, receive in real time), date separators |
| **CONTACTS** | saved contacts with the popup menu: Call / Message / Rename / Delete |
| **DIAL / CALLS** | dial a number and ring another player (they can answer, deny or miss it), recent-calls list |
| **YENAPP** | create a permanent `¥tag` (1-10 chars), search a tag, green **Pay** button, payment screen (10 - 500,000 ¥) |
| **SOCIALLINK** | finds players within 5 blocks who also have SocialLink open; select a number -> **Add Contact** -> nickname |
| **GAMES / SNAKE** | classic snake on a 15x15 grid: levels, speed-up, red game-over wipe, best score saved |

The apps that only appear as icons in the video (MIXI, JAPANIFY, CAMERA, INDEED, DECO, DEMAE, MAPS, TAXI, SPORTS and the other games)
are **not** included, because the video never shows their screens. YenApp and SocialLink screens are not in the video either:
they were designed in the same style following the written rules.

## Phone numbers

* Every player gets a random permanent number `090-XXXX-XXXX`, different for everyone, stored in `world/flipphone/phones.json`.
* The username **BaXzuXka** (exact capitals) always gets **555-5555-5555**. Change it in `PhoneService.java` (`VIP_USERNAME`, `VIP_NUMBER`).
  On an offline-mode (cracked) server anybody can join with that name, so protect the name (whitelist).

## Controls

| Key | Action |
|---|---|
| **Left Alt** (rebindable: Controls > Flip Phone) or right-click the **Flip Phone** item | open / close |
| Arrow keys / mouse wheel | navigate |
| **Enter** | OK (centre key) |
| **Backspace** | back, or delete a character while typing |
| **M** | app menu |
| **0-9** | dial (works from the home screen) |
| **Tab** or **Delete** | right soft key (NEW / MORE / DEL / ...) |
| Esc | close the phone |

Everything can also be clicked: the LCD, the soft-key labels and the physical keys under the screen.
While the phone is open it behaves like a normal Minecraft screen (the world stays visible, the mouse is free, you do not walk).

The **Flip Phone** item is in the creative inventory (Tools & Utilities) or `/give @s flipphone:phone`.
In `PhoneConfig.java` you can make the keybind work only while the item is in your hand (`KEYBIND_NEEDS_ITEM`).

## Yen (how players get money)

The mod has no job system. Yen is given by the server owner / console / your own store or job plug-in with commands
(operator level 4 or console):

```
/phone yen give  <player> <amount>
/phone yen take  <player> <amount>
/phone yen set   <player> <amount>
/phone yen check <player>
/phone number    <player>
```

Players move money with YenApp. The server checks every payment (own tag, target exists, not yourself, 10 - 500,000, enough balance)
and moves the money in one step, then saves immediately.

## Build and install

### With GitHub (like your other mod)
1. Create a new repository and upload everything from this folder (the hidden `.github` folder too).
   If the web upload skips `.github`, create `.github/workflows/build.yml` by hand with the content of the file in this zip.
2. Open the **Actions** tab, wait for "Build mod", download the artifact `flipphone-fabric-1.20.1` (a `.jar`).

### Locally
```
gradle build          # (or ./gradlew build if you have the wrapper jar)
```
The jar is in `build/libs/flipphone-1.0.0.jar`.

### Install
Put the jar into the `mods` folder of **the server AND every player** (it has screens, so clients need it). Requires Fabric Loader 0.15+, Fabric API, Minecraft 1.20.1, Java 17.

## Settings

| What | Where |
|---|---|
| VIP username / number | `server/PhoneService.java` |
| Pay limits (10 / 500,000), tag length (1-10), SocialLink range (5 blocks), message length | `net/PhoneNet.java` |
| RP calendar on the home screen (start year / month, days per month, loop) | `PhoneConfig.java` |
| Keybind rule | `PhoneConfig.java` |
| Colours of the phone | `client/ui/PhoneTheme.java` |
| Screen layout in units of the video (the whole phone = 312 x 828) | `client/ui/PhoneScreen.java` |

## Data
`<world>/flipphone/phones.json`: numbers, yen, tags, contacts, chats (last 300 messages per chat), call log (last 30).
Economy changes are written immediately; the rest every 30 seconds and on shutdown.

## Quick test (2 players)
1. Both join: each has a different `090-` number (MY PHONE). BaXzuXka has 555-5555-5555.
2. Both open SOCIALLINK and stand within 5 blocks: the other number appears under "Searching..." -> select -> Add Contact -> nickname.
3. LINE: write to each other. DIAL: call, answer, hang up -> numbers on MY PHONE change.
4. `/phone yen give <player> 1000`, YENAPP: create tags, search the other tag, Pay.
