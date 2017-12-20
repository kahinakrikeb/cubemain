package com.example.m1.open;

import android.opengl.GLSurfaceView;
import android.opengl.GLU;

import javax.microedition.khronos.opengles.GL10;


public class GLRenderer implements GLSurfaceView.Renderer {
	
	// global variables for cube parameters
    private Cubedessin monCube = new Cubedessin(); // Objet Cube
    public float monCubeRotation; // Valeur de rotation
    private GL10 gl; // instancier un objet de type GL, qui nous permettra d'utiliser les fonctions OpenGL

    private boolean renderCube = false;
    private float XScale = 1;// Abscisse de l'échelle du cube
    private float YScale =1; // Ordonnée de l'échelle du cube
    private float posX = 0;// Abscisse de la position du cube
    private float posY = 0; // Ordonnée de l'ordonnée du cube
    private int vidWidth = 0;
    private int vidHeight = 0;
    private float cubeSize = 2.0f;   // Valeur de la taille du cube
    private double rotSpeed = 0.250f; // Valeur de la vitesse de rotation du cube

    // Setter pour assigner une valeur à la vitesse de rotation du cube
    public void setCubeRotation(int val){
        rotSpeed = 0.250f * val*2;

    }

	//Method to set cube visibility

    public void setRenderCube(boolean val){
        renderCube = val;
    }

	
	//Méthode pour définir les dimensions du frame

    public void setVidDim(int w, int h){
        vidWidth = w;
        vidHeight = h;
        XScale = 11.0f * 2 / vidWidth;
        YScale = 8.0f * 2 / vidHeight;

    }

	
	// Fonction qu assigne les coordonnées de position X et y du cube
    public void setPos(double x, double y){
        x -= vidWidth/2;
        y -= vidHeight/2;
        posX = (float) x * XScale;
        posY = (float) y * -YScale;
        renderCube = true;
    }


    // Fonction permettant d'agir sur l'objet cube affiché dans la frame
    public void onDrawFrame(GL10 gl) {
        if(renderCube) {
            gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
            gl.glLoadIdentity();

            gl.glTranslatef(posX, posY, -90.0f);

            gl.glRotatef(monCubeRotation, 1.0f, 1.0f, 1.0f); //up-down, left-right, cw-acw

            gl.glScalef(cubeSize, cubeSize, cubeSize);

            monCube.draw(gl);

            gl.glLoadIdentity();

            monCubeRotation -= rotSpeed;
        }
        else
            gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
    }

	  //initialiser la création de surface

    @Override
    public void onSurfaceCreated(GL10 gl, javax.microedition.khronos.egl.EGLConfig config) {
        this.gl = gl;
        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.5f);

        gl.glClearDepthf(1.0f);
        gl.glEnable(GL10.GL_DEPTH_TEST);
        gl.glDepthFunc(GL10.GL_LEQUAL);

        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT,
                GL10.GL_NICEST);

    }


    // Fonction permetttant de gérer le changement de surface(quand la caméra bouge)
    public void onSurfaceChanged( GL10 gl, int width, int height ) {
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();
		
        GLU.gluPerspective(gl, 10.0f, (float) width/ (float) height, 0.1f, 100.0f);

        gl.glViewport(0, 0, width, height);

        gl.glMatrixMode(GL10.GL_MODELVIEW);

        gl.glLoadIdentity();
    }
}
