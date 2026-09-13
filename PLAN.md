# Trinity — Plan & Roadmap

A handoff/status log for the Trinity mod, written so a fresh session (or a future you) can pick up
without the prior conversation. Last updated **2026-09-13**.

---

## 1. What it is

Trinity is a **NeoForge 1.21.1** Minecraft mod that adds **one block: a nuclear bomb**
(`trinity:nuclear_bomb`). Arming it (flint & steel / fire charge / redstone) starts a short fuse,
then it detonates: a bright flash, an expanding shockwave that craters and scorches a large area, a
towering mushroom cloud, the death of every living thing in range, and a lingering green radiation
cloud. Water flows back into the crater afterward.

- **Repo:** https://github.com/kameronbrooks/Trinity (public, branch `main`, MIT)
- **Package / mod id / group:** `com.trinity` / `trinity` / `com.trinity`
- **Location on disk:** `C:\Users\kameron\Documents\Projects\Minecraft Projects\Trinity`
- **MC / NeoForge:** 1.21.1 / 21.1.250, Java 21

## 2. Current status — DONE & working

Everything below is implemented, compiles, and was validated by headless `runServer` + `runClient`
boots (logs grepped clean) and by the user detonating bombs in-game.

- Nuclear bomb block, armed by flint & steel / fire charge / redstone, with a beeping fuse.
- Tick-sliced blast (no TNT entities) — crater, burn transforms, fires, mushroom cloud, flash,
  entity kill, radiation cloud, water inflow.
- Custom `big_smoke` particle so the mushroom cloud is made of large puffs.
- Registered: block, block item, block entity, mob effect, 2 damage types (data), particle type,
  creative tab (+ vanilla tab injection), recipe, loot table, mineable tag.
- Published to a public GitHub repo with README + MIT LICENSE.

**Not done yet:** real textures (placeholders in place), a downloadable Release jar, GitHub topics.
See §7 Roadmap.

## 3. How to build / run / test

From `Trinity/`, **JAVA_HOME must point at the Temurin JDK** (the machine default points at Unity's
bundled JDK and breaks gradlew):

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.8.9-hotspot"
.\gradlew.bat compileJava      # fast compile check
.\gradlew.bat build            # jar -> build/libs/trinity-0.1.0.jar
.\gradlew.bat runClient        # dev client
.\gradlew.bat runServer        # dev dedicated server (needs run/eula.txt=true)
```

**In-game (creative + cheats):** `/give @s trinity:nuclear_bomb`, `/give @s minecraft:flint_and_steel`,
place, right-click with flint & steel. Or hands-off: `/setblock ~ ~-1 ~40 trinity:nuclear_bomb` then
`/setblock ~1 ~-1 ~40 minecraft:redstone_block`. **Watch from 60–80 blocks** for the full cloud.

## 4. Architecture / file map

```
src/main/java/com/trinity/
  Trinity.java                         @Mod entry; registers all DeferredRegisters on the mod bus,
                                        + mod-bus listener for creative-tab vanilla injection
  block/
    NuclearBombBlock.java              ARMED state; arm via useItemOn (flint&steel/fire charge) or
                                        redstone (onPlace/neighborChanged); BaseEntityBlock ticker
    entity/NuclearBombBlockEntity.java fuse countdown (FUSE_TICKS=80), beeps/smoke, then removes
                                        itself + calls BlastManager.detonate
  blast/
    BlastShape.java                    precomputed sphere offsets, packed ints sorted center-outward,
                                        cached per radius (the shockwave ordering)
    Blast.java                         THE detonation. Tick-sliced destruction + burn map + fluid
                                        waking + flash + mushroom cloud + entity kill + radiation
    BlastManager.java                  @EventBusSubscriber(GAME); ticks active blasts on
                                        ServerTickEvent.Post; clears on ServerStoppingEvent
  client/
    BigSmokeParticle.java              oversized smoke particle (TextureSheetParticle, quadSize 3-6)
    TrinityClient.java                 @EventBusSubscriber(MOD, Dist.CLIENT); registers the particle
    ClientFlash.java                   plain holder for the current flash brightness (no client imports)
    TrinityHud.java                    @EventBusSubscriber(GAME, Dist.CLIENT); draws + fades the flash
  network/
    NukeFlashPayload.java              server->client packet: flash the screen white (float peak)
    TrinityNetwork.java                registers the payload (called from Trinity via modBus.addListener)
  effect/RadiationEffect.java          harmful green effect; periodic damage via trinity:radiation
  registry/
    TrinityBlocks / TrinityItems / TrinityBlockEntities / TrinityEffects /
    TrinityParticles / TrinityCreativeTabs / TrinityDamageTypes

