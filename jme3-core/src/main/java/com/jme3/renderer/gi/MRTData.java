package com.jme3.renderer.gi;

import com.jme3.texture.FrameBuffer;

public class MRTData {
    private FrameBuffer frameBuffer;
    private FrameBuffer.FrameBufferTextureTarget rt0;
    private FrameBuffer.FrameBufferTextureTarget rt1;
    private FrameBuffer.FrameBufferTextureTarget rt2;

    public MRTData(FrameBuffer frameBuffer, FrameBuffer.FrameBufferTextureTarget rt0, FrameBuffer.FrameBufferTextureTarget rt1, FrameBuffer.FrameBufferTextureTarget rt2) {
        this.frameBuffer = frameBuffer;
        this.rt0 = rt0;
        this.rt1 = rt1;
        this.rt2 = rt2;
    }

    public FrameBuffer getFrameBuffer() {
        return frameBuffer;
    }

    public FrameBuffer.FrameBufferTextureTarget getRt0() {
        return rt0;
    }

    public FrameBuffer.FrameBufferTextureTarget getRt1() {
        return rt1;
    }

    public FrameBuffer.FrameBufferTextureTarget getRt2() {
        return rt2;
    }
}
