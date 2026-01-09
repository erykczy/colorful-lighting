package me.erykczy.colorfullighting.mixin.compat.sodium;

import me.erykczy.colorfullighting.accessors.BlockStateWrapper;
import me.erykczy.colorfullighting.common.ColoredLightEngine;
import me.erykczy.colorfullighting.common.Config;
import me.erykczy.colorfullighting.common.util.ColorRGB4;
import me.erykczy.colorfullighting.common.util.ColorRGB8;
import me.erykczy.colorfullighting.compat.sodium.SodiumAoFaceDataExtension;
import me.erykczy.colorfullighting.compat.sodium.SodiumPackedLightData;
import me.jellysquid.mods.sodium.client.model.light.data.LightDataAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(targets = "me.jellysquid.mods.sodium.client.model.light.smooth.AoFaceData", remap = false)
public abstract class SodiumAoFaceDataMixin implements SodiumAoFaceDataExtension {

    @Shadow
    public final int[] lm = new int[4];
    @Shadow
    public final float[] ao = new float[4];
    @Shadow
    public final float[] bl = new float[4];
    @Shadow
    public final float[] sl = new float[4];
    @Shadow
    private int flags;

    @Unique
    public final float[] gl = new float[4];
    @Unique
    public final float[] bll = new float[4];

    @Shadow
    public abstract boolean hasUnpackedLightData();

    @Unique
    private int getBaseColoredLight(LightDataAccess cache, int x, int y, int z) {
        int word = cache.get(x, y, z);

        if (LightDataAccess.unpackEM(word)) {
            BlockAndTintGetter level = cache.getWorld();
            BlockState state = level.getBlockState(new BlockPos(x, y, z));
            ColorRGB4 emission = Config.getLightColor(new BlockStateWrapper(state));
            return SodiumPackedLightData.packData(LightDataAccess.unpackSL(word), ColorRGB8.fromRGB4(emission));
        }

        int packed = ColoredLightEngine.getInstance()
                .sampleLightColorPacked(x, y, z);

        int sky = LightDataAccess.unpackSL(word);
        // packed is 0x00000RGB (12 bits)
        int r = (packed >> 8) & 0xF;
        int g = (packed >> 4) & 0xF;
        int b = packed & 0xF;

        // Convert 0-15 range to 0-255 range for SodiumPackedLightData
        return SodiumPackedLightData.packData(sky, r * 17, g * 17, b * 17);
    }

    @Unique
    private static final Direction[][] NEIGHBOR_FACES = {
            {Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH},
            {Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH},
            {Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST},
            {Direction.WEST, Direction.EAST, Direction.DOWN, Direction.UP},
            {Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH},
            {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH}
    };

    @Unique
    private static int blend(int a, int b, int c, int d) {
        return SodiumPackedLightData.blend(a, b, c, d);
    }

    @Unique
    private static int clamp(int val, int max) {
        int vR = val & 0xFF;
        int vG = (val >>> 8) & 0xFF;
        int vS = (val >>> 16) & 0xF;
        int vB = (val >>> 20) & 0xFF;

        int mR = max & 0xFF;
        int mG = (max >>> 8) & 0xFF;
        int mS = (max >>> 16) & 0xF;
        int mB = (max >>> 20) & 0xFF;

        int r = Math.max(vR, mR - 17);
        r = Math.min(r, mR + 17);

        int g = Math.max(vG, mG - 17);
        g = Math.min(g, mG + 17);

        int sky = Math.max(vS, mS - 1);
        sky = Math.min(sky, mS + 1);

        int b = Math.max(vB, mB - 17);
        b = Math.min(b, mB + 17);

        return r | (g << 8) | (sky << 16) | (b << 20) | (15 << 28);
    }

    @Unique
    private boolean isGlowing(BlockState state, BlockAndTintGetter world, BlockPos pos) {
        return Config.getEmissionBrightness(new BlockStateWrapper(state)) > 0;
    }

