# Trinity — Plan & Roadmap

A handoff/status log for the Trinity mod, written so a fresh session (or a future you) can pick up
without the prior conversation. Last updated **2026-10-24** (v0.2.0).

---

## 1. What it is

Trinity is a **NeoForge 1.21.1** Minecraft mod that adds a **nuclear bomb** (`trinity:nuclear_bomb`)
and, since 0.2.0, the **uranium production chain** needed to build one: an ore, ten crafting/smelting
phases, a reusable gas centrifuge, radioactive materials, and an advancement tree that guides the
player. Arming the bomb (flint & steel / fire charge / redstone) starts a short fuse; then a bright
flash, an expanding shockwave that craters and scorches a large area, a towering mushroom cloud, the
death of every living thing in range, and a lingering green radiation cloud.

- **Repo:** https://github.com/kameronbrooks/Trinity (public, branch `main`, MIT)
- **Package / mod id / group:** `com.trinity` / `trinity` / `com.trinity`
- **Location on disk:** `C:\Users\kameron\Documents\Projects\Minecraft Projects\Trinity`
- **MC / NeoForge:** 1.21.1 / 21.1.250, Java 21. `mod_version` 0.2.0.
- Played by the user's family via the Prism modpack in `../family-modpack` (rebuild it after changes so
  clients and the server in `../Server` run identical jars).

## 2. Current status

**v0.1.0 (released):** the bomb and its detonation, all validated in-game by the user.

**v0.2.0 (this version):** the crafting chain. Validated by:
- 5 GameTests (`runGameTestServer`, all pass): centrifuge returned worn by 1, centrifuge breaks at max
  wear, final assembly grid → bomb, HEU smelts in a blast furnace only, both ores drop raw uranium.
- Headless server boot: recipes + 12 advancements load with no parse errors (1411 advancements total).
- Worldgen proof: fresh normal world, seed 1945, 49 chunks → uranium ore in 144 chunk-sections
  (25 stone / 119 deepslate) vs vanilla diamond in 175 — roughly diamond-rare, mostly deepslate.
- Static check: all 19 item/block ids resolve to model → texture → lang.
- Playtested in the dev client by the user (got through Metallurgy) — "excellent".

## 3. How to build / run / test

From `Trinity/`, **JAVA_HOME must point at the Temurin JDK** (the machine default points at Unity's
bundled JDK and breaks gradlew):

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.8.9-hotspot"
.\gradlew.bat compileJava        # fast compile check
.\gradlew.bat build              # jar -> build/libs/trinity-0.2.0.jar
.\gradlew.bat runGameTestServer  # headless GameTests; exits when done ("All N required tests passed")
.\gradlew.bat runClient          # dev client
.\gradlew.bat runServer          # dev dedicated server (needs run/eula.txt=true)
python tools/gen_textures.py [sheet.png]   # regenerate item/ore textures (+ optional preview sheet)
```

**In-game (creative + cheats):** the Trinity creative tab lists every item in production order.
`/give @s trinity:nuclear_bomb` + flint & steel to test the blast; watch from 60–80 blocks.

## 4. Architecture / file map

```
src/main/java/com/trinity/
  Trinity.java                         @Mod entry; registers all DeferredRegisters on the mod bus
  block/
    NuclearBombBlock.java              ARMED state; arm via useItemOn (flint&steel/fire charge) or redstone
    entity/NuclearBombBlockEntity.java fuse countdown (FUSE_TICKS=80), then BlastManager.detonate
  blast/
    BlastShape.java                    precomputed sphere offsets sorted center-outward (shockwave order)
    Blast.java                         THE detonation (destruction, burn map, fluid waking, flash,
                                        mushroom cloud, entity kill, radiation cloud)
    BlastManager.java                  ticks active blasts on ServerTickEvent.Post
  item/
    RadioactiveItem.java               inventoryTick: chance of a Radiation dose, scaled by level 1-3
    GasCentrifugeItem.java             durability 32; crafting remainder = itself, +1 damage (EMPTY at max)
  gametest/TrinityGameTests.java       @GameTestHolder; template "empty" (data/trinity/structure/empty.nbt)
  client/  BigSmokeParticle, TrinityClient (MOD bus), ClientFlash, TrinityHud (GAME bus)
  network/ NukeFlashPayload, TrinityNetwork
  effect/RadiationEffect.java          harmful green effect; periodic trinity:radiation damage, floor 1 HP
  registry/
    TrinityBlocks      nuclear_bomb, uranium_ore, deepslate_uranium_ore (DropExperienceBlock, light 3)
    TrinityItems       every item in production order (the creative tab iterates ITEMS.getEntries())
    TrinityBlockEntities / TrinityEffects / TrinityParticles / TrinityCreativeTabs / TrinityDamageTypes

