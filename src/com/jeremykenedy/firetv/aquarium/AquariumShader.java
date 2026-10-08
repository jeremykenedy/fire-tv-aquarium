package com.jeremykenedy.firetv.aquarium;

import android.opengl.GLES20;
import android.opengl.Matrix;

final class AquariumShader {
    private static final String VERTEX =
            "uniform mat4 uModel,uNormal,uViewProjection; uniform mediump float uTime; uniform"
                + " float uMotion,uPhase;attribute vec3 aPosition,aNormal; varying mediump vec3"
                + " vNormal,vWorld,vLocal;void main(){vec3 p=aPosition;vLocal=p;"
                + "if(uMotion>2.5){p.x+=sin(uTime*1.7+uPhase-p.y*2.5)*max(0.0,-p.y)*0.13;p.z+=cos(uTime*1.4+uPhase-p.y*2.0)*max(0.0,-p.y)*0.12;}else"
                + " if(uMotion>1.5)p.x+=sin(uTime*0.7+uPhase+p.y)*p.y*p.y*0.06;else"
                + " if(uMotion>0.5)p.z+=sin(uTime*4.0+uPhase-p.x*3.0)*max(0.0,0.3-p.x)*0.10;vec4"
                + " world=uModel*vec4(p,1.0);vWorld=world.xyz;"
                + "vNormal=normalize(mat3(uNormal)*aNormal);gl_Position=uViewProjection*world;}";

    private static final String FRAGMENT =
            "precision mediump float; uniform vec4 uColor;uniform vec3 uWater,uTint; uniform float"
                + " uMaterial,uSpecies,uTime,uRays;varying vec3 vNormal,vWorld,vLocal;void"
                + " main(){vec3 color=uColor.rgb;float"
                + " alpha=uColor.a;if(uMaterial>0.5&&uMaterial<1.5){"
                + " if(uSpecies<0.5){color=vec3(1.0,0.32,0.045); float"
                + " stripe=min(min(abs(vLocal.x-0.56),abs(vLocal.x+0.05)),abs(vLocal.x+0.62));"
                + " if(stripe<0.17)color=vec3(0.055,0.075,0.07);if(stripe<0.115)color=vec3(0.96,0.97,0.90);}"
                + " else if(uSpecies<1.5)color=vec3(1.0,0.88,0.10); else"
                + " if(uSpecies<2.5){color=vec3(0.04,0.32,0.94);"
                + " if(vLocal.y>0.06&&vLocal.y<0.40&&vLocal.x<0.58)color=vec3(0.025,0.05,0.14);}"
                + " else if(uSpecies<3.5){color=vec3(0.82,0.87,0.87);"
                + " if(abs(sin(vLocal.x*6.5))<0.28)color=vec3(0.1,0.16,0.19);} else"
                + " if(uSpecies<4.5){color=mix(vec3(0.93,0.10,0.16),vec3(0.035,0.90,1.0),step(-0.10,vLocal.y));}"
                + " else {color=mix(vec3(0.13,0.30,0.79),vec3(0.93,0.12,0.25),vLocal.x*0.5+0.5);}"
                + " float"
                + " scales=sin(vLocal.x*65.0)*sin(vLocal.y*48.0);color*=0.94+0.06*scales;}if(uMaterial>2.5&&uMaterial<3.5){float"
                + " grain=fract(sin(dot(vWorld.xz,vec2(12.98,78.23)))*437.5);"
                + " color*=0.84+0.16*grain;}if(uMaterial>3.5&&uMaterial<4.5){ float"
                + " elevation=clamp((vWorld.y+7.0)/24.0,0.0,1.0);color=mix(uWater*0.65,uWater*2.0,elevation);"
                + " float"
                + " beam=max(0.0,sin(vWorld.x*0.33+vWorld.y*0.18+uTime*0.10));beam*=beam;beam*=beam;beam*=beam;"
                + " beam*=0.8+0.2*sin(uTime*1.4+vWorld.x*0.7);"
                + " color+=vec3(0.14,0.25,0.27)*beam*elevation*uRays;gl_FragColor=vec4(color*uTint,1.0);return;}vec3"
                + " normal=normalize(vNormal);vec3 light=normalize(vec3(-0.3,0.8,0.7));float"
                + " diffuse=0.36+0.64*max(0.0,dot(normal,light));vec3"
                + " eye=normalize(vec3(0.0,1.0,28.0)-vWorld);float"
                + " specular=pow(max(0.0,dot(reflect(-light,normal),eye)),32.0)*0.28;"
                + "if(uMaterial>6.5)diffuse=1.0;color=color*diffuse*uTint+specular*uTint;if(uRays>0.5&&uMaterial<3.5){float"
                + " ripple=sin(vWorld.x*2.4+vWorld.z*1.7+uTime*1.3)*sin(vWorld.x*1.1-vWorld.z*2.5-uTime);"
                + "color+=vec3(0.13,0.17,0.12)*pow(max(0.0,ripple),4.0)*max(0.0,normal.y);}if(uMaterial>4.5&&uMaterial<5.5){float"
                + " rim=1.0-abs(dot(normal,eye));"
                + " alpha*=0.15+0.85*rim*rim;color=vec3(0.54,0.83,0.91)+specular;}if(uMaterial>5.5){float"
                + " texture=sin(vLocal.x*18.0+vLocal.y*13.0)*sin(vLocal.z*17.0);color*=0.85+texture*0.15;}float"
                + " fog=clamp((-vWorld.z+2.0)*0.018,0.0,0.45);"
                + "gl_FragColor=vec4(mix(color,uWater,fog),alpha);}";

