package com.jme3.light.gi;

import com.jme3.asset.AssetManager;
import com.jme3.asset.plugins.ClasspathLocator;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.material.Material;
import com.jme3.material.MaterialDef;
import com.jme3.material.plugins.J3MLoader;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.instancing.InstancedNode;
import com.jme3.scene.shape.Sphere;
import com.jme3.shader.plugins.GLSLLoader;
import com.jme3.system.JmeSystem;

/**
 * LightProbeVolumeVisualize
 * <p>
 * @author JohnKkk
 */
public class LightProbeVolumeVisualize {
    private static AssetManager assetManager;
    private static MaterialDef lightProbeDebugMaterialDef;

    private static void initAssetManager(){
        assetManager = JmeSystem.newAssetManager();
        assetManager.registerLocator(".", FileLocator.class);
        assetManager.registerLocator("/", ClasspathLocator.class);
        assetManager.registerLoader(J3MLoader.class, "j3m");
        assetManager.registerLoader(J3MLoader.class, "j3md");
        assetManager.registerLoader(GLSLLoader.class, "vert", "frag","geom","tsctrl","tseval","glsllib","glsl");
    }

    public static final Spatial generateLightProbeVolumeDebugGeometry(LightProbeVolume lightProbeVolume){
        if(true){
            InstancedNode instancedNode = new InstancedNode("lightProbeVolumeDebug");
            instancedNode.setShadowMode(RenderQueue.ShadowMode.Off);
            Geometry spGeo = new Geometry("point_light_cull_box", new Sphere(10, 10, 0.1f));
            if(assetManager == null){
                initAssetManager();
                lightProbeDebugMaterialDef = (MaterialDef) assetManager.loadAsset("Common/MatDefs/Misc/Unshaded.j3md");
            }
            Material spMat = new Material(lightProbeDebugMaterialDef);
            spMat.setColor("Color", ColorRGBA.White);
            spMat.setBoolean("UseInstancing", true);
            spGeo.setMaterial(spMat);

            for(Vector3f p : lightProbeVolume.getProbeLocations()){
                Geometry b = spGeo.clone(false);
                b.setLocalTranslation(p);
                instancedNode.attachChild(b);
            }
            instancedNode.instance();
            return instancedNode;
        }
        return null;
    }
}
