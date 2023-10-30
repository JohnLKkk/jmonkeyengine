package jme3test.pipeline;

import com.jme3.app.SimpleApplication;
import com.jme3.input.KeyInput;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.material.MaterialDef;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.FGBuilderTool;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.framegraph.FGGlobal;
import com.jme3.renderer.framegraph.FGRenderContext;
import com.jme3.renderer.framegraph.FrameGraph;
import com.jme3.renderer.pass.*;
import com.jme3.scene.Geometry;
import com.jme3.system.AppSettings;
import com.jme3.util.TangentBinormalGenerator;
import jme3test.renderpath.RenderPathHelper;

/**
 * Custom SimplePipeline, this example demonstrates the basic usage of CustomPipeline by customizing a GBufferPass.
 * @author JhonKkk
 */
public class TestSimplePipeline extends SimpleApplication {
    private CustomGBufferPass customGBufferPass;
    @Override
    public void simpleUpdate(float tpf) {
        super.simpleUpdate(tpf);
    }

    private final void setupScene(){
        Geometry teapot = (Geometry) assetManager.loadModel("Models/Teapot/Teapot.obj");
        TangentBinormalGenerator.generate(teapot.getMesh(), true);

        teapot.setLocalScale(2f);
        renderManager.setSinglePassLightBatchSize(1);
        Material mat = new Material(assetManager, "jme3test/materials/MyCustomLighting.j3md");
        mat.setFloat("Shininess", 25);
        cam.setLocation(new Vector3f(0.015041917f, 0.4572918f, 5.2874837f));
        cam.setRotation(new Quaternion(-1.8875003E-4f, 0.99882424f, 0.04832061f, 0.0039016632f));

        mat.setColor("Ambient",  ColorRGBA.Black);
        mat.setColor("Diffuse",  ColorRGBA.Gray);
        mat.setColor("Specular", ColorRGBA.Gray);

        teapot.setMaterial(mat);
        rootNode.attachChild(teapot);

        DirectionalLight dl = new DirectionalLight();
        dl.setDirection(new Vector3f(-1, -1, -1).normalizeLocal());
        dl.setColor(ColorRGBA.White);
        rootNode.addLight(dl);
    }

    private final void setupCustomFrameGraph(){
        DeferredShadingPass defaultDeferredShadingPass = FGBuilderTool.findPass(DeferredShadingPass.class);
        TileDeferredShadingPass defaultTileDeferredShadingPass = FGBuilderTool.findPass(TileDeferredShadingPass.class);
        // Redefine the overloaded material definition used by the custom ShadingModel here.
        if(defaultDeferredShadingPass != null){
            defaultDeferredShadingPass.setOverlyMat(new Material((MaterialDef) assetManager.loadAsset("jme3test/materials/MyCustomDeferredShading.j3md")));
            defaultDeferredShadingPass.reset();
            defaultDeferredShadingPass.getSinks().clear();
            defaultDeferredShadingPass.getBinds().clear();
            defaultDeferredShadingPass.init();
        }
        if(defaultTileDeferredShadingPass != null){
            defaultTileDeferredShadingPass.setOverlyMat(new Material((MaterialDef) assetManager.loadAsset("jme3test/materials/MyCustomTileBasedDeferredShading.j3md")));
            defaultTileDeferredShadingPass.reset();
            defaultTileDeferredShadingPass.getSinks().clear();
            defaultTileDeferredShadingPass.getBinds().clear();
            defaultTileDeferredShadingPass.init();
        }
        customGBufferPass = new CustomGBufferPass();
        // Override the default GBufferPass
        FGBuilderTool.registerPass(GBufferPass.class, customGBufferPass);
        flyCam.setMoveSpeed(10.0f);
        renderManager.setRenderPath(RenderManager.RenderPath.Deferred);
        new RenderPathHelper(this, new Vector3f(10, cam.getHeight() - 10, 0), KeyInput.KEY_SPACE, "SPACE");
    }

    @Override
    public void simpleInitApp() {
        setupScene();
        setupCustomFrameGraph();
    }

    public static void main(String[] args) {
        TestCustomPipeline testCustomPipeline = new TestCustomPipeline();
        testCustomPipeline.start();
    }
}
