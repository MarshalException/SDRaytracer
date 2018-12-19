package raytracer;

import java.awt.*;

class RGB {
    float red;
    float green;
    float blue;
    Color color;

    RGB() {}

    RGB(float r, float g, float b)
    { if (r>1) {r=1;} else if (r<0) {r=0;}
        if (g>1) {g=1;} else if (g<0) {g=0;}
        if (b>1) {b=1;} else if (b<0) {b=0;}
        red=r; green=g; blue=b;
    }

    Color rgbcolor()
    { if (color!=null) {return color;}
        color=new Color((int) (red*255),(int) (green*255), (int) (blue*255));
        return color;
    }

    //Refactoring
    RGB addColors(RGB c1, RGB c2, float ratio)
    { return new RGB( (c1.red+c2.red*ratio),
            (c1.green+c2.green*ratio),
            (c1.blue+c2.blue*ratio));
    }

    RGB lighting(Ray ray, IPoint ip, int rec, RGB ambientcolor, Vec3D[] lights, SDRaytracer raytracer) {
        Vec3D point=ip.ipointVar;
        Triangle triangle=ip.triangle;
        RGB color = addColors(triangle.color,ambientcolor,1);
        Ray shadowRay=new Ray();
        for(Vec3D light : lights)
        { shadowRay.start=point;
            shadowRay.dir=light.minus(point).mult(-1);
            shadowRay.dir.normalize();
            IPoint ip2=raytracer.hitObject(shadowRay);
            if(ip2.dist<IPoint.EPSILON)
            {
                float ratio=Math.max(0,shadowRay.dir.dot(triangle.normal));
                color = addColors(color,light.color,ratio);
            }
        }
        Ray reflection=new Ray();
        //R = 2N(N*L)-L)    L ausgehender Vektor
        Vec3D LVec=ray.dir.mult(-1);
        reflection.start=point;
        reflection.dir=triangle.normal.mult(2*triangle.normal.dot(LVec)).minus(LVec);
        reflection.dir.normalize();
        RGB rcolor=raytracer.rayTrace(reflection, rec+1);
        float ratio =  (float) Math.pow(Math.max(0,reflection.dir.dot(LVec)), triangle.shininess);
        color = addColors(color,rcolor,ratio);
        return(color);
    }

}