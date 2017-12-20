package com.example.m1.open;

import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.*;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfInt4;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.imgproc.Imgproc;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity implements CvCameraViewListener2 {

    // declaration des variables globales.
    // Réutilisez des instances OpenCV comme Mat
    private JavaCameraView affichcam; // implémentation de Bridge View entre OpenCV et Java Camera
    private Mat matri1; // la matrice
    private int camDim[] = {320, 240};          // Dimension de la caméra
    private Scalar minCouleurHSV;
    private Scalar maxCouleurHSV;
    private Mat frame, frame2;
    private Point palmCenter;
    private MatOfInt hull;
    private Mat hierarchy;
    private Mat nonZero;
    private List<MatOfPoint> contours;
    private GLRenderer myGLRenderer;  // Objet OpenGL GLRenderer


    // Vérifier et  charge les fonctions/méthodes d'OpenCV
    static {
        if (!OpenCVLoader.initDebug())
            Log.e("init", "OpenCV NOT loaded");
        else
            Log.e("init", "OpenCV successfully loaded");
    }

    // Méthode permettant de verifier que OpenCV s'est correctement lancer et d'activer la caméra
    private BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i("demmarre", "OpenCV callback successful");
                    affichcam.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);// On garde la fenetre on mode on eveiller
        View decorView = getWindow().getDecorView();// Le DecorView est la vue qui tient le fond de la fenetre
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;// On affiche l'application en pleine écran
        decorView.setSystemUiVisibility(uiOptions); // Fonction requise pour le pleine écran (personnalisation de la vue)
        setContentView(R.layout.activity_main);

        //affichage de la camera
        affichcam = (JavaCameraView) findViewById(R.id.surface_affichage);// On récupère une référence dans le fichier xml pour pouvoir l'utiliser dans java
        affichcam.setVisibility(SurfaceView.VISIBLE);// On affiche ce que la caméra filme
        affichcam.setCvCameraViewListener(this);// On implémente un listenner sur la caméra

        affichcam.setMaxFrameSize(camDim[0], camDim[1]); // On dimensionne le cadre de la fenetre de la caméra


		// initialise OpenGL view
        GLSurfaceView maGLView = new GLSurfaceView(this);// instantialtion d'une GLsurfaceView pour le rondu du cube
        maGLView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);// Configure les couleurs RGB de la surfaceView
        maGLView.getHolder().setFormat(PixelFormat.TRANSLUCENT); // Permet de rendre les pixels translucides (invisbles)
        myGLRenderer = new GLRenderer();// Nouvel objet GLRenderer qui genere le cube
        maGLView.setRenderer(myGLRenderer); // On applique cet objet à la surfaceView
        addContentView(maGLView, new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT));// on applique le paramètre wrapContent au layout de cette surfaceview
        maGLView.setZOrderMediaOverlay(true); // Contrôler si la surface de la vue de surface est placé sur le dessus de sa fenêtre.
    }


    public void onResume(){
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_10, this, baseLoaderCallback);
    }

    public void onDestroy() {
        super.onDestroy();
        if (affichcam != null)
            affichcam.disableView();
    }

    // Fonction permettant d'initialiser et de configurer les paramètres de la caméra
    @Override
    public void onCameraViewStarted(int width, int height) {
		
       // setScaleFactors(width, height);// On appliquer les valeurs de largeur et hauteur de l'échelle de la fenêtre
        myGLRenderer.setVidDim(camDim[0], camDim[1]);// On applique les dimensions à la caméra
        matri1 = new Mat(height, width, CvType.CV_8UC3); // On affecte les dimensions de la matrice d'une image
        minCouleurHSV = new Scalar(3);// On affecte la valeur minimum d'un pixel
        maxCouleurHSV = new Scalar(3);// On affecte la valeur maximum d'un pixel

    // interval min max en HSV pour detecter la main
        minCouleurHSV.val[0] = 0;
        minCouleurHSV.val[1] = 30;
        minCouleurHSV.val[2] = 52.75749999999999;
        maxCouleurHSV.val[0] = 18.406875;
        maxCouleurHSV.val[1] =207.890625;
        maxCouleurHSV.val[2] = 252.7575;


        frame = new Mat();//matice pour recuper les images de la video en lisant images pas images
        hull = new MatOfInt();// MatOfInt, MatOfFloat est des classes qui sont héritées de Mat et a 1 canal de type et de taille définie 1xN. Il est analogue de std :: vector <int>, std :: vector <deux>, etc dans le code C ++.
        hierarchy  = new Mat();
        nonZero = new Mat();
        frame2 = new Mat();
        contours = new ArrayList<>();
        palmCenter = new Point(10, 10); // Point de coordonnées x et y correspondant au centre de la main

        myGLRenderer.setRenderCube(true);
        myGLRenderer.setCubeRotation(0);
    }

    // Méthode permettant d'appliquer des fonctions à l'image de la caméra et de renvoyer l'image modifié à la caméra
    @Override
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

        // On remplie la matrice des channels de l'image avec les couleurs RGBA
        matri1 = inputFrame.rgba();

        // Si la main est détectée

			// clone frame beacuse original frame needed for display
            frame = matri1.clone();

			// remove noise and convert to binary in HSV range determined by user input
            Imgproc.GaussianBlur(frame, frame, new Size(9, 9), 5); // Applique un effet de flau gaussian à l'image
            Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGB2HSV_FULL); // Convertit une image d'un espace colorimétrique à un autre.
            Core.inRange(frame, minCouleurHSV, maxCouleurHSV, frame);//applique le filtre interval sur la frame(image)


			// renvoie les zones blanches de l image
            contours =  getAllContours(frame);
            int indexOfPalmContour = getPalmContour(contours);
           // Log.i("SHVss", "onCameraFrame: "+indexOfPalmContour);
            // Conditionnnel permettant de savoir si on affiche le cube ou pas en fonction de la détection du contour de la main ou pas
            if(indexOfPalmContour < 0){
            		// afficher le cube avec une face initial
                myGLRenderer.setRenderCube(true);
                myGLRenderer.monCubeRotation=0;
                myGLRenderer.setCubeRotation(0);
            }
            else{
                //afficher le cube en 3D avec rotation
				// trouver les points ou afficher le cube
                 Point palm = getDistanceTransformCenter(frame);
                // setter la position pour le render du cube sur li'image qui correspondes au point trouvé dans la ligne precedente
                myGLRenderer.setPos(palm.x, palm.y);

                myGLRenderer.setCubeRotation(5);

            }
            return matri1;

    }

    @Override
    public void onCameraViewStopped() {
		// libérer toutes les ressources  on camera close
        frame.release();
        matri1.release();
        hull.release();
        hierarchy.release();
        nonZero.release();
        frame2.release();

        while (contours.size() > 0)
            contours.get(0).release();
    }

    /**
     * Méthode pour calculer et retourner le plus fort point de transformation de distance.
     * Pour une image binaire avec une paume en blanc.

	 */
    protected Point getDistanceTransformCenter(Mat frame){

        Imgproc.distanceTransform(frame, frame, Imgproc.CV_DIST_L2, 3);
        frame.convertTo(frame, CvType.CV_8UC1);
        Core.normalize(frame, frame, 0, 255, Core.NORM_MINMAX);
        Imgproc.threshold(frame, frame, 254, 255, Imgproc.THRESH_TOZERO);
        Core.findNonZero(frame, nonZero);

        // calculer les sommes
        int sumx = 0, sumy = 0;
        for(int i=0; i<nonZero.rows(); i++) {
            sumx += nonZero.get(i, 0)[0];
            sumy += nonZero.get(i, 0)[1];
        }
        sumx /= nonZero.rows();
        sumy /= nonZero.rows();

        return new Point(sumx, sumy);
    }


	/**
	 * revoinvoie le centre du countour ou il faut afficher le dessin
	 */
    protected int getPalmContour(List<MatOfPoint> contours){

        Rect roi;
        int indexOfMaxContour = -1;
        for (int i = 0; i < contours.size(); i++) {
            roi = Imgproc.boundingRect(contours.get(i));
            if(roi.contains(palmCenter))
                return i;
        }
        return indexOfMaxContour;
    }

	
	/**
	 * Method to get all possible contours in binary image frame.
	 */
    protected List<MatOfPoint> getAllContours(Mat frame){
        frame2 = frame.clone();
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Imgproc.findContours(frame2, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        return contours;
    }


}

