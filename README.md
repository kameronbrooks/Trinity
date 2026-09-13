# Trinity

A [NeoForge](https://neoforged.net/) mod for **Minecraft 1.21.1** that adds one spectacular block: a **nuclear bomb**.

Arm it with flint and steel or a redstone signal, take cover, and watch it carve a crater, throw up a towering mushroom cloud, incinerate and level everything in the blast radius, and leave the ruins soaked in green radiation.

## Features

- **Nuclear Bomb block** — armed like TNT (flint & steel, a fire charge, or a redstone signal), with a short beeping fuse.
- **Blinding flash** and a huge, low **boom** at detonation.
- **Expanding shockwave** that levels and scorches everything within the blast radius:
  - carves a **crater**,
  - fuses **sand into green glass** (trinitite — exactly what the real 1945 Trinity test produced),
  - chars **wood into coal blocks**,
  - blackens **stone into blackstone/basalt** and **grass/dirt into coarse dirt** (scorched earth),
  - burns away foliage and scatters **fires** across flammable surfaces.
- **Towering mushroom cloud** built from a custom, oversized smoke particle.
- **Every living thing** in range is killed (creative/spectator players are spared).
- **Lingering green radiation cloud** poisons the crater for minutes — a custom `Radiation` effect stacked with nausea, weakness, hunger, and mining fatigue.
- **Water flows back into the crater** naturally after the blast.

## Performance

A wall of TNT freezes or crashes the game because each primed-TNT *entity* runs its own explosion with block drops, physics, and lighting recalcs. Trinity avoids that entirely: a single blast runs on the server tick loop and does its work in **bounded slices spread across many ticks** — an expanding shockwave — editing blocks with **neighbour updates, block physics, and item drops suppressed**. The whole size of the blast is a single constant (`Blast.RADIUS`).

## Usage

1. Craft the Nuclear Bomb: 4 TNT + 4 gunpowder around an iron block.

   ```
   G T G      G = gunpowder
   T I T      T = TNT
   G T G      I = iron block
   ```

2. Place it, then **arm** it with flint & steel / a fire charge, or give it a redstone signal.
3. Get well clear — the default blast radius is 48 blocks.

## Building

Requires JDK 21.

```bash
./gradlew build
```

The mod jar is produced in `build/libs/`. Drop it into the `mods/` folder of a Minecraft 1.21.1 instance running NeoForge 21.1.x.

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.x

## Notes

The block and effect textures are simple placeholder hazard/trefoil art — functional, but easy to replace with nicer art.

## License

[MIT](LICENSE)
