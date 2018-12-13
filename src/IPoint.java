class IPoint {
    final static float epsilon=0.0001f;
    Triangle triangle;
    Vec3D ipoint;
    float dist;
    IPoint(Triangle tt, Vec3D ip, float d) { triangle=tt; ipoint=ip; dist=d; }
}