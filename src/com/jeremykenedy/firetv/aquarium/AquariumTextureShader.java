package com.jeremykenedy.firetv.aquarium;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/** Textured swimming surfaces with transparent fins and optional moving sunlight. */
final class AquariumTextureShader {
    private static final String VERTEX =
            "attribute vec2 aPosition,aUv;uniform vec4 uPlacement,uUvRect;"
                    + "uniform mediump float uTime;uniform float uMotion,uPhase,uAngle;"
                    + "varying mediump vec2 vUv,vLocal;void main(){vec2 p=aPosition;"
                    + "if(uMotion>1.5){float arms=pow(aUv.y,2.0);"
                    + "p.x+=sin(uTime*1.7+uPhase+aUv.y*8.0)*arms*0.03;"
                    + "p.y+=cos(uTime*1.4+uPhase+aUv.x*7.0)*arms*0.009;}else{"
                    + "float tail=pow(1.0-aUv.x,2.0);"
                    + "p.y+=sin(uTime*4.0+uPhase-aUv.x*4.0)*tail*uMotion*0.025;"
                    + "p.x+=sin(uTime*3.0+uPhase-aUv.y*3.0)*uMotion*0.007;}"
                    + "float c=cos(uAngle),s=sin(uAngle);p=mat2(c,s,-s,c)*p;"
                    + "gl_Position=vec4(uPlacement.xy+p*uPlacement.zw,0.0,1.0);"
                    + "vUv=uUvRect.xy+aUv*uUvRect.zw;vLocal=aUv;}";
    private static final String FRAGMENT =
            "precision mediump float;uniform sampler2D uTexture;uniform vec3 uTint,uWater;uniform"
                + " float uTime,uEffect,uRays,uLook,uFog,uOpacity;varying vec2 vUv,vLocal;void"
                + " main(){vec4 color;if(uEffect>1.5){float"
                + " r=length(vLocal-0.5);if(r>0.48)discard;float"
                + " rim=smoothstep(0.35,0.45,r)*(1.0-smoothstep(0.45,0.48,r));float"
                + " shine=exp(-length(vLocal-vec2(0.33,0.27))*28.0);float"
                + " a=(rim*0.27+shine*0.6)*uOpacity;"
                + "gl_FragColor=vec4(vec3(0.78,0.94,1.0)*a,a);return;}"
                + "color=texture2D(uTexture,vUv);if(color.a<0.025)discard;color.rgb*=uTint;if(uEffect<0.5){float"
                + " vignette=1.0-0.13*pow(length((vLocal-0.5)*1.25),2.0);color.rgb*=vignette;"
                + "if(uLook>0.5&&uLook<1.5){color.rgb*=vec3(0.82,0.95,1.03);float"
                + " edge=min(min(vLocal.x,1.0-vLocal.x),min(vLocal.y,1.0-vLocal.y));"
                + "color.rgb*=mix(0.4,1.0,smoothstep(0.002,0.012,edge));}if(uLook>3.5&&uLook<4.5){float"
                + " gray=dot(color.rgb,vec3(0.3,0.59,0.11));"
                + "color.rgb=mix(vec3(gray),color.rgb,1.12);color.rgb*=vec3(1.02,1.0,0.96);}if(uRays>0.5){float"
                + " beam=pow(max(0.0,sin((vLocal.x+vLocal.y*0.18)*17.0+uTime*0.1)),8.0);"
                + "color.rgb+=vec3(0.035,0.06,0.06)*beam*(1.0-vLocal.y);float"
                + " caustic=sin(vLocal.x*90.0+vLocal.y*32.0+uTime*1.3)"
                + "*sin(vLocal.x*35.0-vLocal.y*67.0-uTime);"
                + "color.rgb+=vec3(0.045,0.05,0.025)*pow(max(0.0,caustic),5.0)"
                + "*smoothstep(0.5,1.0,vLocal.y);}}"
                + "else{color.rgb=mix(color.rgb,uWater*color.a,uFog);"
                + "if(uRays>0.5)color.rgb*=1.0+0.045*sin(uTime*1.3+vLocal.x*7.0);}"
                + "gl_FragColor=color*uOpacity;}";
    private final int program, position, uv, placement, uvRect, time, motion, phase;
    private final int angle, texture, tint, water, effect, rays, look, fog, opacity;
    private final FloatBuffer vertices;
    private final int vertexCount;