    /**
     * @author Mysticpasta1
     * @reason initialize light data
     */
    @Overwrite
    public void initLightData(LightDataAccess cache, BlockPos pos, Direction dir, boolean offset) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        BlockState centerState = cache.getWorld().getBlockState(pos);
        int centerWord = cache.get(x, y, z);

        if (LightDataAccess.unpackEM(centerWord)) {
            ColorRGB4 emission = Config.getLightColor(new BlockStateWrapper(centerState));
            int emissiveLm = SodiumPackedLightData.packData(LightDataAccess.unpackSL(centerWord), ColorRGB8.fromRGB4(emission));
            for (int i = 0; i < 4; i++) {
                this.lm[i] = emissiveLm;
                this.ao[i] = 1.0f;
            }
            this.flags |= 1;
            return;
        }

        int adjX = offset ? x + dir.getStepX() : x;
        int adjY = offset ? y + dir.getStepY() : y;
        int adjZ = offset ? z + dir.getStepZ() : z;

        int adjWord = cache.get(adjX, adjY, adjZ);

        float caao = LightDataAccess.unpackAO(adjWord);

        Direction[] faces = NEIGHBOR_FACES[dir.get3DDataValue()];

        int e0 = cache.get(adjX, adjY, adjZ, faces[0]);
        int e1 = cache.get(adjX, adjY, adjZ, faces[1]);
        int e2 = cache.get(adjX, adjY, adjZ, faces[2]);
        int e3 = cache.get(adjX, adjY, adjZ, faces[3]);

        float e0ao = LightDataAccess.unpackAO(e0);
        float e1ao = LightDataAccess.unpackAO(e1);
        float e2ao = LightDataAccess.unpackAO(e2);
        float e3ao = LightDataAccess.unpackAO(e3);

        boolean e0op = LightDataAccess.unpackOP(e0);
        boolean e1op = LightDataAccess.unpackOP(e1);
        boolean e2op = LightDataAccess.unpackOP(e2);
        boolean e3op = LightDataAccess.unpackOP(e3);

        int e0lm = getBaseColoredLight(cache,
                adjX + faces[0].getStepX(),
                adjY + faces[0].getStepY(),
                adjZ + faces[0].getStepZ());

        int e1lm = getBaseColoredLight(cache,
                adjX + faces[1].getStepX(),
                adjY + faces[1].getStepY(),
                adjZ + faces[1].getStepZ());

        int e2lm = getBaseColoredLight(cache,
                adjX + faces[2].getStepX(),
                adjY + faces[2].getStepY(),
                adjZ + faces[2].getStepZ());

        int e3lm = getBaseColoredLight(cache,
                adjX + faces[3].getStepX(),
                adjY + faces[3].getStepY(),
                adjZ + faces[3].getStepZ());

        int c0lm = (e2op && e0op)
                ? e0lm
                : getBaseColoredLight(cache,
                adjX + faces[0].getStepX() + faces[2].getStepX(),
                adjY + faces[0].getStepY() + faces[2].getStepY(),
                adjZ + faces[0].getStepZ() + faces[2].getStepZ());

        int c1lm = (e3op && e0op)
                ? e0lm
                : getBaseColoredLight(cache,
                adjX + faces[0].getStepX() + faces[3].getStepX(),
                adjY + faces[0].getStepY() + faces[3].getStepY(),
                adjZ + faces[0].getStepZ() + faces[3].getStepZ());

        int c2lm = (e2op && e1op)
                ? e1lm
                : getBaseColoredLight(cache,
                adjX + faces[1].getStepX() + faces[2].getStepX(),
                adjY + faces[1].getStepY() + faces[2].getStepY(),
                adjZ + faces[1].getStepZ() + faces[2].getStepZ());

