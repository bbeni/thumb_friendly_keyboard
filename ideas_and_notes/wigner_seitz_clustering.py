import matplotlib.pyplot as plt
import numpy as np
from copy import deepcopy
from itertools import combinations



class Line:
    def __init__(self, a, b):
        self.a = a
        self.b = b

    def point(self, t):
        return self.a*t + self.b

    def intersection_point(self, line):
        c = line.b - self.b
        det = self.a[0]*line.a[1] - self.a[1]*line.a[0]
        t = 1/det*(line.a[1]*c[0] - line.a[0]*c[1])
        return self.point(t)
        
    def is_point_left(self, s):
        # a t + b = (x, y)
        # a1 t + b1 = x   and a2 t = -b2 + y
        # a1(y-b2)/a2 + b1 -x = 0
        # is 0 if point is on line
        # l = self.a[0] * (s[1] - self.b[1])/ self.a[1] + self.b[0] - s[0]
        # seems to be wrong ...... 
        # lets try crossproduct
        v0 = self.b - s
        v1 = self.a
        cp = v0[0]*v1[1] - v0[1]*v1[0]
        return cp > 0
    
    def dot_prod_direction(self, line):
        return (self.a[0]*line.a[0] + self.a[1]*line.a[1]) / (self.a[0]**2 +self.a[1]**2)**0.5/ (line.a[0]**2 +line.a[1]**2)**0.5
        
    def intersects_segment(self, s1, s2):
        # intersects if s1 and s2 are on other sides
        return self.is_point_left(s1) ^ self.is_point_left(s2)  

    def plot(self):
        t = np.linspace(-100,100,2)
        x0, y0 = self.point(t[0])
        x1, y1 = self.point(t[1]) 
        plt.plot([x0,x1],[y0,y1], 'b--')

def get_middle_perpend(a, b):
    d = b-a
    c = np.matmul(np.array([[0, -1],[1, 0]]), d)
    return Line(c, a+1/2*d)


def test1():
    N = 20
    points = np.random.rand(2, N)
    line = Line(points[:,0], points[:,1] - points[:,0])
    line2 = Line(points[:,3], points[:,4] - points[:,3])
    intersection = line.intersection_point(line2)

    plt.figure()        
    line.plot()
    line2.plot()
    plt.plot(intersection[0], intersection[1], 'ro')
    plt.scatter(points[0], points[1])
    plt.xlim(0,1)
    plt.ylim(0,1)
    plt.show()


def test2():
    N = 100
    points = np.random.rand(2, N)

    perpends = []
    v0 = points[:,0]
    for i in range(1, N):
        vi = points[:,i]
        perpends.append(get_middle_perpend(v0, vi))



    plt.figure(figsize=(8,8))       

    for p in perpends:
        p.plot()

    plt.scatter(points[0], points[1])
    plt.plot(points[0,0], points[1,0], 'ro')
    plt.xlim(0,1)
    plt.ylim(0,1)
    plt.show()

def test3():
    N = 100
    points = np.random.rand(2, N)

    perpends = []
    v0 = points[:,0]
    for i in range(1, N):
        vi = points[:,i]
        perpends.append(get_middle_perpend(v0, vi))


    middle_points_dist = [np.linalg.norm(p.point(0)-v0) for p in perpends]
    indices_sorted = np.argsort(middle_points_dist)
    print(indices_sorted)


    perpends_sorted = np.array(perpends)[indices_sorted]

    plt.figure(figsize=(8,8))

    for p in perpends_sorted[:10]:
        p.plot()


    for p in perpends_sorted[:10]:
        plt.plot(p.point(0)[0], p.point(0)[1], 'go')

    for p0, p1 in zip(perpends_sorted[:10], perpends_sorted[1:11]):
        intersection_point = p0.intersection_point(p1)
        plt.plot(intersection_point[0], intersection_point[1], 'rx')

    plt.scatter(points[0], points[1])
    plt.plot(points[0,0], points[1,0], 'ro')
    plt.xlim(0,1)
    plt.ylim(0,1)
    plt.show()
    
