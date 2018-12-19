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
   int width=1000;
   int height=1000;
   
   Future[] futureList= new Future[width];
   int nrOfProcessors = Runtime.getRuntime().availableProcessors();
   ExecutorService eservice = Executors.newFixedThreadPool(nrOfProcessors);
   
   int maxRec=3;
   int rayPerPixel=1;
   int startX, startY, startZ;

   List<Triangle> triangles;

   //Refactoring
   RGB rgb = new RGB();
   Vec3D mainLight  = new Vec3D(0,100,0, new RGB(0.1f,0.1f,0.1f));

    Vec3D lights[]= new Vec3D[]{ mainLight
                                ,new Vec3D(100,200,300, new RGB(0.5f,0,0.0f))
                                ,new Vec3D(-100,200,300, new RGB(0.0f,0,0.5f))
                                //,new src.Light(new src.raytracer.Vec3D(-100,0,0), new src.raytracer.RGB(0.0f,0.8f,0.0f))
                              };

   RGB [][] image= new RGB[width][height];
   
   float fovx=(float) 0.628;
   float fovy=(float) 0.628;
   RGB ambient_color=new RGB(0.01f,0.01f,0.01f);
   RGB background_color=new RGB(0.05f,0.05f,0.05f);
   RGB black=new RGB(0.0f,0.0f,0.0f);
   int y_angle_factor=4, x_angle_factor=-4;

void profileRenderImage(){
  long end, start, time;

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
            public void paint(Graphics g) {
              System.out.println("fovx="+fovx+", fovy="+fovy+", xangle="+x_angle_factor+", yangle="+y_angle_factor);
              if (image==null) return;
              for(int i=0;i<width;i++)
               for(int j=0;j<height;j++)
                { g.setColor(image[i][j].color());
                  // zeichne einzelnen Pixel
                  g.drawLine(i,height-j,i,height-j);
                }
            }
           };
           
   addKeyListener(new KeyAdapter()
         { public void keyPressed(KeyEvent e)
            { boolean redraw=false;
              if (e.getKeyCode()==KeyEvent.VK_DOWN)
                {  x_angle_factor--;
                   //mainLight.position.y-=10;
                  //fovx=fovx+0.1f;
                  //fovy=fovx;
                  //maxRec--; if (maxRec<0) maxRec=0;
                  redraw=true;
                }
              if (e.getKeyCode()==KeyEvent.VK_UP)
                {  x_angle_factor++;
                   //mainLight.position.y+=10;
                  //fovx=fovx-0.1f;
                  //fovy=fovx;
                  //maxRec++;if (maxRec>10) return;
                  redraw=true;
                }
              if (e.getKeyCode()==KeyEvent.VK_LEFT)
                { y_angle_factor--;
                  //mainLight.position.x-=10;
                  //startX-=10;
                  //fovx=fovx+0.1f;
                  //fovy=fovx;
                  redraw=true;
                }
              if (e.getKeyCode()==KeyEvent.VK_RIGHT)
                { y_angle_factor++;
                  //mainLight.position.x+=10;
                  //startX+=10;
                  //fovx=fovx-0.1f;
                  //fovy=fovx;
                  redraw=true;
                }
              if (redraw)
               { createScene();
                 renderImage();
                 repaint();
               }
            }
         });
         
        area.setPreferredSize(new Dimension(width,height));
        contentPane.add(area);
        this.pack();
        this.setVisible(true);
}
 
Ray eye_ray=new Ray();
double tan_fovx;
double tan_fovy;
 
void renderImage(){
   tan_fovx = Math.tan(fovx);
   tan_fovy = Math.tan(fovy);
   for(int i=0;i<width;i++)
   { futureList[i]=  (Future) eservice.submit(new RaytraceTask(this,i));
   }
   
    for(int i=0;i<width;i++)
       { try {
          RGB [] col = (RGB[]) futureList[i].get();
          for(int j=0;j<height;j++)
            image[i][j]=col[j];
         }
   catch (InterruptedException e) {
           Thread.currentThread().interrupt();
   }
   catch (ExecutionException e) {}
    }
   }
 


RGB rayTrace(Ray ray, int rec) {
   if (rec>maxRec) return black;
   IPoint ip = hitObject(ray);  // (ray, p, n, triangle);
   if (ip.dist>IPoint.epsilon) {
       //Refactoring
       return rgb.lighting(ray, ip, rec, ambient_color, lights,this);
   }
   else
     return black;
}


IPoint hitObject(Ray ray) {
   IPoint isect=new IPoint(null,null,-1);
   float idist=-1;
   for(Triangle t : triangles)
     { IPoint ip = ray.intersect(t);
        if (ip.dist!=-1)
        if ((idist==-1)||(ip.dist<idist))
         { // save that intersection
          idist=ip.dist;
          isect.ipoint_var=ip.ipoint_var;
          isect.dist=ip.dist;
          isect.triangle=t;
         }
     }
   return isect;  // return intersection point and normal
}

  void createScene()
   { triangles = new ArrayList<Triangle>();

   
     Cube.addCube(triangles, 0,35,0, 10,10,10,new RGB(0.3f,0,0),0.4f);       //rot, klein
     Cube.addCube(triangles, -70,-20,-20, 20,100,100,new RGB(0f,0,0.3f),.4f);
     Cube.addCube(triangles, -30,30,40, 20,20,20,new RGB(0,0.4f,0),0.2f);        // gr�n, klein
     Cube.addCube(triangles, 50,-20,-40, 10,80,100,new RGB(.5f,.5f,.5f), 0.2f);
     Cube.addCube(triangles, -70,-26,-40, 130,3,40,new RGB(.5f,.5f,.5f), 0.2f);


     Matrix mRx=Matrix.createXRotation((float) (x_angle_factor*Math.PI/16));
     Matrix mRy=Matrix.createYRotation((float) (y_angle_factor*Math.PI/16));
     Matrix mT=Matrix.createTranslation(0,0,200);
     Matrix m=mT.mult(mRx).mult(mRy);
     m.print();
     m.apply(triangles);
   }

}


