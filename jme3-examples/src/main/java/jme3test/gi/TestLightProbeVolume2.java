package jme3test.gi;

import com.jme3.app.SimpleApplication;
import com.jme3.environment.generation.JobProgressAdapter;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.light.gi.LightProbeVolume;
import com.jme3.light.gi.LightProbeVolumeBake;
import com.jme3.light.gi.LightProbeVolumeVisualize;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Quad;
import com.jme3.system.AppSettings;
import com.sun.management.HotSpotDiagnosticMXBean;

import java.lang.management.ManagementFactory;

public class TestLightProbeVolume2 extends SimpleApplication {
    private int frame = 0;
    LightProbeVolume lightProbeVolume;
    private Material octahedralDebugMat;
    private int probeIndex = 0;
    @Override
    public void simpleInitApp() {
        /* A colored lit cube. Needs light source! */
        Box boxMesh = new Box(1f,1f,1f);
        Geometry boxGeo = new Geometry("Colored Box", boxMesh);
        Material boxMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        boxMat.setColor("Color", ColorRGBA.Blue);
        boxGeo.setMaterial(boxMat);
        boxGeo.setLocalTranslation(-3, 0, 0);
        rootNode.attachChild(boxGeo);

        Geometry box2 = boxGeo.clone(true);
        box2.getMaterial().setColor("Color", ColorRGBA.Red);
        box2.setLocalTranslation(3, 0, 0);
        rootNode.attachChild(box2);

        Quad quad = new Quad(20, 20);
        Geometry floor = new Geometry("floor", quad);
        floor.setLocalRotation(new Quaternion(new float[]{(float)(Math.toRadians(-90.0f)), 0, 0}));
        floor.setLocalTranslation(-10, -1, 10);
        Material floorMat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        floorMat.setBoolean("UseMaterialColors", true);
        floorMat.setColor("Ambient", new ColorRGBA(0.2f, 0.2f, 0.2f, 1.0f));
        floorMat.setColor("Diffuse", new ColorRGBA(0.5f, 0.5f, 0.5f, 1.0f));
        floor.setMaterial(floorMat);
        rootNode.attachChild(floor);

        Geometry wall = new Geometry("wall", boxMesh.clone());
        wall.setLocalScale(0.2f, 3.0f, 6.0f);
        wall.setLocalTranslation(0, 1.5f, 0);
        wall.setMaterial(floorMat.clone());
        rootNode.attachChild(wall);

//        /** A white ambient light source. */
//        AmbientLight ambient = new AmbientLight();
//        ambient.setColor(ColorRGBA.White);
//        rootNode.addLight(ambient);

        /** A white, directional light source */
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection((new Vector3f(-0.5f, -0.5f, -0.5f)).normalizeLocal());
        sun.setColor(ColorRGBA.White);
        rootNode.addLight(sun);

        lightProbeVolume = new LightProbeVolume();
        lightProbeVolume.setProbeOrigin(new Vector3f(-10.0f, 0.1f, -7.0f));
        lightProbeVolume.setProbeCount(new Vector3f(8, 4, 8));
        lightProbeVolume.setProbeStep(new Vector3f(2.2f, 6.0f / 3.0f, 2.35f));
        lightProbeVolume.placeProbes();
        rootNode.addLight(lightProbeVolume);
//        Spatial debugLightProbeVolume = LightProbeVolumeVisualize.generateLightProbeVolumeDebugGeometry(lightProbeVolume);
//        rootNode.attachChild(debugLightProbeVolume);

        LightProbeVolumeBake lightProbeVolumeBake = new LightProbeVolumeBake();
        stateManager.attach(lightProbeVolumeBake);

        inputManager.addMapping("up", new KeyTrigger(KeyInput.KEY_1));
        inputManager.addMapping("down", new KeyTrigger(KeyInput.KEY_2));
        inputManager.addMapping("addMultiplier", new KeyTrigger(KeyInput.KEY_3));
        inputManager.addMapping("subMultiplier", new KeyTrigger(KeyInput.KEY_4));
        inputManager.addMapping("toggle", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addListener(new ActionListener() {
            @Override
            public void onAction(String name, boolean isPressed, float tpf) {
                if(name.equals("toggle") && !isPressed){
                    lightProbeVolume.setEnabled(!lightProbeVolume.isEnabled());
                }
                if(name.equals("addMultiplier")){
                    lightProbeVolume.setIndirectMultiplier(lightProbeVolume.getIndirectMultiplier() + 0.1f);
                }
                if(name.equals("subMultiplier")){
                    lightProbeVolume.setIndirectMultiplier(lightProbeVolume.getIndirectMultiplier() - 0.1f);
                }
                if(name.equals("up") && !isPressed){
                    probeIndex++;
                }
                else if(name.equals("down") && !isPressed){
                    probeIndex--;
                }
                if(probeIndex < 0){
                    probeIndex = 0;
                }
                else if(probeIndex > 256){
                    probeIndex = 255;
                }
//                octahedralDebugMat.setInt("ProbeIndex", probeIndex);
            }
        }, "up", "down", "addMultiplier", "subMultiplier", "toggle");
        flyCam.setMoveSpeed(20.0f);
    }

    @Override
    public void simpleUpdate(float tpf) {
        super.simpleUpdate(tpf);
        if(frame == 2){
            // d
            LightProbeVolumeBake lightProbeVolumeBake = stateManager.getState(LightProbeVolumeBake.class);
            if(lightProbeVolumeBake != null){
                lightProbeVolumeBake.bakeLightProbeVolume(rootNode, lightProbeVolume, new JobProgressAdapter<LightProbeVolume>() {
                    @Override
                    public void done(LightProbeVolume result) {
                        System.out.println("bake done!");
//                        octahedralDebugMat.setTexture("OctahedralData", result.getProbeOctahedralFilteredDistances());
                        Spatial debugLightProbeVolume = LightProbeVolumeVisualize.generateLightProbeVolumeDebugGeometry(lightProbeVolume);
                        rootNode.attachChild(debugLightProbeVolume);
                        rootNode.updateGeometricState();
                    }
                });
            }
        }
        frame++;
    }

    public static void main(String[] args) {
        final HotSpotDiagnosticMXBean hsdiag = ManagementFactory
                .getPlatformMXBean(HotSpotDiagnosticMXBean.class);
        if (hsdiag != null) {
            System.out.println("MaxDirectMemorySize:" + hsdiag.getVMOption("MaxDirectMemorySize"));
        }
        TestLightProbeVolume2 testLightProbeVolume = new TestLightProbeVolume2();
        AppSettings settings = new AppSettings(true);
        settings.setRenderer(AppSettings.LWJGL_OPENGL45);
        testLightProbeVolume.setSettings(settings);
        testLightProbeVolume.start();
    }
}
