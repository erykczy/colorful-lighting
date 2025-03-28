package com.example.examplemod.mixin;

import com.example.examplemod.client.ModRenderTypes;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.client.Camera;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.SortedSet;

@Mixin(LevelRenderer.class)
public class LeverRendererMixin {
    @Inject(at = @At("HEAD"), method = "renderLevel", cancellable = true)
    private void renderLevel(DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f frustumMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
        LevelRenderer renderer = (LevelRenderer)(Object)this;

        TickRateManager tickratemanager = renderer.minecraft.level.tickRateManager();
        float f = deltaTracker.getGameTimeDeltaPartialTick(false);
        RenderSystem.setShaderGameTime(renderer.level.getGameTime(), f);
        renderer.blockEntityRenderDispatcher.prepare(renderer.level, camera, renderer.minecraft.hitResult);
        renderer.entityRenderDispatcher.prepare(renderer.level, camera, renderer.minecraft.crosshairPickEntity);
        ProfilerFiller profilerfiller = renderer.level.getProfiler();
        profilerfiller.popPush("light_update_queue");
        renderer.level.pollLightUpdates();
        profilerfiller.popPush("light_updates");
        renderer.level.getChunkSource().getLightEngine().runLightUpdates();
        Vec3 vec3 = camera.getPosition();
        double d0 = vec3.x();
        double d1 = vec3.y();
        double d2 = vec3.z();
        profilerfiller.popPush("culling");
        boolean flag = renderer.capturedFrustum != null;
        Frustum frustum;
        if (flag) {
            frustum = renderer.capturedFrustum;
            frustum.prepare(renderer.frustumPos.x, renderer.frustumPos.y, renderer.frustumPos.z);
        } else {
            frustum = renderer.cullingFrustum;
        }

        renderer.minecraft.getProfiler().popPush("captureFrustum");
        if (renderer.captureFrustum) {
            renderer.captureFrustum(frustumMatrix, projectionMatrix, vec3.x, vec3.y, vec3.z, flag ? new Frustum(frustumMatrix, projectionMatrix) : frustum);
            renderer.captureFrustum = false;
        }

        profilerfiller.popPush("clear");
        FogRenderer.setupColor(camera, f, renderer.minecraft.level, renderer.minecraft.options.getEffectiveRenderDistance(), gameRenderer.getDarkenWorldAmount(f));
        FogRenderer.levelFogColor();
        RenderSystem.clear(16640, Minecraft.ON_OSX);
        float f1 = gameRenderer.getRenderDistance();
        boolean flag1 = renderer.minecraft.level.effects().isFoggyAt(Mth.floor(d0), Mth.floor(d1)) || renderer.minecraft.gui.getBossOverlay().shouldCreateWorldFog();
        FogRenderer.setupFog(camera, FogRenderer.FogMode.FOG_SKY, f1, flag1, f);
        profilerfiller.popPush("sky");
        RenderSystem.setShader(GameRenderer::getPositionShader);
        renderer.renderSky(frustumMatrix, projectionMatrix, f, camera, flag1, () -> FogRenderer.setupFog(camera, FogRenderer.FogMode.FOG_SKY, f1, flag1, f));
        net.neoforged.neoforge.client.ClientHooks.dispatchRenderStage(net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage.AFTER_SKY, renderer, null, frustumMatrix, projectionMatrix, renderer.ticks, camera, frustum);
        profilerfiller.popPush("fog");
        FogRenderer.setupFog(camera, FogRenderer.FogMode.FOG_TERRAIN, Math.max(f1, 32.0F), flag1, f);
        profilerfiller.popPush("terrain_setup");
        renderer.setupRender(camera, frustum, flag, renderer.minecraft.player.isSpectator());
        profilerfiller.popPush("compile_sections");
        renderer.compileSections(camera);
        profilerfiller.popPush("terrain");
        renderer.renderSectionLayer(ModRenderTypes.COLORED_LIGHT_SOLID, d0, d1, d2, frustumMatrix, projectionMatrix); // added
        renderer.renderSectionLayer(RenderType.solid(), d0, d1, d2, frustumMatrix, projectionMatrix);

        renderer.minecraft.getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS).setBlurMipmap(false, renderer.minecraft.options.mipmapLevels().get() > 0); // Neo: fix flickering leaves when mods mess up the blurMipmap settings
        renderer.renderSectionLayer(ModRenderTypes.COLORED_LIGHT_CUTOUT_MIPPED, d0, d1, d2, frustumMatrix, projectionMatrix); // added
        renderer.renderSectionLayer(RenderType.cutoutMipped(), d0, d1, d2, frustumMatrix, projectionMatrix);