src/main/resources/
  assets/trinity/
    blockstates/nuclear_bomb.json          lit=false/true -> nuclear_bomb / nuclear_bomb_armed
    models/block/{nuclear_bomb,nuclear_bomb_armed}.json  (cube_bottom_top)
    models/item/nuclear_bomb.json
    particles/big_smoke.json               reuses vanilla generic_0..7 smoke frames
    textures/block/{nuclear_bomb_top,_side,_side_armed}.png   PLACEHOLDER hazard art
    textures/mob_effect/radiation.png      PLACEHOLDER green trefoil
    lang/en_us.json
  data/trinity/
    damage_type/{nuclear_blast,radiation}.json   scaling "never"
    loot_table/blocks/nuclear_bomb.json          drops itself
    recipe/nuclear_bomb.json                      4 TNT + 4 gunpowder + iron block
  data/minecraft/tags/block/mineable/pickaxe.json
  templates/META-INF/neoforge.mods.toml
```

## 5. How the blast works (core design)

`NuclearBombBlockEntity` fuse hits 0 → removes its block → `BlastManager.detonate(level, pos)` creates
a `Blast` → `BlastManager` advances every active `Blast` one slice per `ServerTickEvent.Post`.

Each `Blast.tick()`:
1. **age 0:** `initialBurst()` — explosion particles, boom sounds, `sendFlash()`, `killEntities()`,
   `spawnRadiationCloud()`.

**Flash:** the nuke flash is a client-side **full-screen white overlay**, not a particle or lightning.
`Blast.sendFlash()` sends a `NukeFlashPayload` (peak brightness, scaled by the viewer's distance) to
every player within ~220 blocks; `ClientFlash` stores it, `TrinityHud` fills the screen white at that
alpha on `RenderGuiEvent.Post` and fades it (~0.86/tick) on `ClientTickEvent.Post`. (A tried-and-
rejected FLASH particle was too small/brief at distance; visual-only lightning read as "lightning".)
2. **destruction:** process up to `BLOCKS_PER_TICK` offsets from `BlastShape` (center-outward), so the
   damage rolls out as a visible shockwave. Per block (`handle`):
   - unbreakable (destroySpeed < 0, e.g. bedrock) → skip
   - within crater radius → air (`carveAir`)
   - fluid / leaves / replaceable → air
   - else `scorch()`: wood→coal_block, sand→green/lime glass (trinitite), dirt/grass→coarse_dirt,
     stone-like→blackstone/basalt; otherwise a 25% chance of rubble→air within `rubbleR`, else left.
   - Block edits use `UPDATE_CLIENTS | UPDATE_KNOWN_SHAPE` — **no neighbour updates, no physics, no
     drops** (this is what keeps it from lagging).
3. **on dig complete:** `igniteSurfaces()` scatters fires; `wakeFluids()` schedules fluid ticks on the
   boundary fluids `carveAir` recorded, so water/lava floods back in (vanilla suppresses updates, so
   this is the manual wake).
4. **mushroom cloud** (`mushroomStep`, until `ANIMATION_TICKS`): a rising `big_smoke` stem + a
   billowing cap that expands over time, plus a sustained `FLASH` for the first few ticks and
   FLAME/LAVA glow at the base.

**Radiation:** custom `RadiationEffect` (green) deals periodic `trinity:radiation` damage; on the
blast it's applied stacked with vanilla nausea/weakness/hunger/mining-fatigue, and an
`AreaEffectCloud` (green ENTITY_EFFECT particles) sits in the crater ~5 min reapplying it.

## 6. Key decisions & gotchas (READ before editing)

- **Never spawn TNT entities.** The whole point is the tick-sliced procedural blast. Reverting to
  entities/vanilla explosions reintroduces the lag/crash.
- **Particles need the long-distance flag.** The plain `ServerLevel.sendParticles(...)` broadcast is
  culled by clients beyond **32 blocks**. Use the per-player overload
  `sendParticles(player, particle, true, ...)` — that's what the `Blast.spawn(...)` helper does. Without
  it, flash/cloud are invisible from any safe viewing distance. (This bit us once.)
- **Custom particle wiring** = 3 matching pieces: the `SimpleParticleType` (`TrinityParticles`), the
  client provider (`TrinityClient` on the **MOD** bus, `Dist.CLIENT`), and the JSON
  (`particles/big_smoke.json`). Mismatch → client load error.
- **The flash is a HUD overlay driven by a packet**, not a particle/lightning. The payload handler
  (`TrinityNetwork`, common code) must only touch `ClientFlash` (a plain float holder) — never a
  client-only class directly — or a dedicated server would `NoClassDefFound`. `TrinityHud` (the
  `net.minecraft.client.*` side) is a separate `Dist.CLIENT` GAME-bus subscriber.
- **NeoForge 1.21.1 datapacks use SINGULAR folders:** `recipe/`, `loot_table/`, `tags/block/`,
  `damage_type/`. Recipe ingredients are object form (`{"item": "..."}`), not bare strings.
- **Sound events:** some are `Holder<SoundEvent>` needing `.value()` (GENERIC_EXPLODE, NOTE_BLOCK_*),
  others are plain `SoundEvent` (TNT_PRIMED). The compiler tells you which.
- **Dev-launch flakes:** the NeoForge early-loading splash can crash with a
  `FileSystemNotFoundException` in `RenderElement.fox`; disabled via `run/config/fml.toml`
  `earlyWindowControl = false`. When editing `.toml`/config from PowerShell, **write ASCII / UTF-8
  without BOM** — a BOM breaks the TOML parser.
- **Can't drive the dev client via computer-use:** it's a raw `java` process the app resolver won't
  grant control of (only Start-menu apps), and it can't be screenshotted. The user triggers blasts;
  validate code via headless boot + log grep instead.
- **`run/` and `build/` are gitignored** (also `.gradle/ repo/ *.log`). Don't commit them.

## 7. Tuning knobs (current values → where)

All in `blast/Blast.java` unless noted. Single most important knob: **`RADIUS`**.

| Knob | Value | Meaning |
|---|---|---|
| `RADIUS` | 48 | total blast radius (was 24; user wanted 2×) |
| `BLOCKS_PER_TICK` | 3500 | destruction budget/tick (lower = smoother/slower shockwave) |
| `ANIMATION_TICKS` | 200 | mushroom-cloud duration |
| `RISE_PER_TICK` | 1.3 | how fast the cloud column climbs |
| crater ratio | 0.32·R | open pit (shrunk from 0.42 on user request) |
| rubble ratio / chance | 0.60·R / 25% | messy partial cratering |
| capHeight / capMaxR | 1.6·R / 0.95·R | cloud height / cap width |
| fires | 6·R | scattered surface fires |
| radiation cloud | min(0.6·R,30), 6000t | crater haze radius / duration |
| entity damage / fire | 100000 / 300t | lethal; applyRadiation 1200t |
| `FUSE_TICKS` | 80 | fuse length (`NuclearBombBlockEntity`) |
| big_smoke quadSize / colour | 3–6 / grey 0.24–0.42 | puff size / shade (`client/BigSmokeParticle`) |
| flash range / full-white | 260 blocks / within `RADIUS×1.3` | `Blast.sendFlash()` |
| flash hold / fade | 6 ticks, then ×0.90 / tick | `client/ClientFlash` (hold = blinding window; higher fade = lingers) |

Recipe (`data/trinity/recipe/nuclear_bomb.json`): 4 gunpowder + 4 TNT + 1 iron block.

## 8. Session changelog

- **2026-09-12** — Built the mod from scratch: block, fuse BE, `Blast`/`BlastShape`/`BlastManager`,
  radiation effect + damage types, creative tab, recipe/loot/tags, placeholder textures. Validated by
  headless server + client boots. RADIUS 24; mushroom cloud from vanilla campfire smoke.
- **2026-09-13** — Round of user feedback:
  - RADIUS 24 → 48; crater ratio 0.42 → 0.32.
  - Burn variety added: sand→green/lime glass, dirt/grass→coarse dirt, stone→blackstone/basalt
    (wood→coal kept).
  - Water inflow fixed (`fluidWakes` + scheduled fluid ticks).
  - Custom `big_smoke` particle for a genuinely larger mushroom cloud.
  - **Fixed invisible flash/cloud** — particles now sent long-distance (force flag).
  - Published to public GitHub repo with README + MIT LICENSE, plus this PLAN.md.
  - **Real nuke flash** — replaced the tiny FLASH particle (then a rejected visual-only lightning
    bolt) with a packet-driven full-screen white HUD overlay that fades, scaled by viewer distance;
    then tuned brighter/longer (6-tick hold, ×0.90 fade, wider full-white zone).
  - Cut the **v0.1.0 GitHub Release** with the jar attached.

## 9. Roadmap / ideas (rough priority)

**Near-term / polish**
- [ ] Real textures for the bomb block (top/side/armed) and the radiation HUD icon — replace placeholders.
- [x] Cut a **v0.1.0 GitHub Release** with the built jar — done: https://github.com/kameronbrooks/Trinity/releases/tag/v0.1.0
- [ ] Add GitHub **topics** (`minecraft`, `neoforge`, `minecraft-mod`, `mod`) for discoverability.
- [ ] Custom detonation **sound** (a real nuke boom) instead of scaled `GENERIC_EXPLODE`.

**Features to consider**
- [ ] **Config file** (NeoForge common config) exposing `RADIUS`, fuse length, whether it kills in
      creative, radiation duration — so no recompile to tune.
- [ ] **Fallout over time** — radiation cloud spreading/decaying, contaminated water, or blocks that
      slowly "heal"/stay irradiated.
- [ ] **Blast variants** — a smaller "tactical" bomb and a bigger one, or a craftable yield selector.
- [ ] **Defuse / dud mechanic** — cut the fuse, or a chance of a dud.
- [ ] **Screen shake / camera** and a brief white screen overlay (client-side) for the flash.
- [ ] **Better mushroom shape** — separate stem vs. cap particle behaviour, smoke that lingers longer.
- [ ] **Bigger craters underwater** — currently water refills; maybe a temporary steam/void effect.

**Known limitations**
- Very large `RADIUS` or detonating inside a big ocean = lots of fluid updates while it refills
  (expected, throttled by the vanilla scheduler, but heavier).
- Lighting relight over tens of thousands of blocks can briefly tax weaker machines; dial `RADIUS` /
  `BLOCKS_PER_TICK` down if needed.
- Placeholder textures.
