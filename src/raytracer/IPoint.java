package raytracer;

class IPoint {
    static final float EPSILON=0.0001f;
    Triangle triangle;
    Vec3D ipointVar;
    float dist;
    IPoint(Triangle tt, Vec3D ip, float d) { triangle=tt; ipointVar=ip; dist=d; }
}