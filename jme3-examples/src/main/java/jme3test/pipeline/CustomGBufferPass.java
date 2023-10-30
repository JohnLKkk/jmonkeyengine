package jme3test.pipeline;

import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.framegraph.FGFramebufferSource;
import com.jme3.renderer.framegraph.FGRenderTargetSource;
import com.jme3.renderer.framegraph.FGVarSource;
import com.jme3.renderer.pass.DeferredLightDataSource;
import com.jme3.renderer.pass.GBufferPass;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;

/**
 * This code demonstrates how to define a custom GBufferPass.
 * @author JhonKkk
 */
public class CustomGBufferPass extends GBufferPass {
    CustomGBufferPass(){
        super("CustomGBufferPass");
    }
    @Override
    public void reshape(Renderer renderer, ViewPort vp, int w, int h) {
        boolean recreate = false;
        if(gBuffer != null){
            if(frameBufferWidth != w || frameBufferHeight != h){
                gBuffer.dispose();
                gBuffer.deleteObject(renderer);

                frameBufferWidth = w;
                frameBufferHeight = h;

                recreate = true;
            }
        }
        else{
            recreate = true;
            frameBufferWidth = w;
            frameBufferHeight = h;
        }

        if(recreate){
            // recreate
            gBufferData0 = new Texture2D(w, h, Image.Format.RGBA16F);
            gBufferData1 = new Texture2D(w, h, Image.Format.RGBA16F);
            this.getSinks().clear();
            gBufferData4 = new Texture2D(w, h, Image.Format.Depth);
            gBuffer = new FrameBuffer(w, h, 1);
            FrameBuffer.FrameBufferTextureTarget rt0 = FrameBuffer.FrameBufferTarget.newTarget(gBufferData0);
            FrameBuffer.FrameBufferTextureTarget rt1 = FrameBuffer.FrameBufferTarget.newTarget(gBufferData1);
            FrameBuffer.FrameBufferTextureTarget rt4 = FrameBuffer.FrameBufferTarget.newTarget(gBufferData4);
            gBuffer.addColorTarget(rt0);
            gBuffer.addColorTarget(rt1);
            gBuffer.setDepthTarget(rt4);
            gBuffer.setMultiTarget(true);
            registerSource(new FGRenderTargetSource(S_RT_0, rt0));
            registerSource(new FGRenderTargetSource(S_RT_1, rt1));
            registerSource(new FGRenderTargetSource(S_RT_4, rt4));
            registerSource(new DeferredLightDataSource(S_LIGHT_DATA, lightData));
            bHasDrawVarSource = new FGVarSource<Boolean>(S_EXECUTE_STATE, bHasDraw);
            registerSource(bHasDrawVarSource);
            registerSource(new FGFramebufferSource(S_FB, gBuffer));
        }
    }
}