        int c3lm = (e3op && e1op)
                ? e1lm
                : getBaseColoredLight(cache,
                adjX + faces[1].getStepX() + faces[3].getStepX(),
                adjY + faces[1].getStepY() + faces[3].getStepY(),
                adjZ + faces[1].getStepZ() + faces[3].getStepZ());

        int corner0 = cache.get(adjX, adjY, adjZ, faces[0], faces[3]);
        int corner1 = cache.get(adjX, adjY, adjZ, faces[0], faces[2]);
        int corner2 = cache.get(adjX, adjY, adjZ, faces[1], faces[2]);
        int corner3 = cache.get(adjX, adjY, adjZ, faces[1], faces[3]);

        float cornerAo0 = LightDataAccess.unpackAO(corner0);
        float cornerAo1 = LightDataAccess.unpackAO(corner1);
        float cornerAo2 = LightDataAccess.unpackAO(corner2);
        float cornerAo3 = LightDataAccess.unpackAO(corner3);

        this.ao[0] = (e3ao + e0ao + cornerAo0 + caao) * 0.25f;
        this.ao[1] = (e2ao + e0ao + cornerAo1 + caao) * 0.25f;
        this.ao[2] = (e2ao + e1ao + cornerAo2 + caao) * 0.25f;
        this.ao[3] = (e3ao + e1ao + cornerAo3 + caao) * 0.25f;

        int lm = getBaseColoredLight(cache, adjX, adjY, adjZ);
        this.lm[0] = blend(clamp(e3lm, lm), clamp(e0lm, lm), clamp(c1lm, lm), lm);
        this.lm[1] = blend(clamp(e2lm, lm), clamp(e0lm, lm), clamp(c0lm, lm), lm);
        this.lm[2] = blend(clamp(e2lm, lm), clamp(e1lm, lm), clamp(c2lm, lm), lm);
        this.lm[3] = blend(clamp(e3lm, lm), clamp(e1lm, lm), clamp(c3lm, lm), lm);

        this.flags |= 1;
    }

    /**
     * @author Mysticpasta1
     * @reason unpack light data
     */
    @Overwrite
    public void unpackLightData() {
        for (int i = 0; i < 4; i++) {
            int packed = this.lm[i];
            this.bl[i] = packed & 0xFF;
            this.gl[i] = (packed >>> 8) & 0xFF;
            this.sl[i] = (packed >>> 16) & 0xF;
            this.bll[i] = (packed >>> 20) & 0xFF;
        }
        this.flags |= 2;
    }

    @Override
    public void ensureUnpacked() {
        if (!this.hasUnpackedLightData()) {
            unpackLightData();
        }
    }

    @Override
    public int getBlendedLightMap(float[] w) {
        ensureUnpacked();
        float r = weightedSum(this.bl, w);
        float g = weightedSum(this.gl, w);
        float b = weightedSum(this.bll, w);
        float s = weightedSum(this.sl, w);
        return SodiumPackedLightData.packData((int) s, (int) r, (int) g, (int) b);
    }

    @Override
    public float getBlendedShade(float[] w) {
        ensureUnpacked();
        return weightedSum(this.ao, w);
    }

    @Override
    public float getBlendedRed(float[] w) {
        ensureUnpacked();
        return weightedSum(this.bl, w);
    }

    @Override
    public float getBlendedGreen(float[] w) {
        ensureUnpacked();
        return weightedSum(this.gl, w);
    }

    @Override
    public float getBlendedBlue(float[] w) {
        ensureUnpacked();
        return weightedSum(this.bll, w);
    }

    @Override
    public float getBlendedSky(float[] w) {
        ensureUnpacked();
        return weightedSum(this.sl, w);
    }

    @Override
    public boolean hasLightData() {
        return (this.flags & 1) != 0;
    }

    @Unique
    private static float weightedSum(float[] v, float[] w) {
        return v[0] * w[0] + v[1] * w[1] + v[2] * w[2] + v[3] * w[3];
    }
}