package me.erykczy.colorfullighting.mixin.compat.sodium;

import me.erykczy.colorfullighting.ColorfulLighting;
import me.erykczy.colorfullighting.accessors.BlockStateWrapper;
import me.erykczy.colorfullighting.common.ColoredLightEngine;
import me.erykczy.colorfullighting.common.Config;
import me.erykczy.colorfullighting.common.accessors.BlockStateAccessor;
import me.erykczy.colorfullighting.common.accessors.LevelAccessor;
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

    @Shadow public final int[] lm = new int[4];
    @Shadow public final float[] ao = new float[4];
    @Shadow public final float[] bl = new float[4];
    @Shadow public final float[] sl = new float[4];
    @Shadow private int flags;

    @Unique
    public final float[] gl = new float[4];
    @Unique
    public final float[] bll = new float[4];

    @Shadow public abstract boolean hasUnpackedLightData();
    @Shadow public abstract boolean hasLightData();

    @Unique
    private int getBaseColoredLight(LightDataAccess cache, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        ColorRGB4 color = ColoredLightEngine.getInstance().sampleLightColor(pos);
        int word = cache.get(x, y, z);
        int skyLight = LightDataAccess.unpackSL(word);
        if (LightDataAccess.unpackEM(word)) {
             BlockAndTintGetter level = cache.getWorld();
             BlockState state = level.getBlockState(pos);
             LevelAccessor levelAccessor = ColorfulLighting.clientAccessor.getLevel();
             if(levelAccessor != null) {
                BlockStateAccessor stateAccessor = new BlockStateWrapper(state);
                var emission = Config.getLightColor(stateAccessor);
                return SodiumPackedLightData.packData(15, ColorRGB8.fromRGB4(emission));
             }
             return SodiumPackedLightData.packData(15, 255, 255, 255);
        }
        return SodiumPackedLightData.packData(skyLight, ColorRGB8.fromRGB4(color));
    }

    @Unique
    private int getFilteredNeighborLight(LightDataAccess cache, int x, int y, int z, BlockState centerState, int centerLight) {
        BlockPos neighborPos = new BlockPos(x, y, z);
        BlockState neighborState = cache.getWorld().getBlockState(neighborPos);

        // If the center block is a filter and the neighbor is a strong light source,
        // use the center block's already-filtered light value instead of the neighbor's raw light.
        if (neighborState.getLightEmission() > 1 && !centerState.canOcclude() && !Config.getColoredLightTransmittance(null, null, new BlockStateWrapper(centerState)).equals(ColorRGB4.fromRGB4(255, 255, 255))) {
            return centerLight;
        }

        return getBaseColoredLight(cache, x, y, z);
    }

    @Unique
    private static final Direction[][] NEIGHBOR_FACES = new Direction[6][];
    static {
        NEIGHBOR_FACES[0] = new Direction[] { Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH };
        NEIGHBOR_FACES[1] = new Direction[] { Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH };
        NEIGHBOR_FACES[2] = new Direction[] { Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST };
        NEIGHBOR_FACES[3] = new Direction[] { Direction.WEST, Direction.EAST, Direction.DOWN, Direction.UP };
        NEIGHBOR_FACES[4] = new Direction[] { Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH };
        NEIGHBOR_FACES[5] = new Direction[] { Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH };
    }

    @Unique
    private static int blend(int a, int b, int c, int d) {
        var da = SodiumPackedLightData.unpackData(a);
        var db = SodiumPackedLightData.unpackData(b);
        var dc = SodiumPackedLightData.unpackData(c);
        var dd = SodiumPackedLightData.unpackData(d);
        
        int sky = blendChannel(da.skyLight4, db.skyLight4, dc.skyLight4, dd.skyLight4);
        int red = blendChannel(da.red8, db.red8, dc.red8, dd.red8);
        int green = blendChannel(da.green8, db.green8, dc.green8, dd.green8);
        int blue = blendChannel(da.blue8, db.blue8, dc.blue8, dd.blue8);
        
        return SodiumPackedLightData.packData(sky, red, green, blue);
    }

    @Unique
    private static int blendChannel(int a, int b, int c, int d) {
        if ((a == 0) || (b == 0) || (c == 0) || (d == 0)) {
            final int min = minNonZero(minNonZero(a, b), minNonZero(c, d));
            a = Math.max(a, min);
            b = Math.max(b, min);
            c = Math.max(c, min);
            d = Math.max(d, min);
        }
        return (a + b + c + d) >> 2;
    }
    
    @Unique
    private static int minNonZero(int a, int b) {
        if (a == 0) return b;
        if (b == 0) return a;
        return Math.min(a, b);
    }

    private static int clampLightmap(int val, int max) {
        int vR =  val         & 0xFF;
        int vG = (val >>> 8)  & 0xFF;
        int vS = (val >>> 16) & 0xF;
        int vB = (val >>> 20) & 0xFF;

        int mR =  max         & 0xFF;
        int mG = (max >>> 8)  & 0xFF;
        int mS = (max >>> 16) & 0xF;
        int mB = (max >>> 20) & 0xFF;

        int r   = compress(vR, mR, 21);
        int g   = compress(vG, mG, 21);
        int s   = compress(vS, mS, 2);
        int b   = compress(vB, mB, 21);

        return r | (g << 8) | (s << 16) | (b << 20) | (15 << 28);
    }

    @Unique
    private static int compress(int v, int m, int range) {
        int d = v - m;
        int ad = Math.abs(d);
        if (ad <= range) return v;
        float t = (ad - range) / (float) range;
        if (t > 1.0f) t = 1.0f;
        t = t * t * (3.0f - 2.0f * t);
        return Math.round(m + d * (1.0f - t));
    }

    @Unique
    private static int blendSmooth(int a, int b, int c, int d) {
        int ar = a & 0xFF;
        int ag = (a >>> 8) & 0xFF;
        int as = (a >>> 16) & 0xF;
        int ab = (a >>> 20) & 0xFF;

        int br = b & 0xFF;
        int bg = (b >>> 8) & 0xFF;
        int bs = (b >>> 16) & 0xF;
        int bb = (b >>> 20) & 0xFF;

        int cr = c & 0xFF;
        int cg = (c >>> 8) & 0xFF;
        int cs = (c >>> 16) & 0xF;
        int cb = (c >>> 20) & 0xFF;

        int dr = d & 0xFF;
        int dg = (d >>> 8) & 0xFF;
        int ds = (d >>> 16) & 0xF;
        int db = (d >>> 20) & 0xFF;

        int r = (ar + br + cr + dr) >> 2;
        int g = (ag + bg + cg + dg) >> 2;
        int s = (as + bs + cs + ds) >> 2;
        int bch = (ab + bb + cb + db) >> 2;

        return r | (g << 8) | (s << 16) | (bch << 20) | (15 << 28);
    }

    /**
     * @author Erykczy
     * @reason Inject colored lighting logic into AO calculation
     */
    @Overwrite
    public void initLightData(LightDataAccess cache, BlockPos pos, Direction direction, boolean offset) {
        final int x = pos.getX();
        final int y = pos.getY();
        final int z = pos.getZ();

        final BlockState centerState = cache.getWorld().getBlockState(pos);
        final int centerLight = getBaseColoredLight(cache, x, y, z);

        final int adjX;
        final int adjY;
        final int adjZ;

        if (offset) {
            adjX = x + direction.getStepX();
            adjY = y + direction.getStepY();
            adjZ = z + direction.getStepZ();
        } else {
            adjX = x;
            adjY = y;
            adjZ = z;
        }

        final int adjWord = cache.get(adjX, adjY, adjZ);

        final float caao = LightDataAccess.unpackAO(adjWord);

        Direction[] faces = NEIGHBOR_FACES[direction.get3DDataValue()];

        final int e0 = cache.get(adjX, adjY, adjZ, faces[0]);
        final int e0lm = getFilteredNeighborLight(cache, adjX + faces[0].getStepX(), adjY + faces[0].getStepY(), adjZ + faces[0].getStepZ(), centerState, centerLight);
        final float e0ao = LightDataAccess.unpackAO(e0);
        final boolean e0op = LightDataAccess.unpackOP(e0);

        final int e1 = cache.get(adjX, adjY, adjZ, faces[1]);
        final int e1lm = getFilteredNeighborLight(cache, adjX + faces[1].getStepX(), adjY + faces[1].getStepY(), adjZ + faces[1].getStepZ(), centerState, centerLight);
        final float e1ao = LightDataAccess.unpackAO(e1);
        final boolean e1op = LightDataAccess.unpackOP(e1);

        final int e2 = cache.get(adjX, adjY, adjZ, faces[2]);
        final int e2lm = getFilteredNeighborLight(cache, adjX + faces[2].getStepX(), adjY + faces[2].getStepY(), adjZ + faces[2].getStepZ(), centerState, centerLight);
        final float e2ao = LightDataAccess.unpackAO(e2);
        final boolean e2op = LightDataAccess.unpackOP(e2);

        final int e3 = cache.get(adjX, adjY, adjZ, faces[3]);
        final int e3lm = getFilteredNeighborLight(cache, adjX + faces[3].getStepX(), adjY + faces[3].getStepY(), adjZ + faces[3].getStepZ(), centerState, centerLight);
        final float e3ao = LightDataAccess.unpackAO(e3);
        final boolean e3op = LightDataAccess.unpackOP(e3);

        final int c0lm;
        if (e2op && e0op) {
            c0lm = e0lm;
        } else {
            c0lm = getFilteredNeighborLight(cache, adjX + faces[0].getStepX() + faces[2].getStepX(), adjY + faces[0].getStepY() + faces[2].getStepY(), adjZ + faces[0].getStepZ() + faces[2].getStepZ(), centerState, centerLight);
        }

        final int c1lm;
        if (e3op && e0op) {
            c1lm = e0lm;
        } else {
            c1lm = getFilteredNeighborLight(cache, adjX + faces[0].getStepX() + faces[3].getStepX(), adjY + faces[0].getStepY() + faces[3].getStepY(), adjZ + faces[0].getStepZ() + faces[3].getStepZ(), centerState, centerLight);
        }

        final int c2lm;
        if (e2op && e1op) {
            c2lm = e1lm;
        } else {
            c2lm = getFilteredNeighborLight(cache, adjX + faces[1].getStepX() + faces[2].getStepX(), adjY + faces[1].getStepY() + faces[2].getStepY(), adjZ + faces[1].getStepZ() + faces[2].getStepZ(), centerState, centerLight);
        }

        final int c3lm;
        if (e3op && e1op) {
            c3lm = e1lm;
        } else {
            c3lm = getFilteredNeighborLight(cache, adjX + faces[1].getStepX() + faces[3].getStepX(), adjY + faces[1].getStepY() + faces[3].getStepY(), adjZ + faces[1].getStepZ() + faces[3].getStepZ(), centerState, centerLight);
        }

        float[] ao = this.ao;
        ao[0] = (e3ao + e0ao + LightDataAccess.unpackAO(cache.get(adjX, adjY, adjZ, faces[0], faces[3])) + caao) * 0.25f;
        ao[1] = (e2ao + e0ao + LightDataAccess.unpackAO(cache.get(adjX, adjY, adjZ, faces[0], faces[2])) + caao) * 0.25f;
        ao[2] = (e2ao + e1ao + LightDataAccess.unpackAO(cache.get(adjX, adjY, adjZ, faces[1], faces[2])) + caao) * 0.25f;
        ao[3] = (e3ao + e1ao + LightDataAccess.unpackAO(cache.get(adjX, adjY, adjZ, faces[1], faces[3])) + caao) * 0.25f;

        int[] cb = this.lm;

        final int calm;
        final boolean caem;

        if (offset && LightDataAccess.unpackFO(adjWord)) {
            final int originWord = cache.get(x, y, z);
            calm = getFilteredNeighborLight(cache, x, y, z, centerState, centerLight);
            caem = LightDataAccess.unpackEM(originWord);
        } else {
            calm = getFilteredNeighborLight(cache, adjX, adjY, adjZ, centerState, centerLight);
            caem = LightDataAccess.unpackEM(adjWord);
        }

        cb[0] = blend(e3lm, e0lm, c1lm, calm);
        cb[1] = blend(e2lm, e0lm, c0lm, calm);
        cb[2] = blend(e2lm, e1lm, c2lm, calm);
        cb[3] = blend(e3lm, e1lm, c3lm, calm);

        for (int i = 0; i < 4; i++) {
            cb[i] = clampLightmap(cb[i], calm); // use max-safe
        }

        this.flags |= 1;
    }

    /**
     * @author Erykczy
     * @reason Unpack colored light data
     */
    @Overwrite
    public void unpackLightData() {
        int[] lm = this.lm;

        float[] bl = this.bl;
        float[] sl = this.sl;
        float[] gl = this.gl;
        float[] bll = this.bll;

        for(int i=0; i<4; i++) {
            var data = SodiumPackedLightData.unpackData(lm[i]);
            bl[i] = data.red8;
            gl[i] = data.green8;
            bll[i] = data.blue8;
            sl[i] = data.skyLight4;
        }

        this.flags |= 2;
    }
    
    @Override
    public void ensureUnpacked() {
        if (!this.hasUnpackedLightData()) {
            this.unpackLightData();
        }
    }
    
    @Override
    public int getBlendedLightMap(float[] w) {
        ensureUnpacked();
        float r = weightedSum(this.bl, w);
        float g = weightedSum(this.gl, w);
        float b = weightedSum(this.bll, w);
        float s = weightedSum(this.sl, w);
        
        return SodiumPackedLightData.packData((int)s, (int)r, (int)g, (int)b);
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
    
    @Unique
    private static float weightedSum(float[] v, float[] w) {
        float t0 = v[0] * w[0];
        float t1 = v[1] * w[1];
        float t2 = v[2] * w[2];
        float t3 = v[3] * w[3];

        return t0 + t1 + t2 + t3;
    }
}
