package com.trinity.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * The oversized smoke puff behind Trinity's mushroom cloud. It is an ordinary smoke-sheet particle,
 * but with its quad size multiplied several times over so each puff is metres across — the "make the
 * particles actually bigger" that vanilla campfire/smoke particles don't allow. It drifts upward,
 * slows, darkens, and fades; the cloud's overall shape comes from where {@link com.trinity.blast.Blast}
 * chooses to emit these.
 */
public class BigSmokeParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    protected BigSmokeParticle(ClientLevel level, double x, double y, double z,
                               double xd, double yd, double zd, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.friction = 0.97F;
        this.gravity = 0.0F;
        this.hasPhysics = false;
        this.xd = xd * 0.4;
        this.zd = zd * 0.4;
        this.yd = 0.10 + this.random.nextFloat() * 0.06; // buoyant rise
        this.lifetime = 70 + this.random.nextInt(50);
        this.quadSize = 3.0F + this.random.nextFloat() * 3.0F; // metres across, not centimetres
        float g = 0.24F + this.random.nextFloat() * 0.18F; // medium grey — reads against a bright sky
        this.setColor(g, g * 0.95F, g * 0.9F);
        this.setSpriteFromAge(sprites);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteFromAge(this.sprites);
        float lifeRatio = (float) this.age / (float) this.lifetime;
        this.alpha = 1.0F - lifeRatio * 0.7F; // billow out and thin near the end
    }

    /** Sprite-set provider registered on the client (see {@link TrinityClient}). */
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z, double xd, double yd, double zd) {
            return new BigSmokeParticle(level, x, y, z, xd, yd, zd, this.sprites);
        }
    }
}
