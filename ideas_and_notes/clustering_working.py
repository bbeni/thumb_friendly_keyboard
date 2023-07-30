import matplotlib.pyplot as plt
import numpy as np
from copy import deepcopy
from itertools import combinations
import time

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

        if (np.abs(det) < 0.000000000001):
            print('Warning calculating determinant!!', det)
            return False
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

def shif_polygon_fraction_to_point(polygon, point, fraction):
    for i in range(len(polygon)):
        a = polygon[i] - point
        polygon[i] = point + a*fraction
    return polygon

def construct_polygon_around_point(v0, v_other_list, max_nearest_neighbours=None):
    '''constructs the polygon surrounding a point
        by taking the middle perpendiculars (lines) between the point and all other points
        then constructs the minimum polygon using these lines
        similar to wigner seitz cell construction

        max_nearest_neighbours int'''

    perpends = []
    for i in range(len(v_other_list)):
        if np.allclose(v0, v_other_list[i]):
            continue
        perpends.append(get_middle_perpend(v0, v_other_list[i]))

    if max_nearest_neighbours:
        distances = [(p.point(0)-v0).dot(p.point(0)-v0) for p in perpends]
        perpends = np.array(perpends)[np.argsort(distances)][:max_nearest_neighbours]


    angles = [np.arctan((p.point(0)[0]-v0[0])/(p.point(0)[1]-v0[1])) for p in perpends]
    indices_sorted = np.argsort(angles)
    perpends_sorted = np.array(perpends)[indices_sorted]
    perpends_sorted = list(perpends_sorted)


    perps_to_delete = []
    polygon = []
    for a, b, c in combinations(perpends_sorted, 3):
        polygon = []
        for pa, pb in zip([a,b,c], [b,c,a]):
            exit_early = False
            intersection_point = pa.intersection_point(pb)
            if intersection_point is False:
                exit_early = True
                break
            polygon.append(intersection_point)
        
        if exit_early:
            continue

        ca, cb, cc = polygon
        if not Line(cb-ca, ca).is_point_left(v0) or \
            not Line(cc-cb, cb).is_point_left(v0) or \
            not Line(ca-cc, cc).is_point_left(v0):
            continue
        else:
            perps_to_delete = [a, b, c]
            break
    else:
        raise Exception('didnt find enclosing triangle')

    for p in perps_to_delete:
        perpends_sorted.remove(p)

    def cut_points(poly, line):
        points = []
        indices = []
        for i, c0, c1 in zip(range(1000), poly, poly[1:]+[poly[0]]):    
            if line.intersects_segment(c1, c0):
                ip = line.intersection_point(Line(c0-c1, c0))
                if ip is False:
                    return [], []
                points.append(ip)
                indices.append(i)

        # convex assertion
        assert(len(points) == 2 or len(points) == 0)
        return points, indices

    # for all other perpendiculars cutting the polygon
    # calclulate the intersection points and remove the ones cut away and add the two
    # note its always a convex polygon
    for perp in perpends_sorted:
        cps, indices = cut_points(polygon, perp)
        if len(cps) == 0:
            continue
        
        line = Line(cps[0]-cps[1], cps[0])
        if not line.is_point_left(v0):
            polygon[indices[0]+1:indices[1]+1] = cps
        else:
            polygon[:indices[0]+1], polygon[indices[1]+1:] = [cps[0]], [cps[1]]

    return polygon


def plot_example():
    plt.figure(figsize=(8,8))
    
    N = 100
    points_support = np.random.rand(N, 2)*2 - 1
    points = points_support[np.linalg.norm(points_support, axis=1) < 0.6 ]

    polygons = []
    for v0 in points:
        polygon = construct_polygon_around_point(v0, points_support)
        polygons.append(polygon)

    # plot polygons
    for polygon in polygons:
        for c0, c1 in zip(polygon, polygon[1:]+[polygon[0]]):
            plt.plot([c0[0], c1[0]], [c0[1],c1[1]], 'r')

    plt.scatter(points_support[:,0], points_support[:,1], label='support points')
    plt.scatter(points[:,0], points[:,1], label='points')

    plt.plot(v0[0], v0[1], 'ro')
    plt.xlim(-1,1)
    plt.ylim(-1,1)
    plt.legend()
    plt.show()


