package raytracer;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.Container;
import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Dimension;
import java.util.List;
import java.util.ArrayList;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/* Implementation of a very simple Raytracer
   Stephan Diehl, Universit�t Trier, 2010-2016
*/



public class SDRaytracer extends JFrame
{
   private static final long serialVersionUID = 1L;
   boolean profiling=false;
   int widthRay =1000;
   int heightRay =1000;
   
   transient Future[] futureList= new Future[widthRay];
   int nrOfProcessors = Runtime.getRuntime().availableProcessors();
   transient ExecutorService eservice = Executors.newFixedThreadPool(nrOfProcessors);
   
   int maxRec=3;
   int rayPerPixel=1;
   int startX;
   int startY;
   int startZ;

   transient List<Triangle> triangles;

   //Refactoring
   transient RGB rgb = new RGB();
   transient Vec3D mainLight  = new Vec3D(0,100,0, new RGB(0.1f,0.1f,0.1f));

    transient Vec3D lights[]= new Vec3D[]{ mainLight
                                ,new Vec3D(100,200,300, new RGB(0.5f,0,0.0f))
                                ,new Vec3D(-100,200,300, new RGB(0.0f,0,0.5f))
                                //,new src.Light(new src.raytracer.Vec3D(-100,0,0), new src.raytracer.RGB(0.0f,0.8f,0.0f))
                              };

   transient RGB [][] image= new RGB[widthRay][heightRay];
   
   float fovx=(float) 0.628;
   float fovy=(float) 0.628;
   transient RGB ambientColor =new RGB(0.01f,0.01f,0.01f);
   transient RGB backgroundColor =new RGB(0.05f,0.05f,0.05f);
   transient RGB black=new RGB(0.0f,0.0f,0.0f);
   int yAngleFactor =4;
   int xAngleFactor =-4;

void profileRenderImage(){
  long end;
  long start;
  long time;

  renderImage(); // initialisiere Datenstrukturen, erster Lauf verf�lscht sonst Messungen
  
  for(int procs=1; procs<6; procs++) {

   maxRec=procs-1;
   System.out.print(procs);
   for(int i=0; i<10; i++)
     { start = System.currentTimeMillis();

       renderImage();

       end = System.currentTimeMillis();
       time = end - start;
       System.out.print(";"+time);
     }
    System.out.println("");
   }
}

SDRaytracer()
 {
   createScene();

   if (!profiling) renderImage(); else profileRenderImage();
   
   setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
   Container contentPane = this.getContentPane();
   contentPane.setLayout(new BorderLayout());
   JPanel area = new JPanel() {
       @Override
            public void paint(Graphics g) {
              System.out.println("fovx="+fovx+", fovy="+fovy+", xangle="+ xAngleFactor +", yangle="+ yAngleFactor);
              if (image==null) return;
              for(int i = 0; i< widthRay; i++)
               for(int j = 0; j< heightRay; j++)
                { g.setColor(image[i][j].rgbcolor());
                  // zeichne einzelnen Pixel
                  g.drawLine(i, heightRay -j,i, heightRay -j);
                }
            }
           };
           
   addKeyListener(new KeyAdapter()
         {
             @Override
             public void keyPressed(KeyEvent e)
            { boolean redraw=false;
              if (e.getKeyCode()==KeyEvent.VK_DOWN)
                {  xAngleFactor--;
                  redraw=true;
                }
              if (e.getKeyCode()==KeyEvent.VK_UP)
                {  xAngleFactor++;
                  redraw=true;
                }
              if (e.getKeyCode()==KeyEvent.VK_LEFT)
                { yAngleFactor--;
                  redraw=true;
                }
              if (e.getKeyCode()==KeyEvent.VK_RIGHT)
                { yAngleFactor++;
                  redraw=true;
                }
              if (redraw)
               { createScene();
                 renderImage();
                 repaint();
               }
            }
         });
         
        area.setPreferredSize(new Dimension(widthRay, heightRay));
        contentPane.add(area);
        this.pack();
        this.setVisible(true);
}
 
transient Ray eyeRay =new Ray();
double tanFovx;
double tanFovy;
 
void renderImage(){
   tanFovx = Math.tan(fovx);
   tanFovy = Math.tan(fovy);
   for(int i = 0; i< widthRay; i++)
   { futureList[i]=  (Future) eservice.submit(new RaytraceTask(this,i));
   }
   
    for(int i = 0; i< widthRay; i++)
       { try {
          RGB [] col = (RGB[]) futureList[i].get();
          for(int j = 0; j< heightRay; j++)
            image[i][j]=col[j];
         }
   catch (InterruptedException e) {
           Thread.currentThread().interrupt();
   }
   catch (ExecutionException e) {
           e.printStackTrace();
   }
    }
   }
 


RGB rayTrace(Ray ray, int rec) {
   if (rec>maxRec) return black;
   IPoint ip = hitObject(ray);
   if (ip.dist>IPoint.EPSILON) {
       //Refactoring
       return rgb.lighting(ray, ip, rec, ambientColor, lights,this);
   }
   else
     return black;
}


IPoint hitObject(Ray ray) {
   IPoint isect=new IPoint(null,null,-1);
   float idist=-1;
   for(Triangle t : triangles)
     { IPoint ip = ray.intersect(t);
        if (ip.dist!=-1) {
            if ((idist == -1) || (ip.dist < idist)) { // save that intersection
                idist = ip.dist;
                isect.ipointVar = ip.ipointVar;
                isect.dist = ip.dist;
                isect.triangle = t;
            }
        }
     }
   return isect;  // return intersection point and normal
}

  void createScene()
   { triangles = new ArrayList<>();

   
     Cube.addCube(triangles, 0,35,0, 10,10,10,new RGB(0.3f,0,0),0.4f);       //rot, klein
     Cube.addCube(triangles, -70,-20,-20, 20,100,100,new RGB(0f,0,0.3f),.4f);
     Cube.addCube(triangles, -30,30,40, 20,20,20,new RGB(0,0.4f,0),0.2f);        // gr�n, klein
     Cube.addCube(triangles, 50,-20,-40, 10,80,100,new RGB(.5f,.5f,.5f), 0.2f);
     Cube.addCube(triangles, -70,-26,-40, 130,3,40,new RGB(.5f,.5f,.5f), 0.2f);


     Matrix mRx=Matrix.createXRotation((float) (xAngleFactor *Math.PI/16));
     Matrix mRy=Matrix.createYRotation((float) (yAngleFactor *Math.PI/16));
     Matrix mT=Matrix.createTranslation(0,0,200);
     Matrix m=mT.mult(mRx).mult(mRy);
     m.print();
     m.apply(triangles);
   }

}


