package jme3test.gi;

import com.jme3.app.SimpleApplication;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.material.MaterialDef;
import com.jme3.math.Vector3f;
import com.jme3.renderer.gi.Lpv;
import com.jme3.renderer.gi.RSMInjection;
import com.jme3.renderer.gi.VplVisualisation;
import com.jme3.scene.*;
import com.jme3.texture.Texture2D;
import com.jme3.ui.Picture;
import com.jme3.util.BufferUtils;
import sun.security.provider.certpath.Vertex;

/**
 *
 * @author JhonKkk
 */
public class TestRSM extends SimpleApplication{
    int frame = 0;
    RSMInjection rsmInjection;
    Lpv lpv;
    DirectionalLight directionalLight;
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
        Spatial scene = assetManager.loadModel("Models/sponza_with_teapot/sponza_with_teapot.j3o");
        rootNode.attachChild(scene);

        Node mainCamera = (Node) ((Node)scene).getChild("mainCamera");
        cam.setLocation(mainCamera.getWorldTranslation());
        cam.setRotation(mainCamera.getWorldRotation());
        cam.setFrustumPerspective(45.0f, cam.getWidth() * 1.0f / cam.getHeight(), 0.01f, 100.0f);
        directionalLight = (DirectionalLight) scene.getLocalLightList().get(0);

        flyCam.setMoveSpeed(20.0f);

        rsmInjection = new RSMInjection(directionalLight, 512);
        lpv = new Lpv(512, 32, renderManager);
        Node vpls = VplVisualisation.visualizeVpl(2.25f, 32);
//        rootNode.attachChild(vpls);


        Geometry fullTriangle = createFullscreenTriangle();
        Material fullMat = new Material((MaterialDef) assetManager.loadAsset("Common/MatDefs/Lpv/TextureBlit.j3md"));
        fullTriangle.setMaterial(fullMat);
//        fullMat.setTexture("Texture", (Texture2D) lpv.getInjectionMRTData().getRt0().getTexture());
        fullMat.setTexture("Texture", (Texture2D) lpv.getAccumulatedMRTData().getRt0().getTexture());
        rootNode.attachChild(fullTriangle);
//        Picture p = new Picture("Debug");
//        p.move(0,0,-1);
//        p.setPosition(100, 100);
//        p.setWidth(512);
//        p.setHeight(512);
//        p.setTexture(assetManager, (Texture2D) lpv.getInjectionMRTData().getRt0().getTexture(), false);
////        p.setTexture(assetManager, rsmInjection.getColorBuffer(), false);
//        guiNode.attachChild(p);
    }

    @Override
    public void simpleUpdate(float tpf) {
        if(frame >= 0){
            rsmInjection.injection(rootNode, renderManager);
            lpv.clearInjectionBuffer();
            lpv.clearAccumulatedBuffer();
            lpv.lightInjection(rsmInjection);
            lpv.geometryInjection(rsmInjection, directionalLight.getDirection());
            lpv.lightPropagation(1);
        }
        frame++;
        super.simpleUpdate(tpf);
    }

    public static void main(String[] args) {
        TestRSM testRSM = new TestRSM();
        testRSM.start();
    }

}