def keyboard_test():
    plt.figure(figsize=(10,10))
    
    keyboard_height = 0.76
    left_thumb_ankle = np.array([-0.6, 0])
    right_thumb_ankle = np.array([1+0.6, 0])

    max_angle = -np.arctan(keyboard_height/left_thumb_ankle[0])-0.1
    min_angle = 0.1
    angle_offset = 0.01

    angles = np.linspace(min_angle, max_angle, 6)
    radii = np.linspace(0.6+0.26, 0.6+0.48, 3)

    points = []
    for i, r in enumerate(radii):
        if (i%2 == 0):
            angles_new = angles + angle_offset
        else:
            angles_new = angles - angle_offset
        for angle in angles_new:
            x = r*np.cos(angle)
            y = r*np.sin(angle)
            points.append(np.array([x, y]) + left_thumb_ankle)


    angles_support = np.linspace(min_angle-0.1, max_angle+0.1, 11)
    radii_support = np.linspace(0.6+0.15, 0.6+0.6, 8)   

    points_support = []
    for angle in angles_support:
        r = radii_support[0]
        x = r*np.cos(angle)
        y = r*np.sin(angle)
        points_support.append(np.array([x, y]) + left_thumb_ankle)
        r = radii_support[-1]
        x = r*np.cos(angle)
        y = r*np.sin(angle)
        points_support.append(np.array([x, y]) + left_thumb_ankle)

    for r in radii_support[1:-1]:
        angle = angles_support[0]
        x = r*np.cos(angle)
        y = r*np.sin(angle)
        points_support.append(np.array([x, y]) + left_thumb_ankle)
        angle = angles_support[-1]
        x = r*np.cos(angle)
        y = r*np.sin(angle)
        points_support.append(np.array([x, y]) + left_thumb_ankle)

        
    points_support = np.array(points_support + points)
    points = np.array(points)

    polygons = []

    start_time_total = time.time_ns()
    for v0 in points:
        start_time = time.time_ns()
        polygon = construct_polygon_around_point(v0, points_support, max_nearest_neighbours=9)
        end_time = time.time_ns()
        print('took {}ns = {}s'.format(end_time-start_time, (end_time-start_time)/10**9))

        polygon = shif_polygon_fraction_to_point(polygon, v0, 0.95)
        polygons.append(polygon)

    total_time = time.time_ns() - start_time_total
    print('for all points took {}ns = {}s'.format(total_time, (total_time)/10**9))


    # plot polygons
    for polygon in polygons:
        for c0, c1 in zip(polygon, polygon[1:]+[polygon[0]]):
            plt.plot([c0[0], c1[0]], [c0[1],c1[1]], 'r')

    plt.scatter(points_support[:,0], points_support[:,1], label='support points')
    #plt.scatter(points[:,0], points[:,1], label='points')

    plt.xlim(-0.1,1.1)
    plt.ylim(-0.1,1.1)
    plt.vlines([0, 1], 0, keyboard_height)
    plt.hlines([0, keyboard_height], 0, 1)
    plt.legend()
    plt.show()

def keyboard_test2():
    plt.figure(figsize=(10,10))
    
    padding = 0.056
    keyboard_height = 0.66

    noise_level = 0.0000001

    x_space1 = np.linspace(padding,1-padding,9)
    x_space2 = np.linspace(padding*2,1-padding*2,8)
    y_space = np.linspace(padding, keyboard_height-padding, 7)


    points = []
    for i in range(1,6):
        if i%2 == 0:
            xspace = x_space1[1:-1]
        else:
            xspace = x_space2[1:-1]
        for x in xspace:
            points.append(np.array([x, y_space[i]]) + np.random.uniform(-noise_level, noise_level, size=(2,)))
        
    points_support = []

    for i in [0,6]:
        if i%2 == 0:
            xspace = x_space1
        else:
            xspace = x_space2
        for x in xspace:
            points_support.append(np.array([x, y_space[i]])+ np.random.uniform(-noise_level, noise_level, size=(2,)))

    for i in range(1,6):
        if i%2 == 0:
            xspace = x_space1
        else:
            xspace = x_space2
        for x in [xspace[0], xspace[-1]]:
            points_support.append(np.array([x, y_space[i]])+ np.random.uniform(-noise_level, noise_level, size=(2,)))


    points_support = np.array(points_support + points)
    points = np.array(points)

    polygons = []
    for v0 in points:
        polygon = construct_polygon_around_point(v0, points_support, max_nearest_neighbours=7)
        polygon = shif_polygon_fraction_to_point(polygon, v0, 0.9)
        polygons.append(polygon)

    # plot polygons
    for polygon in polygons:
        for c0, c1 in zip(polygon, polygon[1:]+[polygon[0]]):
            plt.plot([c0[0], c1[0]], [c0[1],c1[1]], 'r')

    plt.scatter(points_support[:,0], points_support[:,1], label='support points')
    plt.scatter(points[:,0], points[:,1], label='points')

    plt.xlim(-0.1,1.1)
    plt.ylim(-0.1,1.1)
    plt.vlines([0, 1], 0, keyboard_height)
    plt.hlines([0, keyboard_height], 0, 1)
    plt.legend()
    plt.show()




#plot_example()
keyboard_test()
#keyboard_test2()