        renderer.minecraft.getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS).restoreLastBlurMipmap();
        renderer.renderSectionLayer(ModRenderTypes.COLORED_LIGHT_CUTOUT, d0, d1, d2, frustumMatrix, projectionMatrix); // added
        renderer.renderSectionLayer(RenderType.cutout(), d0, d1, d2, frustumMatrix, projectionMatrix);

        if (renderer.level.effects().constantAmbientLight()) {
            Lighting.setupNetherLevel();
        } else {
            Lighting.setupLevel();
        }

        profilerfiller.popPush("entities");
        renderer.renderedEntities = 0;
        renderer.culledEntities = 0;
        if (renderer.itemEntityTarget != null) {
            renderer.itemEntityTarget.clear(Minecraft.ON_OSX);
            renderer.itemEntityTarget.copyDepthFrom(renderer.minecraft.getMainRenderTarget());
            renderer.minecraft.getMainRenderTarget().bindWrite(false);
        }

        if (renderer.weatherTarget != null) {
            renderer.weatherTarget.clear(Minecraft.ON_OSX);
        }

        if (renderer.shouldShowEntityOutlines()) {
            renderer.entityTarget.clear(Minecraft.ON_OSX);
            renderer.minecraft.getMainRenderTarget().bindWrite(false);
        }

        Matrix4fStack matrix4fstack = RenderSystem.getModelViewStack();
        matrix4fstack.pushMatrix();
        matrix4fstack.mul(frustumMatrix);
        RenderSystem.applyModelViewMatrix();
        boolean flag2 = false;
        PoseStack posestack = new PoseStack();
        MultiBufferSource.BufferSource multibuffersource$buffersource = renderer.renderBuffers.bufferSource();

        for (Entity entity : renderer.level.entitiesForRendering()) {
            if (renderer.entityRenderDispatcher.shouldRender(entity, frustum, d0, d1, d2) || entity.hasIndirectPassenger(renderer.minecraft.player)) {
                BlockPos blockpos = entity.blockPosition();
                if ((renderer.level.isOutsideBuildHeight(blockpos.getY()) || renderer.isSectionCompiled(blockpos))
                        && (
                        entity != camera.getEntity()
                                || camera.isDetached()
                                || camera.getEntity() instanceof LivingEntity && ((LivingEntity)camera.getEntity()).isSleeping()
                )
                        && (!(entity instanceof LocalPlayer) || camera.getEntity() == entity || (entity == renderer.minecraft.player && !renderer.minecraft.player.isSpectator()))) { // Neo: render local player entity when it is not the camera entity
                    renderer.renderedEntities++;
                    if (entity.tickCount == 0) {
                        entity.xOld = entity.getX();
                        entity.yOld = entity.getY();
                        entity.zOld = entity.getZ();
                    }

                    MultiBufferSource multibuffersource;
                    if (renderer.shouldShowEntityOutlines() && renderer.minecraft.shouldEntityAppearGlowing(entity)) {
                        flag2 = true;
                        OutlineBufferSource outlinebuffersource = renderer.renderBuffers.outlineBufferSource();
                        multibuffersource = outlinebuffersource;
                        int i = entity.getTeamColor();
                        outlinebuffersource.setColor(FastColor.ARGB32.red(i), FastColor.ARGB32.green(i), FastColor.ARGB32.blue(i), 255);
                    } else {
                        if (renderer.shouldShowEntityOutlines() && entity.hasCustomOutlineRendering(renderer.minecraft.player)) { // FORGE: allow custom outline rendering
                            flag2 = true;
                        }
                        multibuffersource = multibuffersource$buffersource;
                    }

                    float f2 = deltaTracker.getGameTimeDeltaPartialTick(!tickratemanager.isEntityFrozen(entity));
                    renderer.renderEntity(entity, d0, d1, d2, f2, posestack, multibuffersource);
                }
            }
        }

        multibuffersource$buffersource.endLastBatch();
        renderer.checkPoseStack(posestack);
        multibuffersource$buffersource.endBatch(RenderType.entitySolid(TextureAtlas.LOCATION_BLOCKS));
        multibuffersource$buffersource.endBatch(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS));
        multibuffersource$buffersource.endBatch(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
        multibuffersource$buffersource.endBatch(RenderType.entitySmoothCutout(TextureAtlas.LOCATION_BLOCKS));
        net.neoforged.neoforge.client.ClientHooks.dispatchRenderStage(net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage.AFTER_ENTITIES, renderer, posestack, frustumMatrix, projectionMatrix, renderer.ticks, camera, frustum);
        profilerfiller.popPush("blockentities");

        for (SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection : renderer.visibleSections) {
            List<BlockEntity> list = sectionrenderdispatcher$rendersection.getCompiled().getRenderableBlockEntities();
            if (!list.isEmpty()) {
                for (BlockEntity blockentity1 : list) {
                    if (!net.neoforged.neoforge.client.ClientHooks.isBlockEntityRendererVisible(renderer.blockEntityRenderDispatcher, blockentity1, frustum)) continue;
                    BlockPos blockpos4 = blockentity1.getBlockPos();
                    MultiBufferSource multibuffersource1 = multibuffersource$buffersource;
                    posestack.pushPose();
                    posestack.translate((double)blockpos4.getX() - d0, (double)blockpos4.getY() - d1, (double)blockpos4.getZ() - d2);
                    SortedSet<BlockDestructionProgress> sortedset = renderer.destructionProgress.get(blockpos4.asLong());
                    if (sortedset != null && !sortedset.isEmpty()) {
                        int j = sortedset.last().getProgress();
                        if (j >= 0) {
                            PoseStack.Pose posestack$pose = posestack.last();
                            VertexConsumer vertexconsumer = new SheetedDecalTextureGenerator(
                                    renderer.renderBuffers.crumblingBufferSource().getBuffer(ModelBakery.DESTROY_TYPES.get(j)), posestack$pose, 1.0F
                            );
                            multibuffersource1 = p_234298_ -> {
                                VertexConsumer vertexconsumer3 = multibuffersource$buffersource.getBuffer(p_234298_);
                                return p_234298_.affectsCrumbling() ? VertexMultiConsumer.create(vertexconsumer, vertexconsumer3) : vertexconsumer3;
                            };
                        }
                    }
                    if (renderer.shouldShowEntityOutlines() && blockentity1.hasCustomOutlineRendering(renderer.minecraft.player)) { // Neo: allow custom outline rendering
                        flag2 = true;
                    }

                    renderer.blockEntityRenderDispatcher.render(blockentity1, f, posestack, multibuffersource1);
                    posestack.popPose();
                }
            }
        }

        synchronized (renderer.globalBlockEntities) {
            for (BlockEntity blockentity : renderer.globalBlockEntities) {
                if (!net.neoforged.neoforge.client.ClientHooks.isBlockEntityRendererVisible(renderer.blockEntityRenderDispatcher, blockentity, frustum)) continue;
                BlockPos blockpos3 = blockentity.getBlockPos();
                posestack.pushPose();
                posestack.translate((double)blockpos3.getX() - d0, (double)blockpos3.getY() - d1, (double)blockpos3.getZ() - d2);
                if (renderer.shouldShowEntityOutlines() && blockentity.hasCustomOutlineRendering(renderer.minecraft.player)) { // Neo: allow custom outline rendering
                    flag2 = true;
                }
                renderer.blockEntityRenderDispatcher.render(blockentity, f, posestack, multibuffersource$buffersource);
                posestack.popPose();
            }
        }

        renderer.checkPoseStack(posestack);
        multibuffersource$buffersource.endBatch(RenderType.solid());
        multibuffersource$buffersource.endBatch(RenderType.endPortal());
        multibuffersource$buffersource.endBatch(RenderType.endGateway());
        multibuffersource$buffersource.endBatch(Sheets.solidBlockSheet());
        multibuffersource$buffersource.endBatch(Sheets.cutoutBlockSheet());
        multibuffersource$buffersource.endBatch(Sheets.bedSheet());
        multibuffersource$buffersource.endBatch(Sheets.shulkerBoxSheet());
        multibuffersource$buffersource.endBatch(Sheets.signSheet());
        multibuffersource$buffersource.endBatch(Sheets.hangingSignSheet());
        multibuffersource$buffersource.endBatch(Sheets.chestSheet());
        renderer.renderBuffers.outlineBufferSource().endOutlineBatch();
        // Neo: handle outline effect requests outside glowing entities
        if (renderer.outlineEffectRequested) {
            flag2 |= renderer.shouldShowEntityOutlines();
            renderer.outlineEffectRequested = false;
        }
        if (flag2) {
            renderer.entityEffect.process(deltaTracker.getGameTimeDeltaTicks());
            renderer.minecraft.getMainRenderTarget().bindWrite(false);
        }

        net.neoforged.neoforge.client.ClientHooks.dispatchRenderStage(net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES, renderer, posestack, frustumMatrix, projectionMatrix, renderer.ticks, camera, frustum);
        profilerfiller.popPush("destroyProgress");

        for (Long2ObjectMap.Entry<SortedSet<BlockDestructionProgress>> entry : renderer.destructionProgress.long2ObjectEntrySet()) {
            BlockPos blockpos2 = BlockPos.of(entry.getLongKey());
            double d3 = (double)blockpos2.getX() - d0;
            double d4 = (double)blockpos2.getY() - d1;
            double d5 = (double)blockpos2.getZ() - d2;
            if (!(d3 * d3 + d4 * d4 + d5 * d5 > 1024.0)) {
                SortedSet<BlockDestructionProgress> sortedset1 = entry.getValue();
                if (sortedset1 != null && !sortedset1.isEmpty()) {
                    int k = sortedset1.last().getProgress();
                    posestack.pushPose();
                    posestack.translate((double)blockpos2.getX() - d0, (double)blockpos2.getY() - d1, (double)blockpos2.getZ() - d2);
                    PoseStack.Pose posestack$pose1 = posestack.last();
                    VertexConsumer vertexconsumer1 = new SheetedDecalTextureGenerator(
                            renderer.renderBuffers.crumblingBufferSource().getBuffer(ModelBakery.DESTROY_TYPES.get(k)), posestack$pose1, 1.0F
                    );
                    net.neoforged.neoforge.client.model.data.ModelData modelData = renderer.level.getModelData(blockpos2);
                    renderer.minecraft
                            .getBlockRenderer()
                            .renderBreakingTexture(renderer.level.getBlockState(blockpos2), blockpos2, renderer.level, posestack, vertexconsumer1, modelData);
                    posestack.popPose();
                }
            }
        }

        renderer.checkPoseStack(posestack);
        HitResult hitresult = renderer.minecraft.hitResult;
        if (renderBlockOutline && hitresult != null && hitresult.getType() == HitResult.Type.BLOCK) {
            profilerfiller.popPush("outline");
            BlockPos blockpos1 = ((BlockHitResult)hitresult).getBlockPos();
            BlockState blockstate = renderer.level.getBlockState(blockpos1);
            if (!net.neoforged.neoforge.client.ClientHooks.onDrawHighlight(renderer, camera, hitresult, deltaTracker, posestack, multibuffersource$buffersource))
                if (!blockstate.isAir() && renderer.level.getWorldBorder().isWithinBounds(blockpos1)) {
                    VertexConsumer vertexconsumer2 = multibuffersource$buffersource.getBuffer(RenderType.lines());
                    renderer.renderHitOutline(posestack, vertexconsumer2, camera.getEntity(), d0, d1, d2, blockpos1, blockstate);
                }
        } else if (hitresult != null && hitresult.getType() == HitResult.Type.ENTITY) {
            net.neoforged.neoforge.client.ClientHooks.onDrawHighlight(renderer, camera, hitresult, deltaTracker, posestack, multibuffersource$buffersource);
        }

        renderer.minecraft.debugRenderer.render(posestack, multibuffersource$buffersource, d0, d1, d2);
        multibuffersource$buffersource.endLastBatch();
        multibuffersource$buffersource.endBatch(Sheets.translucentCullBlockSheet());
        multibuffersource$buffersource.endBatch(Sheets.bannerSheet());
        multibuffersource$buffersource.endBatch(Sheets.shieldSheet());
        multibuffersource$buffersource.endBatch(RenderType.armorEntityGlint());
        multibuffersource$buffersource.endBatch(RenderType.glint());
        multibuffersource$buffersource.endBatch(RenderType.glintTranslucent());
        multibuffersource$buffersource.endBatch(RenderType.entityGlint());
        multibuffersource$buffersource.endBatch(RenderType.entityGlintDirect());
        multibuffersource$buffersource.endBatch(RenderType.waterMask());
        renderer.renderBuffers.crumblingBufferSource().endBatch();
        if (renderer.transparencyChain != null) {
            multibuffersource$buffersource.endBatch(RenderType.lines());
            multibuffersource$buffersource.endBatch();
            renderer.translucentTarget.clear(Minecraft.ON_OSX);
            renderer.translucentTarget.copyDepthFrom(renderer.minecraft.getMainRenderTarget());
            profilerfiller.popPush("translucent");
            renderer.renderSectionLayer(ModRenderTypes.COLORED_LIGHT_TRANSLUCENT, d0, d1, d2, frustumMatrix, projectionMatrix);
            renderer.renderSectionLayer(RenderType.translucent(), d0, d1, d2, frustumMatrix, projectionMatrix);
            profilerfiller.popPush("string");
            renderer.renderSectionLayer(RenderType.tripwire(), d0, d1, d2, frustumMatrix, projectionMatrix);
            renderer.particlesTarget.clear(Minecraft.ON_OSX);
            renderer.particlesTarget.copyDepthFrom(renderer.minecraft.getMainRenderTarget());
            RenderStateShard.PARTICLES_TARGET.setupRenderState();
            profilerfiller.popPush("particles");
            renderer.minecraft.particleEngine.render(lightTexture, camera, f, frustum, type -> true);
            net.neoforged.neoforge.client.ClientHooks.dispatchRenderStage(net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage.AFTER_PARTICLES, renderer, posestack, frustumMatrix, projectionMatrix, renderer.ticks, camera, frustum);
            RenderStateShard.PARTICLES_TARGET.clearRenderState();
        } else {
            // Neo: render solid particles before translucent geometry to match order of chunk render types, fixes solid particles disappearing underwater in Fast/Fancy (MC-161917)
            profilerfiller.popPush("solid_particles");
            renderer.minecraft.particleEngine.render(lightTexture, camera, f, frustum, type -> !type.isTranslucent());
            profilerfiller.popPush("translucent");
            if (renderer.translucentTarget != null) {
                renderer.translucentTarget.clear(Minecraft.ON_OSX);
            }

            renderer.renderSectionLayer(ModRenderTypes.COLORED_LIGHT_TRANSLUCENT, d0, d1, d2, frustumMatrix, projectionMatrix);
            renderer.renderSectionLayer(RenderType.translucent(), d0, d1, d2, frustumMatrix, projectionMatrix);
            multibuffersource$buffersource.endBatch(RenderType.lines());
            multibuffersource$buffersource.endBatch();
            profilerfiller.popPush("string");
            renderer.renderSectionLayer(RenderType.tripwire(), d0, d1, d2, frustumMatrix, projectionMatrix);
            profilerfiller.popPush("particles");
            renderer.minecraft.particleEngine.render(lightTexture, camera, f, frustum, type -> type.isTranslucent()); // Neo: only render translucent particles at renderer stage
            net.neoforged.neoforge.client.ClientHooks.dispatchRenderStage(net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage.AFTER_PARTICLES, renderer, posestack, frustumMatrix, projectionMatrix, renderer.ticks, camera, frustum);
        }

        if (renderer.minecraft.options.getCloudsType() != CloudStatus.OFF) {
            if (renderer.transparencyChain != null) {
                renderer.cloudsTarget.clear(Minecraft.ON_OSX);
            }

            profilerfiller.popPush("clouds");
            renderer.renderClouds(posestack, frustumMatrix, projectionMatrix, f, d0, d1, d2);
        }

        if (renderer.transparencyChain != null) {
            RenderStateShard.WEATHER_TARGET.setupRenderState();
            profilerfiller.popPush("weather");
            renderer.renderSnowAndRain(lightTexture, f, d0, d1, d2);
            net.neoforged.neoforge.client.ClientHooks.dispatchRenderStage(net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage.AFTER_WEATHER, renderer, posestack, frustumMatrix, projectionMatrix, renderer.ticks, camera, frustum);
            renderer.renderWorldBorder(camera);
            RenderStateShard.WEATHER_TARGET.clearRenderState();
            renderer.transparencyChain.process(deltaTracker.getGameTimeDeltaTicks());
            renderer.minecraft.getMainRenderTarget().bindWrite(false);
        } else {
            RenderSystem.depthMask(false);
            profilerfiller.popPush("weather");
            renderer.renderSnowAndRain(lightTexture, f, d0, d1, d2);
            net.neoforged.neoforge.client.ClientHooks.dispatchRenderStage(net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage.AFTER_WEATHER, renderer, posestack, frustumMatrix, projectionMatrix, renderer.ticks, camera, frustum);
            renderer.renderWorldBorder(camera);
            RenderSystem.depthMask(true);
        }

        renderer.renderDebug(posestack, multibuffersource$buffersource, camera);
        multibuffersource$buffersource.endLastBatch();
        matrix4fstack.popMatrix();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        FogRenderer.setupNoFog();
        ci.cancel();
    }
}
