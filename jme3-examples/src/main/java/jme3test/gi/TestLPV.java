package jme3test.gi;

import com.jme3.app.SimpleApplication;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.material.MaterialDef;
import com.jme3.material.TechniqueDef;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.FXAAFilter;
import com.jme3.post.filters.ToneMapFilter;
import com.jme3.post.lpv.LPVGIFilter;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.gi.Lpv;
import com.jme3.renderer.gi.MRTData;
import com.jme3.renderer.gi.RSMInjection;
import com.jme3.renderer.gi.VplVisualisation;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.*;
import com.jme3.shadow.DirectionalLightShadowFilter;
import com.jme3.texture.Texture2D;
import com.jme3.util.BufferUtils;

/**
 *
 * @author JhonKkk
 */
public class TestLPV extends SimpleApplication{
    int frame = 0;
    RSMInjection rsmInjection;
    Lpv lpv;
    DirectionalLight directionalLight;
    Vector3f dir;
    LPVGIFilter lpvgiFilter;
    Spatial scene;
    Node vpls;
    boolean rotateLight = false;
    int lpvGridSize = 32;
    private final Geometry createFullscreenTriangle(){
        Mesh triangle = new Mesh();
        Vector3f positions[] = new Vector3f[3];
        positions[0] = new Vector3f(-1, -1, 0);
        positions[1] = new Vector3f(3, -1, 0);
        positions[2] = new Vector3f(-1, 3, 0);
        triangle.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(positions));
        Geometry geo = new Geometry("triangle", triangle);
        geo.setCullHint(Spatial.CullHint.Never);
        return geo;
    }

    @Override
    public void simpleInitApp() {
        renderManager.setRenderPath(RenderManager.RenderPath.Deferred);
        renderManager.setPreferredLightMode(TechniqueDef.LightMode.SinglePass);
        scene = assetManager.loadModel("Models/test.j3o");
        for(Spatial n : ((Node)scene).getChildren()){
            if(n instanceof Geometry){
                Geometry geo = (Geometry)n;
                geo.getMaterial().setBoolean("UseLpv", true);
            }
            else if(n instanceof Node){
                for(Spatial n2 : ((Node) n).getChildren()){
                    if(n2 instanceof Geometry){
                        Geometry geo = ((Geometry)n2);
                        geo.getMaterial().setBoolean("UseLpv", true);
                    }
                }
            }
        }
        Geometry cell = (Geometry) ((Node)scene).getChild("cell");
//        scene.setLocalTranslation(0, -10, 0);
        scene.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        rootNode.attachChild(scene);
        AmbientLight ambientLight = new AmbientLight(new ColorRGBA(0.1f, 0.1f, 0.1f, 0.1f));
        rootNode.addLight(ambientLight);

        Node mainCamera = (Node) ((Node)scene).getChild("mainCamera");
        cam.setLocation(mainCamera.getWorldTranslation());
        cam.setRotation(mainCamera.getWorldRotation());
        cam.setFrustumPerspective(70.0f, cam.getWidth() * 1.0f / cam.getHeight(), 0.01f, 100.0f);
        directionalLight = (DirectionalLight) scene.getLocalLightList().get(0);
//        directionalLight.setColor(new ColorRGBA(1.25f, 1.0f, 1.0f, 1.0f));
        dir = directionalLight.getDirection().clone();
//        directionalLight.setDirection(new Vector3f(0, -1, 0));

        flyCam.setDragToRotate(true);
        flyCam.setMoveSpeed(20.0f);

        rsmInjection = new RSMInjection(directionalLight, 512);
        rsmInjection.setInjectionFactor(13.0f);
        lpv = new Lpv(512, lpvGridSize, renderManager);
        vpls = VplVisualisation.visualizeVpl(2.25f, lpvGridSize);
        vpls.setQueueBucket(RenderQueue.Bucket.Opaque);
        vpls.setCullHint(Spatial.CullHint.Always);
        rootNode.attachChild(vpls);

        FilterPostProcessor filterPostProcessor = new FilterPostProcessor(assetManager);
        DirectionalLightShadowFilter directionalLightShadowFilter = new DirectionalLightShadowFilter(assetManager, 4096, 1);
        directionalLightShadowFilter.setLight(directionalLight);
        directionalLightShadowFilter.setShadowIntensity(1.0f);
        directionalLightShadowFilter.setShadowZExtend(5000.0f);
        directionalLightShadowFilter.setShadowZFadeLength(1000.0f);
        filterPostProcessor.addFilter(directionalLightShadowFilter);
        lpvgiFilter = new LPVGIFilter();
        lpvgiFilter.setEnabled(false);
        filterPostProcessor.addFilter(lpvgiFilter);
        FXAAFilter fxaaFilter = new FXAAFilter();
        filterPostProcessor.addFilter(fxaaFilter);
        ToneMapFilter toneMapFilter = new ToneMapFilter();
//        toneMapFilter.setWhitePoint(new Vector3f(2.4f, 2.4f, 2.4f));
        filterPostProcessor.addFilter(toneMapFilter);
//        SSAOFilter ssaoFilter = new SSAOFilter(0.1f, 2.5f, 2.2f, 0.0f);
//        ssaoFilter.setApproximateNormals(true);
//        filterPostProcessor.addFilter(ssaoFilter);
        viewPort.addProcessor(filterPostProcessor);


        Geometry fullTriangle = createFullscreenTriangle();
        Material fullMat = new Material((MaterialDef) assetManager.loadAsset("Common/MatDefs/Lpv/TextureBlit.j3md"));
        fullTriangle.setMaterial(fullMat);
//        fullMat.setTexture("Texture", (Texture2D) lpv.getInjectionMRTData().getRt0().getTexture());
        fullMat.setTexture("Texture", (Texture2D) lpv.getAccumulatedMRTData().getRt0().getTexture());
//        rootNode.attachChild(fullTriangle);
//        Picture p = new Picture("Debug");
//        p.move(0,0,-1);
//        p.setPosition(100, 100);
//        p.setWidth(512);
//        p.setHeight(512);
//        p.setTexture(assetManager, (Texture2D) lpv.getInjectionMRTData().getRt0().getTexture(), false);
////        p.setTexture(assetManager, rsmInjection.getColorBuffer(), false);
//        guiNode.attachChild(p);
        inputManager.addMapping("rotateLight", new KeyTrigger(KeyInput.KEY_G));
        inputManager.addMapping("showVPLs", new KeyTrigger(KeyInput.KEY_P));
        inputManager.addMapping("enableLPV", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("close", new KeyTrigger(KeyInput.KEY_1));
        inputManager.addMapping("open", new KeyTrigger(KeyInput.KEY_2));
        Vector3f c = new Vector3f();
        inputManager.addListener(new ActionListener() {
            @Override
            public void onAction(String name, boolean isPressed, float tpf) {
                if(name.equals("rotateLight") && isPressed){
                    rotateLight = !rotateLight;
                }
                else if(name.equals("enableLPV") && isPressed){
                    lpvgiFilter.setEnabled(!lpvgiFilter.isEnabled());
                }
                else if(name.equals("showVPLs") && isPressed){
                    vpls.setCullHint(vpls.getCullHint() == Spatial.CullHint.Always ? Spatial.CullHint.Never : Spatial.CullHint.Always);
                }
                else if(name.equals("close") && isPressed){
                    c.x += 1.0f;
                }
                else if(name.equals("open") && isPressed){
                    c.x -= 1.0f;
                }
                if(c.x <= -100.0f){
                    c.x = -100.0f;
                }
                else if(c.x >= 0.0f){
                    c.x = 0.0f;
                }
                cell.setLocalTranslation(c);
            }
        }, "rotateLight", "enableLPV", "showVPLs", "close", "open");
    }
    float angleY = 1.0f;
    Quaternion q1 = new Quaternion();
    Quaternion q2 = new Quaternion();

    @Override
    public void simpleUpdate(float tpf) {
        if(frame >= 1){
            if(rotateLight){
                q1.fromAngles(0, angleY * tpf, 0);
                q2.multLocal(q1);
                directionalLight.setDirection(q2.mult(dir));
            }
            RenderManager.RenderPath currentRenderPath = renderManager.getRenderPath();
            renderManager.setRenderPath(RenderManager.RenderPath.Forward);
            rsmInjection.injection(rootNode, renderManager);
            lpv.clearInjectionBuffer();
            lpv.clearAccumulatedBuffer();
            lpv.lightInjection(rsmInjection);
            lpv.geometryInjection(rsmInjection, directionalLight.getDirection());
            lpv.lightPropagation(64);
            renderManager.setRenderPath(currentRenderPath);
            if(frame == 1){
                MRTData acc = lpv.getAccumulatedMRTData();
                lpvgiFilter.setTexture("Context_InGBuff0", renderManager.getgBufferPass().getgBufferData0());
                lpvgiFilter.setTexture("Context_InGBuff2", renderManager.getgBufferPass().getgBufferData2());
                lpvgiFilter.setTexture("Context_InGBuff4", renderManager.getgBufferPass().getgBufferData4());
                lpvgiFilter.setTexture("RedIndirectLight", acc.getRt0().getTexture());
                lpvgiFilter.setTexture("GreenIndirectLight", acc.getRt1().getTexture());
                lpvgiFilter.setTexture("BlueIndirectLight", acc.getRt2().getTexture());
                lpvgiFilter.setLpvGridSize(lpvGridSize);
                lpvgiFilter.setIndirectLightAttenuation(1.0f);
                lpvgiFilter.setEnabled(true);
            }
//            if(frame >= 0){
//                for(Spatial n : s.getChildren()){
//                    if(n instanceof Geometry){
//                        Geometry geo = (Geometry)n;
//                        geo.getMaterial().setBoolean("UseLpvGi", true);
//                        geo.getMaterial().setInt("LpvGridSize", 32);
//                        geo.getMaterial().setFloat("IndirectLightAttenuation", 1.0f);
//                        MRTData acc = lpv.getAccumulatedMRTData();
//                        geo.getMaterial().setTexture("RedIndirectLight", acc.getRt0().getTexture());
//                        geo.getMaterial().setTexture("GreenIndirectLight", acc.getRt1().getTexture());
//                        geo.getMaterial().setTexture("BlueIndirectLight", acc.getRt2().getTexture());
//                    }
//                }
//            }
        }
        frame++;
        super.simpleUpdate(tpf);
    }

    public static void main(String[] args) {
        TestLPV testRSM = new TestLPV();
        testRSM.start();
    }

}
