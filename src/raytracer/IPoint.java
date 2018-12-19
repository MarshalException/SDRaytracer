package raytracer;

class IPoint {
    static final float epsilon=0.0001f;
    Triangle triangle;
    Vec3D ipoint_var;
    float dist;
    IPoint(Triangle tt, Vec3D ip, float d) { triangle=tt; ipoint_var=ip; dist=d; }
}