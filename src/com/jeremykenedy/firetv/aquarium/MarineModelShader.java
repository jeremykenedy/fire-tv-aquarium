package com.jeremykenedy.firetv.aquarium;

import android.opengl.GLES20;

/** Textured volume with a traveling spinal wave and directional underwater light. */
final class MarineModelShader {
    // Zero identifies body vertices; membrane roots reserve a signed .0001 marker.
    // The fixed aquarium camera faces -Z; a +Z nose shows the frontal skin.
    private static final String VERTEX =
            "attribute vec3 aPosition,aNormal;attribute vec2 aUv;attribute float aFinFlex;uniform"
                + " mat4 uModel,uViewProjection;uniform float"
                + " uTime,uPhase,uTailVertical,uHasFinMask;uniform vec4 uFrontUv;uniform vec3"
                + " uFrontBlend;varying mediump vec2 vUv,vFrontalUv;varying mediump float"
                + " vFrontWeight;varying mediump float vSolid;varying mediump vec3"
                + " vNormal,vWorld;void main(){vec3 p=aPosition;float tail=max(0.0,.25-p.x);float"
                + " wave=uTime*4.2+uPhase-p.x*3.4;float"
                + " sway=sin(wave)*tail*tail*.075;p+=vec3(0.0,sway*uTailVertical,sway*(1.0-uTailVertical));float"
                + " flex=max(0.0,(abs(aFinFlex)-.0001)/.9999);float"
                + " finPhase=uTime*3.2+uPhase+p.x*2.3;float"
                + " flutter=sin(finPhase+(aFinFlex<0.0?1.4:0.0))*flex*.025*(aFinFlex<0.0?-1.0:1.0);if(uTailVertical>.5&&p.x<-.5){p.y+=flutter;}else{p.z+=flutter;}p.y+=cos(finPhase)*flex*.006;vec3"
                + " n=aNormal;float slope=(-cos(wave)*3.4*tail*tail-2.0*sin(wave)*tail)*.075;"
                + "n.x-=slope*mix(n.z,n.y,uTailVertical);vNormal=normalize(mat3(uModel)*n);vec4"
                + " world=uModel*vec4(p,1.0);vWorld=world.xyz;vUv=aUv;vSolid=uHasFinMask*(abs(aFinFlex)<.00001?1.0:0.0);"
                + "vFrontalUv=clamp(vec2(aPosition.z*uFrontUv.x+uFrontUv.y,aPosition.y*uFrontUv.z+uFrontUv.w),vec2(.51,.02),vec2(.99,.98));vFrontWeight=0.0;if(uFrontBlend.x>.5){vFrontWeight=vSolid*smoothstep(-.035,.035,aNormal.x)*smoothstep(uFrontBlend.y,uFrontBlend.z,aPosition.x)*smoothstep(.78,.99,normalize(mat3(uModel)*vec3(1.0,0.0,0.0)).z);}gl_Position=uViewProjection*world;}";
    private static final String FRAGMENT =
            "precision mediump float;uniform sampler2D uTexture;uniform float uLook;uniform vec3"
                + " uWater,uTint;varying mediump vec2 vUv,vFrontalUv;varying mediump float"
                + " vFrontWeight;varying mediump float vSolid;varying mediump vec3"
                + " vNormal,vWorld;void main(){vec4"
                + " sample=texture2D(uTexture,vUv);if(vFrontWeight>.001){sample=mix(sample,texture2D(uTexture,vFrontalUv),vFrontWeight);}if(vSolid>.999){sample.rgb=sample.a>.01?sample.rgb/sample.a:vec3(.018,.012,.016);sample.a=1.0;}else"
                + " if(sample.a<.12)discard;vec3 n=normalize(vNormal);float"
                + " diffuse=.62+.38*max(0.0,dot(n,normalize(vec3(-.4,.8,1.0))));vec3"
                + " rgb=sample.rgb*diffuse*uTint;float gray=dot(rgb,vec3(.299,.587,.114));"
                + "if(uLook>.5&&uLook<1.5){rgb=mix(vec3(gray),rgb,.8)*vec3(.9,.98,1.04);}else"
                + " if(uLook>1.5&&uLook<2.5){rgb=mix(vec3(gray),rgb,1.12);rgb+=sample.a*pow(max(0.0,dot(n,normalize(vec3(-.3,.6,1.0)))),18.0)*.08;}else"
                + " if(uLook>2.5&&uLook<3.5){float"
                + " bands=floor(diffuse*4.0)/4.0;rgb=sample.rgb*(.4+bands*.65)*uTint;rgb=mix(vec3(dot(rgb,vec3(.299,.587,.114))),rgb,1.3);}else"
                + " if(uLook>3.5&&uLook<4.5){rgb=mix(vec3(gray),rgb,1.3)*vec3(1.04,1.01,.96);}else"
                + " if(uLook>4.5){rgb=mix(rgb,vec3(gray),.18);rgb=mix(rgb,vec3(.77,.87,.95)*sample.a,.12);rgb*=vec3(1.02,.98,1.05);}float"
                + " fog=clamp((-vWorld.z+2.0)*.024,0.0,.42);"
                + "gl_FragColor=vec4(mix(rgb,uWater*sample.a,fog),sample.a);}";
    private final int program;
    private final int position;
    private final int normal;
    private final int uv;
    private final int flex;
    private final int model;
    private final int projection;
    private final int time;
    private final int phase;
    private final int water;
    private final int tint;
    private final int texture;
    private final int look;
    private final int tailVertical;
    private final int finMask;
    private final int frontUv;
    private final int frontBlend;