    final int program;
    final int position;
    final int normal;
    private final int model;
    private final int normalMatrix;
    private final int viewProjection;
    private final int color;
    private final int material;
    private final int species;
    private final int time;
    private final int motion;
    private final int phase;
    private final int water;
    private final int tint;
    private final int rays;
    private final float[] inverse = new float[16];
    private final float[] transpose = new float[16];

    AquariumShader() {
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
        normal = GLES20.glGetAttribLocation(program, "aNormal");
        model = location("uModel");
        normalMatrix = location("uNormal");
        viewProjection = location("uViewProjection");
        color = location("uColor");
        material = location("uMaterial");
        species = location("uSpecies");
        time = location("uTime");
        motion = location("uMotion");
        phase = location("uPhase");
        water = location("uWater");
        tint = location("uTint");
        rays = location("uRays");
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

    void frame(
            float[] camera,
            float seconds,
            float[] waterColor,
            float[] lighting,
            boolean lightRays) {
        GLES20.glUseProgram(program);
        GLES20.glUniformMatrix4fv(viewProjection, 1, false, camera, 0);
        // All shader frequencies share this period. Keep mediump time precise
        // during long screensaver sessions without a visible animation jump.
        GLES20.glUniform1f(time, seconds % 62.831853f);
        GLES20.glUniform3fv(water, 1, waterColor, 0);
        GLES20.glUniform3fv(tint, 1, lighting, 0);
        GLES20.glUniform1f(rays, lightRays ? 1 : 0);
        GLES20.glEnableVertexAttribArray(position);
        GLES20.glEnableVertexAttribArray(normal);
    }

    void object(
            float[] transform,
            float red,
            float green,
            float blue,
            float alpha,
            int surface,
            int fishSpecies,
            float movement,
            float offset) {
        Matrix.invertM(inverse, 0, transform, 0);
        Matrix.transposeM(transpose, 0, inverse, 0);
        GLES20.glUniformMatrix4fv(model, 1, false, transform, 0);
        GLES20.glUniformMatrix4fv(normalMatrix, 1, false, transpose, 0);
        GLES20.glUniform4f(color, red, green, blue, alpha);
        GLES20.glUniform1f(material, surface);
        GLES20.glUniform1f(species, fishSpecies);
        GLES20.glUniform1f(motion, movement);
        GLES20.glUniform1f(phase, offset);
    }

    void draw(AquariumMesh mesh) {
        mesh.vertices.position(0);
        GLES20.glVertexAttribPointer(position, 3, GLES20.GL_FLOAT, false, 24, mesh.vertices);
        mesh.vertices.position(3);
        GLES20.glVertexAttribPointer(normal, 3, GLES20.GL_FLOAT, false, 24, mesh.vertices);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mesh.count);
    }
}