def test4():
    plt.figure(figsize=(8,8))
    
    N = 100
    points = np.random.rand(2, N)

    perpends = []
    v0 = np.array([0.5,0.5])
    for i in range(N):
        vi = points[:,i]
        perp = get_middle_perpend(v0, vi)
        perpends.append(perp)

    #middle_points_dist = [np.linalg.norm(p.point(0)-v0) for p in perpends]
    angles = [np.arctan((p.point(0)[0]-v0[0])/(p.point(0)[1]-v0[1])) for p in perpends]
    indices_sorted = np.argsort(angles)
    print(indices_sorted)
    perpends_sorted = np.array(perpends)[indices_sorted]

    # make triangle with 3 farthest points
    # p0, p1, p2 = perpends_sorted[:3]
    # polygon = [p1.intersection_point(p2), p2.intersection_point(p0), p1.intersection_point(p0)]

    # take the first triangle that is enclosing
    # does only work if v0 is enclosed!
    old_perpends_sorted = perpends_sorted.copy()
    updated_perpends_sorted = []
    ps = []
    for perp in perpends_sorted[::-1]:
        if len(ps) == 0:
            ps.append(perp)
            continue
        elif len(ps) < 3 and perp.dot_prod_direction(ps[-1]) < 1/2:
            ps.append(perp)
            continue
        updated_perpends_sorted.append(perp)

    p0, p1, p2 = ps
    polygon = [p1.intersection_point(p2), p2.intersection_point(p0), p1.intersection_point(p0)]
    perpends_sorted = updated_perpends_sorted

    def cut_points(poly, line):
        points = []
        indices = []
        for i, c0, c1 in zip(range(1000), poly, poly[1:]+[poly[0]]):    
            print(i, c0, c1)
            if line.intersects_segment(c1, c0):
                points.append(line.intersection_point(Line(c0-c1, c0)))
                indices.append(i)
        assert(len(points) == 2 or len(points) == 0)
        return points, indices

    # for all other perpendiculars cutting the polygon
    # calclulate the intersection points and remove the ones cut away and add the two
    # assume convex polygon
    for perp in perpends_sorted[:-1]:
        print("polygon is : ", polygon)
        cps, indices = cut_points(polygon, perp)
        if len(cps) == 0:
            continue

        plt.plot(cps[1][0], cps[1][1], 'kx')
        
        print(indices, cps)
        line = Line(cps[0]-cps[1], cps[0])
        if line.is_point_left(v0):
            print('is this side')
            polygon[indices[0]+1:indices[1]+1] = cps
        else:
            print('around')
            polygon[:indices[0]+1], polygon[indices[1]+1:] = [cps[0]], [cps[1]]



    print(p0.is_point_left(v0), p1.is_point_left(v0), p2.is_point_left(v0))

    for p in old_perpends_sorted:
        p.plot()

    for p in old_perpends_sorted[:]:
        plt.plot(p.point(0)[0], p.point(0)[1], 'go')

    # plot polygon
    for c0, c1 in zip(polygon, polygon[1:]+[polygon[0]]):
        plt.plot([c0[0], c1[0]], [c0[1],c1[1]], 'r')


    plt.scatter(points[0], points[1])
    plt.plot(v0[0], v0[1], 'ro')
    plt.xlim(0,1)
    plt.ylim(0,1)
    plt.show()




def test5():
    plt.figure(figsize=(8,8))
    
    N = 10
    points = np.random.rand(2, N)

    perpends = []
    v0 = np.array([0.5,0.5])
    for i in range(N):
        vi = points[:,i]
        perp = get_middle_perpend(v0, vi)
        perpends.append(perp)

    angles = [np.arctan((p.point(0)[0]-v0[0])/(p.point(0)[1]-v0[1])) for p in perpends]
    indices_sorted = np.argsort(angles)
    perpends_sorted = np.array(perpends)[indices_sorted]
    perpends_sorted = list(perpends_sorted)
    old_perpends_sorted = list(perpends_sorted)

    ps = []
    polygon = []
    for a, b, c in combinations(perpends_sorted, 3):
        polygon = []
        for pa, pb in zip([a,b,c], [b,c,a]):
            polygon.append(pa.intersection_point(pb))
        
        ca, cb, cc = polygon
        if not Line(cb-ca, ca).is_point_left(v0) or \
            not Line(cc-cb, cb).is_point_left(v0) or \
            not Line(ca-cc, cc).is_point_left(v0):
            continue
        else:
            ps = [a, b, c]
            break
    else:
        raise Exception('didnt find enclosing triangle')

    for p in ps:
        perpends_sorted.remove(p)


    start_polygon = deepcopy(polygon)
    def cut_points(poly, line):
        points = []
        indices = []
        for i, c0, c1 in zip(range(1000), poly, poly[1:]+[poly[0]]):    
            if line.intersects_segment(c1, c0):
                points.append(line.intersection_point(Line(c0-c1, c0)))
                indices.append(i)
        assert(len(points) == 2 or len(points) == 0)
        return points, indices

    # for all other perpendiculars cutting the polygon
    # calclulate the intersection points and remove the ones cut away and add the two
    # assume convex polygon
    for perp in perpends_sorted:
        cps, indices = cut_points(polygon, perp)
        if len(cps) == 0:
            continue

        plt.plot(cps[1][0], cps[1][1], 'kx')
        
        print(indices, cps)
        line = Line(cps[0]-cps[1], cps[0])
        if not line.is_point_left(v0):
            polygon[indices[0]+1:indices[1]+1] = cps
        else:
            polygon[:indices[0]+1], polygon[indices[1]+1:] = [cps[0]], [cps[1]]

    for p in old_perpends_sorted:
        p.plot()

    for p in old_perpends_sorted[:]:
        plt.plot(p.point(0)[0], p.point(0)[1], 'go')

    # plot polygon
    for c0, c1 in zip(polygon, polygon[1:]+[polygon[0]]):
        plt.plot([c0[0], c1[0]], [c0[1],c1[1]], 'r')

    # plot start polygon
    for c0, c1 in zip(start_polygon, start_polygon[1:]+[start_polygon[0]]):
        plt.plot([c0[0], c1[0]], [c0[1],c1[1]], 'r-.')




    plt.scatter(points[0], points[1])
    plt.plot(v0[0], v0[1], 'ro')
    plt.xlim(0,1)
    plt.ylim(0,1)
    plt.show()





#test1()
#test2()
#test3()
#test4()
test5()



