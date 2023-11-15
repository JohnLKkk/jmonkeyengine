package com.jme3.post.gi;

import com.jme3.asset.AssetManager;
import com.jme3.light.gi.LightProbeVolume;
import com.jme3.material.Material;
import com.jme3.post.Filter;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Texture;

public class LightProbeVolumeFilter extends Filter {
    private static String _g_ApplyGI = "UseIrradianceProbesGi";
    private static String _g_DiffuseGIIntensity = "DiffuseGIIntensity";
    private static String _g_ProbeCounts = "ProbeCounts";
    private static String _g_ProbeStartPosition = "ProbeStartPosition";
    private static String _g_ProbeStep = "ProbeStep";
    private static String _g_IrradianceProbeGrid = "IrradianceProbeGrid";
    private static String _g_MeanDistProbeGrid = "MeanDistProbeGrid";
    private Material irradianceProbesGIMat;
    private LightProbeVolume lightProbeVolume;
    @Override
    protected void initFilter(AssetManager manager, RenderManager renderManager, ViewPort vp, int w, int h) {
        irradianceProbesGIMat = new Material(manager, "Common/MatDefs/LightProbeVolume/LightProbeVolume.j3md");
        irradianceProbesGIMat.setBoolean(_g_ApplyGI, false);
    }

    @Override
    protected void postFrame(RenderManager renderManager, ViewPort viewPort, FrameBuffer prevFilterBuffer, FrameBuffer sceneBuffer) {
        super.postFrame(renderManager, viewPort, prevFilterBuffer, sceneBuffer);
        if(lightProbeVolume != null){
            irradianceProbesGIMat.setFloat(_g_DiffuseGIIntensity, lightProbeVolume.getIndirectMultiplier());
        }
    }

    public void setLightProbeVolume(LightProbeVolume lightProbeVolume) {
        this.lightProbeVolume = lightProbeVolume;
        if(lightProbeVolume != null){
            irradianceProbesGIMat.setVector3(_g_ProbeStartPosition, lightProbeVolume.getProbeOrigin());
            irradianceProbesGIMat.setVector3(_g_ProbeCounts, lightProbeVolume.getProbeCount());
            irradianceProbesGIMat.setVector3(_g_ProbeStep, lightProbeVolume.getProbeStep());
            irradianceProbesGIMat.setFloat(_g_DiffuseGIIntensity, lightProbeVolume.getIndirectMultiplier());
            irradianceProbesGIMat.setTexture(_g_IrradianceProbeGrid, lightProbeVolume.getProbeOctahedralIrradiances());
            irradianceProbesGIMat.setTexture(_g_MeanDistProbeGrid, lightProbeVolume.getProbeOctahedralFilteredDistances());
            irradianceProbesGIMat.setBoolean(_g_ApplyGI, true);
        }
    }

    @Override
    protected boolean isRequiresSceneTexture() {
        return super.isRequiresSceneTexture();
    }

    @Override
    protected Material getMaterial() {
        return irradianceProbesGIMat;
    }
    public final void setTexture(String name, Texture value){
        irradianceProbesGIMat.setTexture(name, value);
    }
    public final void setLpvGridSize(int gridSize){
        irradianceProbesGIMat.setInt("LpvGridSize", gridSize);
    }
    public final void setIndirectLightAttenuation(float attenuation){
        irradianceProbesGIMat.setFloat("IndirectLightAttenuation", attenuation);
    }
}