    AquariumTextureShader() {
        int vertex = compile(GLES20.GL_VERTEX_SHADER, VERTEX);
        int fragment = compile(GLES20.GL_FRAGMENT_SHADER, FRAGMENT);
        program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertex);
        GLES20.glAttachShader(program, fragment);
        GLES20.glLinkProgram(program);
        int[] status = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0);
        if (status[0] == 0) throw new IllegalStateException(GLES20.glGetProgramInfoLog(program));
        GLES20.glDeleteShader(vertex);
        GLES20.glDeleteShader(fragment);
        position = GLES20.glGetAttribLocation(program, "aPosition");
        uv = GLES20.glGetAttribLocation(program, "aUv");
        placement = location("uPlacement");
        uvRect = location("uUvRect");
        time = location("uTime");
        motion = location("uMotion");
        phase = location("uPhase");
        angle = location("uAngle");
        texture = location("uTexture");
        tint = location("uTint");
        water = location("uWater");
        effect = location("uEffect");
        rays = location("uRays");
        look = location("uLook");
        fog = location("uFog");
        opacity = location("uOpacity");
        int columns = 16, rows = 8;
        vertexCount = columns * rows * 6;
        vertices =
                ByteBuffer.allocateDirect(vertexCount * 4 * 4)
                        .order(ByteOrder.nativeOrder())
                        .asFloatBuffer();
        for (int y = 0; y < rows; y++)
            for (int x = 0; x < columns; x++) {
                point((float) x / columns, (float) y / rows);
                point((float) (x + 1) / columns, (float) y / rows);
                point((float) x / columns, (float) (y + 1) / rows);
                point((float) (x + 1) / columns, (float) y / rows);
                point((float) (x + 1) / columns, (float) (y + 1) / rows);
                point((float) x / columns, (float) (y + 1) / rows);
            }
    }

    private void point(float x, float y) {
        vertices.put(x - 0.5f).put(0.5f - y).put(x).put(y);
    }

    private int location(String name) {
        return GLES20.glGetUniformLocation(program, name);
    }

    private static int compile(int type, String source) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        int[] status = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0);
        if (status[0] == 0) throw new IllegalStateException(GLES20.glGetShaderInfoLog(shader));
        return shader;
    }

    void frame(AquariumOptions settings, float seconds, float[] waterColor) {
        GLES20.glUseProgram(program);
        GLES20.glUniform1i(texture, 0);
        GLES20.glUniform1f(time, seconds % 62.831853f);
        GLES20.glUniform1f(rays, settings.rays ? 1 : 0);
        GLES20.glUniform1f(look, settings.look);
        GLES20.glUniform3fv(water, 1, waterColor, 0);
        if (settings.light == 1) GLES20.glUniform3f(tint, 1.0f, 0.9f, 0.76f);
        else if (settings.light == 2) GLES20.glUniform3f(tint, 0.4f, 0.62f, 0.88f);
        else GLES20.glUniform3f(tint, 1, 1, 1);
        GLES20.glEnableVertexAttribArray(position);
        GLES20.glEnableVertexAttribArray(uv);
        vertices.position(0);
        GLES20.glVertexAttribPointer(position, 2, GLES20.GL_FLOAT, false, 16, vertices);
        vertices.position(2);
        GLES20.glVertexAttribPointer(uv, 2, GLES20.GL_FLOAT, false, 16, vertices);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
    }

    void draw(
            int id,
            float x,
            float y,
            float width,
            float height,
            float left,
            float top,
            float uvWidth,
            float uvHeight,
            float movement,
            float offset,
            float rotation,
            float depthFog,
            int kind,
            float alpha) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, id);
        GLES20.glUniform4f(placement, x, y, width, height);
        GLES20.glUniform4f(uvRect, left, top, uvWidth, uvHeight);
        GLES20.glUniform1f(motion, movement);
        GLES20.glUniform1f(phase, offset);
        GLES20.glUniform1f(angle, rotation);
        GLES20.glUniform1f(fog, depthFog);
        GLES20.glUniform1f(effect, kind);
        GLES20.glUniform1f(opacity, alpha);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount);
    }
}