    MarineModelShader() {
        program = GLES20.glCreateProgram();
        int vertex = compile(GLES20.GL_VERTEX_SHADER, VERTEX);
        int fragment = compile(GLES20.GL_FRAGMENT_SHADER, FRAGMENT);
        GLES20.glAttachShader(program, vertex);
        GLES20.glAttachShader(program, fragment);
        GLES20.glLinkProgram(program);
        int[] status = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0);
        if (status[0] == 0) throw new IllegalStateException(GLES20.glGetProgramInfoLog(program));
        GLES20.glDeleteShader(vertex);
        GLES20.glDeleteShader(fragment);
        position = GLES20.glGetAttribLocation(program, "aPosition");
        normal = GLES20.glGetAttribLocation(program, "aNormal");
        uv = GLES20.glGetAttribLocation(program, "aUv");
        flex = GLES20.glGetAttribLocation(program, "aFinFlex");
        model = GLES20.glGetUniformLocation(program, "uModel");
        projection = GLES20.glGetUniformLocation(program, "uViewProjection");
        time = GLES20.glGetUniformLocation(program, "uTime");
        phase = GLES20.glGetUniformLocation(program, "uPhase");
        water = GLES20.glGetUniformLocation(program, "uWater");
        tint = GLES20.glGetUniformLocation(program, "uTint");
        texture = GLES20.glGetUniformLocation(program, "uTexture");
        look = GLES20.glGetUniformLocation(program, "uLook");
        tailVertical = GLES20.glGetUniformLocation(program, "uTailVertical");
        finMask = GLES20.glGetUniformLocation(program, "uHasFinMask");
        frontUv = GLES20.glGetUniformLocation(program, "uFrontUv");
        frontBlend = GLES20.glGetUniformLocation(program, "uFrontBlend");
    }

    void draw(
            TexturedMarineModel mesh,
            float[] transform,
            float[] camera,
            float seconds,
            float offset,
            float[] waterColor,
            float[] lighting,
            int appearance,
            float verticalTail) {
        GLES20.glUseProgram(program);
        GLES20.glUniformMatrix4fv(model, 1, false, transform, 0);
        GLES20.glUniformMatrix4fv(projection, 1, false, camera, 0);
        GLES20.glUniform1f(time, seconds % 628.31853f);
        GLES20.glUniform1f(phase, offset);
        GLES20.glUniform3fv(water, 1, waterColor, 0);
        GLES20.glUniform3fv(tint, 1, lighting, 0);
        GLES20.glUniform1i(texture, 0);
        GLES20.glUniform1f(look, appearance);
        GLES20.glUniform1f(tailVertical, verticalTail);
        mesh.draw(position, normal, uv, flex, finMask, frontUv, frontBlend);
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
}
