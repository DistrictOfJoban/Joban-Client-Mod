package com.lx862.mtrscripting.core.integration;

import com.lx862.mtrscripting.core.annotation.ApiInternal;
import org.mtr.mapping.holder.DefaultParticleType;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.ParticleEffect;
import org.mtr.mapping.holder.ParticleTypes;

import java.util.HashMap;
import java.util.Map;

@ApiInternal
public class MinecraftParticleData {
    private static final Map<Identifier, DefaultParticleType> particleTypeMap = new HashMap<>();
    public static DefaultParticleType[] particleTypes = new DefaultParticleType[] {
            ParticleTypes.getItemSlimeMapped(),
            ParticleTypes.getAshMapped(),
            ParticleTypes.getFishingMapped(),
            ParticleTypes.getTotemOfUndyingMapped(),
            ParticleTypes.getFireworkMapped(),
            ParticleTypes.getDamageIndicatorMapped(),
            ParticleTypes.getSquidInkMapped(),
            ParticleTypes.getComposterMapped(),
            ParticleTypes.getBubblePopMapped(),
            ParticleTypes.getExplosionEmitterMapped(),
            ParticleTypes.getNautilusMapped(),
            ParticleTypes.getWitchMapped(),
            ParticleTypes.getSplashMapped(),
            ParticleTypes.getEnchantedHitMapped(),
            ParticleTypes.getPortalMapped(),
            ParticleTypes.getSpitMapped(),
            ParticleTypes.getFallingObsidianTearMapped(),
            ParticleTypes.getSweepAttackMapped(),
            ParticleTypes.getLandingLavaMapped(),
            ParticleTypes.getFallingLavaMapped(),
            ParticleTypes.getSoulFireFlameMapped(),
            ParticleTypes.getAmbientEntityEffectMapped(),
            ParticleTypes.getCampfireSignalSmokeMapped(),
            ParticleTypes.getReversePortalMapped(),
            ParticleTypes.getExplosionMapped(),
            ParticleTypes.getItemSnowballMapped(),
            ParticleTypes.getBubbleColumnUpMapped(),
            ParticleTypes.getEndRodMapped(),
            ParticleTypes.getFallingNectarMapped(),
            ParticleTypes.getHeartMapped(),
            ParticleTypes.getPoofMapped(),
            ParticleTypes.getHappyVillagerMapped(),
            ParticleTypes.getEntityEffectMapped(),
            ParticleTypes.getDrippingHoneyMapped(),
            ParticleTypes.getFlashMapped(),
            ParticleTypes.getDrippingWaterMapped(),
            ParticleTypes.getEffectMapped(),
            ParticleTypes.getWarpedSporeMapped(),
            ParticleTypes.getSoulMapped(),
            ParticleTypes.getNoteMapped(),
            ParticleTypes.getCampfireCosySmokeMapped(),
            ParticleTypes.getDrippingLavaMapped(),
            ParticleTypes.getCritMapped(),
            ParticleTypes.getCloudMapped(),
            ParticleTypes.getEnchantMapped(),
            ParticleTypes.getLavaMapped(),
            ParticleTypes.getUnderwaterMapped(),
            ParticleTypes.getDolphinMapped(),
            ParticleTypes.getSmokeMapped(),
            ParticleTypes.getBubbleMapped(),
            ParticleTypes.getCrimsonSporeMapped(),
            ParticleTypes.getLargeSmokeMapped(),
            ParticleTypes.getFlameMapped(),
            ParticleTypes.getMyceliumMapped(),
            ParticleTypes.getInstantEffectMapped(),
            ParticleTypes.getFallingHoneyMapped(),
            ParticleTypes.getDragonBreathMapped(),
            ParticleTypes.getDrippingObsidianTearMapped(),
            ParticleTypes.getLandingHoneyMapped(),
            ParticleTypes.getRainMapped(),
            ParticleTypes.getAngryVillagerMapped(),
            ParticleTypes.getFallingWaterMapped(),
            ParticleTypes.getLandingObsidianTearMapped(),
            ParticleTypes.getElderGuardianMapped(),
            ParticleTypes.getCurrentDownMapped(),
            ParticleTypes.getWhiteAshMapped(),
            ParticleTypes.getSneezeMapped()
    };

    public static ParticleEffect getData(Identifier particleId) {
        DefaultParticleType particleType = particleTypeMap.get(particleId);
        return particleType == null ? null : ParticleEffect.cast(particleType);
    }

    public static void init() {
        for(DefaultParticleType particleType : particleTypes) {
            particleTypeMap.put(new Identifier(particleType.asString()), particleType);
        }
    }
}
