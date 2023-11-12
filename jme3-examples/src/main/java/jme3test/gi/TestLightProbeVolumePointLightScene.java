package jme3test.gi;

import com.jme3.app.SimpleApplication;
import com.jme3.environment.generation.JobProgressAdapter;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.Light;
import com.jme3.light.LightList;
import com.jme3.light.PointLight;
import com.jme3.light.gi.LightProbeVolume;
import com.jme3.light.gi.LightProbeVolumeBake;
import com.jme3.light.gi.LightProbeVolumeVisualize;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.system.AppSettings;
import com.sun.management.HotSpotDiagnosticMXBean;

import java.lang.management.ManagementFactory;

public class TestLightProbeVolumePointLightScene extends SimpleApplication {
    private int frame = 0;
    LightProbeVolume lightProbeVolume;
    private Material octahedralDebugMat;
    private int probeIndex = 0;
    @Override
    public void simpleInitApp() {
        Node scene = (Node) assetManager.loadModel("Scenes/ManyLights/Main.scene");
        rootNode.attachChild(scene);
        Node n = (Node) rootNode.getChild(0);
        final LightList lightList = n.getWorldLightList();
        final Geometry g = (Geometry) n.getChild("Grid-geom-1");

        g.getMaterial().setColor("Ambient", new ColorRGBA(0.2f, 0.2f, 0.2f, 1f));
        g.getMaterial().setBoolean("VertexLighting", false);

        int nb = 0;
        for (Light light : lightList) {
            nb++;
            PointLight p = (PointLight) light;
            if (nb > 7) {
                n.removeLight(light);
            } else {
                int rand = FastMath.nextRandomInt(0, 3);
                switch (rand) {
                    case 0:
                        light.setColor(ColorRGBA.Red.mult(10));
                        break;
                    case 1:
                        light.setColor(ColorRGBA.Yellow.mult(10));
                        break;
                    case 2:
                        light.setColor(ColorRGBA.Green.mult(10));
                        break;
                    case 3:
                        light.setColor(ColorRGBA.Orange.mult(10));
                        break;
                }
            }
//            Geometry b = boxGeo.clone(false);
//            instancedNode.attachChild(b);
//            b.setLocalTranslation(p.getPosition().x, p.getPosition().y, p.getPosition().z);
//            b.setLocalScale(p.getRadius() * 0.5f);

        }

        cam.setLocation(new Vector3f(-180.61f, 64, 7.657533f));
        cam.lookAtDirection(new Vector3f(0.93f, -0.344f, 0.044f), Vector3f.UNIT_Y);

        cam.setLocation(new Vector3f(-26.85569f, 15.701239f, -19.206047f));
        cam.lookAtDirection(new Vector3f(0.13871355f, -0.6151029f, 0.7761488f), Vector3f.UNIT_Y);

        lightProbeVolume = new LightProbeVolume();
        lightProbeVolume.setProbeOrigin(new Vector3f(-37.0f, -1.8f, -37.0f));
        lightProbeVolume.setProbeCount(new Vector3f(6, 4, 6));
        lightProbeVolume.setProbeStep(new Vector3f(15.35f, 6.0f / 2.0f, 15.35f));
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
        TestLightProbeVolumePointLightScene testLightProbeVolume = new TestLightProbeVolumePointLightScene();
        AppSettings settings = new AppSettings(true);
        settings.setRenderer(AppSettings.LWJGL_OPENGL45);
        testLightProbeVolume.setSettings(settings);
        testLightProbeVolume.start();
    }
}
