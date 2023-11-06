package com.jme3.post.lpv;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.post.Filter;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.texture.Texture;

public class LPVGIFilter extends Filter {
    private Material lpvGIMat;
    @Override
    protected void initFilter(AssetManager manager, RenderManager renderManager, ViewPort vp, int w, int h) {
        lpvGIMat = new Material(manager, "Common/MatDefs/LPV/Lpv.j3md");
        lpvGIMat.setBoolean("UseLpvGi", true);
    }

    @Override
    protected boolean isRequiresSceneTexture() {
        return super.isRequiresSceneTexture();
    }

    @Override
    protected Material getMaterial() {
        return lpvGIMat;
    }
    public final void setTexture(String name, Texture value){
        lpvGIMat.setTexture(name, value);
    }
    public final void setLpvGridSize(int gridSize){
        lpvGIMat.setInt("LpvGridSize", gridSize);
    }
    public final void setIndirectLightAttenuation(float attenuation){
        lpvGIMat.setFloat("IndirectLightAttenuation", attenuation);
    }
}
