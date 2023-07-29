import matplotlib.pyplot as plt
import numpy as np



class Line:
	def __init__(self, a, b):
		self.a = a
		self.b = b

	def point(self, x):
		return self.a*x + self.b

	def intersection_point(self, line):
		c = line.b - self.b
		det = self.a[0]*line.a[1] - self.a[1]*line.a[0]
		t = 1/det*(line.a[1]*c[0] - line.a[0]*c[1])
		return self.point(t)

	def plot(self):
		t = np.linspace(-10,10,2)
		print(self.point(1))
		x0, y0 = self.point(t[0])
		x1, y1 = self.point(t[1]) 
		plt.plot([x0,x1],[y0,y1])

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
		print(p.point(0))
		plt.plot(p.point(0)[0], p.point(0)[1], 'go')

	plt.scatter(points[0], points[1])
	plt.plot(points[0,0], points[1,0], 'ro')
	plt.xlim(0,1)
	plt.ylim(0,1)
	plt.show()

test3()