src/main/resources/
  assets/trinity/  blockstates, models (item/generated + cube_all ores), textures, particles, lang
  data/trinity/
    recipe/                19 recipes (the whole chain; nuclear_bomb.json is now the final assembly)
    advancement/           12-node guide tree; root (find uranium) rewards ALL recipes
    loot_table/blocks/     bomb + both ores (silk touch / raw uranium 1-2 + fortune)
    worldgen/{configured,placed}_feature/ore_uranium.json
    neoforge/biome_modifier/add_uranium_ore.json   adds it to #minecraft:is_overworld, underground_ores
    damage_type/, structure/empty.nbt
  data/minecraft/tags/block/{mineable/pickaxe,needs_iron_tool}.json
  data/c/tags/...          c:ores/uranium, c:raw_materials/uranium (block + item)
tools/gen_textures.py      recolours vanilla textures from the NeoForge resources jar + ASCII pixel art
```

## 5. The production chain (0.2.0)

| # | Phase | Output | Recipe |
|---|---|---|---|
| 1 | Mining | raw_uranium | uranium ore (iron pick+), drops 1-2 + fortune |
| 2 | Milling | yellowcake | smelt/blast raw uranium (or the ore block) |
| 3 | Conversion | uranium_hexafluoride_canister ×2 | ` I `/`YBY`/` I ` (iron, yellowcake, blaze powder) |
| 4 | Enrichment | gas_centrifuge; low_enriched_uranium; highly_enriched_uranium | centrifuge `IDI`/`RHR`/`ICI` (iron blocks, diamond, redstone, hopper, comparator); shapeless centrifuge + 4 UF6 → LEU, centrifuge + 4 LEU → HEU |
| 5 | Metallurgy | enriched_uranium_ingot | **blasting only** HEU |
| 6 | Core | neutron_initiator; fissile_core | `ANA`/`NGN`/`ANA` (amethyst, gold nugget, glowstone); `GUG`/`GNG`/`GUG` (gold, 2 ingots, initiator) |
| 7 | Implosion | explosive_lens ×2; implosion_assembly | ` G `/`PTP`/` N `; 8 lenses around the core |
| 8 | Detonator | detonator_circuit; firing_set | `RQR`/`GCG`/`RPR`; `IDI`/`LBL`/`IDI` (2 circuits, lightning rods, redstone block) |
| 9 | Housing | casing_plate ×2; tail_fin ×2; bomb_housing | `IBI`; `P `/`PP`; `PPP`/`O O`/`PPP` (obsidian) |
| 10 | Assembly | nuclear_bomb | ` S `/` A `/`FHF` |

Cost of one bomb ≈ 32 raw uranium (2 ingots = 2 HEU = 8 LEU = 32 UF6 = 32 yellowcake), ~10 centrifuge uses.

**Radioactivity** (`RadioactiveItem`): every 80 ticks per stack, chance `0.08 × level` of Radiation for
`40 + 30×level` ticks. Levels: raw uranium 1, yellowcake 1, LEU 2, HEU 3, ingot 3, fissile core 3,
implosion assembly 1. UF6 canisters, initiator, and bomb parts are not radioactive. Creative/spectator immune.

## 6. How the blast works

`NuclearBombBlockEntity` fuse hits 0 → removes its block → `BlastManager.detonate` creates a `Blast`,
advanced one slice per `ServerTickEvent.Post`:
1. **age 0:** `initialBurst()` — explosion particles, boom, `sendFlash()`, `killEntities()`, radiation cloud.
2. **destruction:** `BLOCKS_PER_TICK` offsets per tick, center-outward. Crater → air; fluids/leaves →
   air; `scorch()`: wood→coal, sand→green/lime glass, dirt/grass→coarse dirt, stone→blackstone/basalt;
   25% rubble within `rubbleR`. Edits use `UPDATE_CLIENTS | UPDATE_KNOWN_SHAPE` (no neighbour updates,
   physics, or drops).
3. **dig complete:** `igniteSurfaces()`; `wakeFluids()` schedules ticks on boundary fluids so water floods in.
4. **mushroom cloud** until `ANIMATION_TICKS`: rising `big_smoke` stem + expanding cap.

**Flash:** `NukeFlashPayload` (peak scaled by distance) → `ClientFlash` → `TrinityHud` draws a
full-screen white overlay that holds then fades.

## 7. Key decisions & gotchas (READ before editing)

- **Never spawn TNT entities.** The tick-sliced blast is the whole point.
- **Particles need the long-distance flag** (`sendParticles(player, particle, true, ...)`, see
  `Blast.spawn`) or clients cull them beyond 32 blocks.
- **Custom particle wiring** = particle type + MOD-bus client provider + `particles/*.json`.
- **Flash packet handler** (common code) may only touch `ClientFlash`, never a client-only class.
- **Datapacks use SINGULAR folders** (`recipe/`, `loot_table/`, `advancement/`, `tags/block/`), object-form
  ingredients; NeoForge biome modifiers live in `data/<ns>/neoforge/biome_modifier/`.
- **Reusable crafting tool** = override `hasCraftingRemainingItem(ItemStack)` /
  `getCraftingRemainingItem(ItemStack)` (NeoForge stack-aware hooks) — see `GasCentrifugeItem`.
- **Recipe book unlock:** recipes only appear in the book once the root advancement (find uranium)
  fires; they're always craftable manually.
- **Sound events:** some are `Holder<SoundEvent>` needing `.value()`; the compiler tells you.
- **Dev-launch flakes:** early-loading splash crash fixed via `run/config/fml.toml`
  `earlyWindowControl = false`. Write config files without a BOM.
- **Stopping a headless server from a script:** stdin `stop` doesn't make it through `gradlew`; enable
  RCON in `run/server.properties` and send `stop` over RCON (the 0.2.0 ore test did this, then turned
  RCON back off). From Python, launch `gradlew.bat` by **full path** — `cmd` won't find it in cwd here.
- **Can't drive the dev client via computer-use** (raw `java` process). Validate headlessly.
- **`run/` and `build/` are gitignored** (also `.gradle/ repo/ *.log`).

## 8. Tuning knobs

| Knob | Value | Where |
|---|---|---|
| `RADIUS` | 48 | `Blast` (total blast radius) |
| `BLOCKS_PER_TICK` / `ANIMATION_TICKS` / `RISE_PER_TICK` | 3500 / 200 / 1.3 | `Blast` |
| crater / rubble ratio | 0.32·R / 0.60·R (25%) | `Blast` |
| flash range / hold / fade | 260 blocks (full white within 1.3·R) / 6 ticks / ×0.90 | `Blast.sendFlash`, `ClientFlash` |
| `FUSE_TICKS` | 80 | `NuclearBombBlockEntity` |
| ore veins / size / height | 5 per chunk / 4 / trapezoid Y −64..32 | `worldgen/placed_feature/ore_uranium.json` |
| raw uranium per ore | 1-2 + fortune | `loot_table/blocks/*uranium_ore.json` |
| centrifuge durability | 32 | `TrinityItems.GAS_CENTRIFUGE` |
| radioactivity | interval 80t, chance 0.08×level, dose 40+30×level ticks | `RadioactiveItem` |

## 9. Session changelog

- **2026-09-12** — Built the mod: block, fuse, tick-sliced blast, radiation, placeholder textures.
- **2026-09-13** — RADIUS 48, smaller crater, burn variety, water inflow, `big_smoke` particle,
  long-distance particles, packet-driven white-out flash (tuned brighter/longer). Public repo, PLAN.md,
  **v0.1.0 release**.
- **2026-10-24** — **v0.2.0: crafting chain.** Uranium ore (+ deepslate) with worldgen, 16 new items,
  19 recipes replacing the cheap 4-TNT recipe, gas centrifuge as a wearing reusable tool, radioactive
  materials, 12-node advancement guide, `c:` tags, generated textures (`tools/gen_textures.py`), first
  GameTests.

## 10. Roadmap / ideas

**Near-term**
- [x] User playtest of 0.2.0.
- [x] Family modpack + `built-mods/` rebuilt with 0.2.0; **v0.2.0 released**: https://github.com/kameronbrooks/Trinity/releases/tag/v0.2.0
- [ ] Real textures for the bomb block (top/side/armed) and the Radiation effect icon.
- [ ] GitHub topics; custom detonation sound.

**Ideas**
- [ ] Config file for `RADIUS`, fuse, radioactivity, ore rarity.
- [ ] Lead / shielded storage (a lined chest or container) to carry radioactive material safely.
- [ ] A real centrifuge **machine block** (cascade over time) instead of the crafting-grid tool.
- [ ] Fallout over time; blast variants (tactical / bigger yield); defuse or dud mechanic; screen shake.

**Known limitations**
- Detonating inside a big ocean = heavy fluid updates while it refills.
- Relighting tens of thousands of blocks can tax weaker machines; lower `RADIUS`/`BLOCKS_PER_TICK`.
