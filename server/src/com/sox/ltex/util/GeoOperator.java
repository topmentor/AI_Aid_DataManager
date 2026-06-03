/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sox.ltex.util;


import com.ithows.ResultMap;
import com.sox.ltex.model.ShapeType;
import com.sox.ltex.model.shape.GPoint;
import com.sox.ltex.model.shape.MBR;
import java.util.ArrayList;
import java.util.Random;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

/**
 * 지오메트리(Geometry) 연산을 지원하는 클래스
 *
 */
public class GeoOperator {


	public static void main(String[] args) {
		GPoint pt1 = new GPoint(0,0);
		GPoint pt2 = new GPoint(100,100);
		GPoint pt3 = new GPoint(0,100);

		ArrayList<GPoint> poly = new ArrayList<>();
		poly.add(pt1);
		poly.add(pt2);
		poly.add(pt3);

		System.out.println( getMovingPoints(poly, 10)  );

	}


    // 좌표기준 군집을 만들고 MBR 리턴     
    // maxIter는 50이나 100정도가 안정적    
    public static ArrayList<GeoCluster> clusterByKMeans(ArrayList<ResultMap> list, int k, int maxIter) {
        int n = list.size();
        if (n == 0 || k <= 0){
            return null;
        }

        ArrayList<double[]> points = new ArrayList<>();
        for (ResultMap rm : list) {
            double lat = Double.parseDouble(rm.get("target_latitude").toString());
            double lng = Double.parseDouble(rm.get("target_longitude").toString());
            points.add(new double[]{lat, lng});
        }

        // k가 1이면 K-Means 반복 없이 전체 포인트를 감싸는 MBR을 직접 계산
        if (k <= 1) {
            double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
            double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
            double sumLat = 0.0, sumLng = 0.0;

            for (double[] p : points) {
                double lat = p[0];
                double lng = p[1];
                minX = Math.min(minX, lng);
                minY = Math.min(minY, lat);
                maxX = Math.max(maxX, lng);
                maxY = Math.max(maxY, lat);
                sumLat += lat;
                sumLng += lng;
            }

            double centroidLat = sumLat / n;
            double centroidLng = sumLng / n;

            ArrayList<GeoCluster> single = new ArrayList<>();
            single.add(new GeoCluster(new MBR(minX, minY, maxX, maxY), centroidLng, centroidLat));
            return single;
        }

        // k가 1이면 전체 포인트를 감싸는 단일 클러스터를 바로 계산한다.
        if (k <= 1) {
            ArrayList<GPoint> clusterPoints = new ArrayList<>();
            double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
            double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
            double sumLat = 0.0, sumLng = 0.0;

            for (double[] p : points) {
                double lat = p[0];
                double lng = p[1];
                minX = Math.min(minX, lng);
                minY = Math.min(minY, lat);
                maxX = Math.max(maxX, lng);
                maxY = Math.max(maxY, lat);
                sumLat += lat;
                sumLng += lng;
                clusterPoints.add(new GPoint(lng, lat));
            }

            double centroidLat = sumLat / n;
            double centroidLng = sumLng / n;

            GeoCluster cluster = new GeoCluster(new MBR(minX, minY, maxX, maxY), centroidLng, centroidLat);
            ArrayList<GeoCluster> result = new ArrayList<>();
            result.add(cluster);
            return result;
        }

        // --- 1. 초기 중심 랜덤 선택
        Random rnd = new Random();
        ArrayList<double[]> centroids = new ArrayList<>();
        for (int i = 0; i < k; i++) {
            centroids.add(points.get(rnd.nextInt(n)).clone());
        }

        int[] labels = new int[n];

        // --- 2. K-Means 반복 수행
        for (int iter = 0; iter < maxIter; iter++) {
            boolean changed = false;

            // (1) 각 점의 최근접 중심 찾기
            for (int i = 0; i < n; i++) {
                double[] p = points.get(i);
                int bestCluster = 0;
                double bestDist = distance(p, centroids.get(0));

                for (int c = 1; c < k; c++) {
                    double dist = distance(p, centroids.get(c));
                    if (dist < bestDist) {
                        bestDist = dist;
                        bestCluster = c;
                    }
                }

                if (labels[i] != bestCluster) {
                    labels[i] = bestCluster;
                    changed = true;
                }
            }

            if (!changed) break;  // 변화 없으면 끝

            // (2) 중심 업데이트
            double[][] sum = new double[k][2];
            int[] count = new int[k];

            for (int i = 0; i < n; i++) {
                int cluster = labels[i];
                sum[cluster][0] += points.get(i)[0];
                sum[cluster][1] += points.get(i)[1];
                count[cluster]++;
            }

            for (int c = 0; c < k; c++) {
                if (count[c] > 0) {
                    centroids.get(c)[0] = sum[c][0] / count[c];
                    centroids.get(c)[1] = sum[c][1] / count[c];
                }
            }
        }

        // --- 3. ������ bounding box + centroid ���
        ArrayList<GeoCluster> results = new ArrayList<>();

        for (int c = 0; c < k; c++) {
            MBR clusterMbr = new MBR();
            clusterMbr.reset();
            ArrayList<GPoint> clusterPoints = new ArrayList<>();

            double sumLat = 0.0, sumLng = 0.0;
            int count = 0;

            for (int i = 0; i < n; i++) {
                if (labels[i] == c) {
                    double lat = points.get(i)[0];
                    double lng = points.get(i)[1];

                    GPoint gp = new GPoint(lng, lat); // x=lng, y=lat
                    clusterMbr.append(gp);
                    clusterPoints.add(gp);

                    sumLat += lat;
                    sumLng += lng;
                    count++;
                }
            }

            if (count == 0) continue; // ����ִ� ���� ����

            double centroidLat = sumLat / count;
            double centroidLng = sumLng / count;

            GeoCluster cluster = new GeoCluster(new MBR(clusterMbr), centroidLng, centroidLat);
            results.add(cluster);
        }


        return results;
    }

    
    private static double distance(double[] p1, double[] p2) {
        double a = p1[0] - p2[0];
        double b = p1[1] - p2[1];
        return a * a + b * b;
    }
        
        
        

	///////////////////////////////////////////////////////////////////////
	//  Calculate Angle, Radian

	/**
	 * 두 포인트 간의 각도를 구합니다.
	 * @param start 첫번째 포인트
	 * @param end 두번째 포인트
	 * @return 두 포인트 간의 각도 (라디안 = theta)
	 */
	public static double getAngle(GPoint start, GPoint end) {
		return Math.atan2(end.y-start.y, end.x-start.x);// * (180.0 / Math.PI);
	}

	public static double getRadian(double degree){
//		double d2r = Math.PI / 180;   // degrees to radians
		degree %= 360;
		return Math.toRadians(degree);
	}

	public static double getDegrees(double radian){
//		double r2d = 180 / Math.PI;   // radians to degrees
		return Math.toDegrees(radian);
	}

	/**
	 * 시작점과 끝점으로 구성된 하나의 선분에 대해 그 선분의 시작점을 중심으로 radian만큼 회전된 포인트를 구합니다.
	 * @param pivot 선분의 시작점이면서 회전의 중심점
	 * @param point 선분의 끝점
	 * @param radian
	 * @return 회전된 포인트
	 */
	public static GPoint getPointByAngle(GPoint pivot, GPoint point, double radian) {
		double x = (point.x - pivot.x);
		double y = (point.y - pivot.y);
		
		double rx = (Math.cos(radian)*x - Math.sin(radian)*y);
		double ry = (Math.sin(radian)*x + Math.cos(radian)*y);
		
		return new GPoint(rx + pivot.x, ry + pivot.y);
	}



	///////////////////////////////////////////////////////////////////////
	//  Calculate Area

	
	/**
	 * 주어진 폴리곤의 부호가 있는 넓이를 구합니다.
	 * @param points 폴리곤의 구성 좌표
	 * @return 부호가 있는 폴리곤의 넓이
	 */
	public static double getRingArea(ArrayList<GPoint> points) {
		int next;
		double partArea = 0.0;
		double AX, AY, BX, BY;
		
		int cntVertex = points.size();
		for (int i=0; i<cntVertex; i++) {
			next = (i+1)%cntVertex;

			GPoint vtxA = points.get(i);
			GPoint vtxB = points.get(next);

			AX = vtxA.x;
			AY = vtxA.y;
			BX = vtxB.x;
			BY = vtxB.y;
			
			partArea += AX * BY;
			partArea -= AY * BX;
		}

		partArea /= 2.0;

		return partArea;		
	}



	///////////////////////////////////////////////////////////////////////
	//  Find Center Point, Label Point

	
	/**
	 * 폴리곤의 무게중심 좌표를 구합니다.
	 * @param points 폴리곤의 좌표값 리스트
	 * @return 무게중심 좌표
	 */
	public static GPoint getCentroidPolygon(ArrayList<GPoint> points) {
		double Area = getRingArea(points);
		double factor;
		double AX, AY, BX, BY;
		int N = points.size();
		int i, j;
		double cx = 0.0f, cy = 0.0f;
		
		for (i=0; i<N; i++) {
			j = (i + 1) % N;
			GPoint vtxA = points.get(i);
			GPoint vtxB = points.get(j);
			
			AX = vtxA.x;
			AY = vtxA.y;
			BX = vtxB.x;
			BY = vtxB.y;
			
			factor = (AX*BY-BX*AY);
			cx += (AX+BX)*factor;
			cy += (AY+BY)*factor;
		}

		Area *= 6.0;
		factor = 1.0/Area;
		cx *= factor;
		cy *= factor;

		return new GPoint(cx, cy);
	}

	/**
	 * 폴리라인에 대한 라벨을 표시할 위치를 얻습니다.
	 */
	public static GPoint getCentroidPolyline(ArrayList<GPoint> coordsList) {
		int cntCoord = coordsList.size();
		if (cntCoord < 2) {
			return null;
		}

		if (cntCoord == 2) {
			GPoint coord1 = coordsList.get(0);
			GPoint coord2 = coordsList.get(1);

			return new GPoint((coord1.x + coord2.x) / 2, (coord1.y + coord2.y) / 2);
		}

		int halfIndex = (int)(cntCoord / 2.0);
		GPoint centerCoord = coordsList.get(halfIndex);

		return new GPoint(centerCoord.x, centerCoord.y);
	}




	///////////////////////////////////////////////////////////////////////
	//  Distance

	public static double getDistanceWGSPoints(GPoint Pt1, GPoint Pt2){
            return CoordTransformUtil.getDistanceBetween(Pt2, Pt2, true);
        }
        
	/**
	 * 포인트 사이의 거리를 구합니다.
	 * @param Pt1 첫번재 좌표
	 * @param Pt2 두번째 좌표
	 * @return 포인트간 거리
	 */
	public static double getDistanceBetweenPointAndPoint(GPoint Pt1, GPoint Pt2){
		return Math.sqrt(Math.pow(Pt1.x - Pt2.x, 2.0) +  Math.pow(Pt1.y - Pt2.y, 2.0));
	}

	/**
	 * 포인트와 선분 사이의 거리를 구합니다.
	 * @param linePt1 선분을 구성하는 첫번재 좌표
	 * @param linePt2 선분을 구성하는 두번째 좌표
	 * @param pt 포인트
	 * @return 포인트와 선분 사이의 거리
	 */
	public static double getDistanceBetweenLineAndPoint(GPoint linePt1, GPoint linePt2, GPoint pt)
	{
		double LineMag = Math.sqrt(Math.pow(linePt2.x - linePt1.x, 2.0) +  Math.pow(linePt2.y - linePt1.y, 2.0));
		double U = (((pt.x-linePt1.x) * (linePt2.x-linePt1.x)) +
				((pt.y-linePt1.y) * (linePt2.y-linePt1.y))) /
				(LineMag * LineMag);

		if( U < 0.0 || U > 1.0 ) return -1.0;

		double intX = linePt1.x + U * ( linePt2.x - linePt1.x);
		double intY = linePt1.y + U * ( linePt2.y - linePt1.y);

		return Math.sqrt(Math.pow(intX - pt.x, 2.0) +  Math.pow(intY - pt.y, 2.0));
	}

	/**
	 * Polyline과 포인트 간 거리 측정
	 * @param points
	 * @param pt
	 * @return
	 */

	public static double  getDistanceBetweenPolylineAndPoint(ArrayList<GPoint> points, GPoint pt) {
		int n = points.size();
		GPoint p1, p2;
		double dist;
		double min = 9999999;

		for(int i=1; i<n; i++) {
			p1 = points.get(i-1);
			p2 = points.get(i);

			dist = getDistanceBetweenLineAndPoint(p1, p2, pt);
			if(min > dist && dist > 0){
				min = dist;
			}

		}
		return min;
	}



	/**
	 * 폴리곤(Ring) 과 포인트간 거리
	 * @param points
	 * @param pt
	 * @return
	 */
	public static double getDistanceBetweenRingAndPoint(ArrayList<GPoint> points, GPoint pt)
	{
		double min = 99999999;

		if(pointInPolygon(points,pt) == true){
			min = 0;
		}else{
			min = getDistanceBetweenPolylineAndPoint(points,pt);
		}

		return min;
	}


	/**
	 *  라인상에서 포인트와 가장 가까운 지점 찾기
	 * @param pt  : 포인트
	 * @param lineSPt : 라인 시작점
	 * @param lineEPt : 라인 끝점
	 * @return
	 */
	public static GPoint findDistanceToLine(GPoint pt , GPoint lineSPt , GPoint lineEPt ){
		GPoint closestPt = new GPoint();
		double DX, DY, t;
		double dist;
		DX = lineEPt.x - lineSPt.x ;
		DY = lineEPt.y - lineSPt.y ;

		if ((DX == 0) && (DY == 0)){
			closestPt.x = lineSPt.x;
			closestPt.y = lineSPt.y;
			DX = pt.x - lineSPt.x;
			DY = pt.y - lineSPt.y;
			dist = Math.sqrt(DX * DX + DY * DY);
		}

		// Calculate the t that minimizes the distance.
		t = ((pt.x - lineSPt.x) * DX + (pt.y - lineSPt.y) * DY) / (DX * DX + DY * DY);

		// See if this represents one of the segment's
		// end points or a point in the middle.
		if (t < 0 ){
			closestPt.x = lineSPt.x;
			closestPt.y = lineSPt.y;
			DX = pt.x - lineSPt.x;
			DY = pt.y - lineSPt.y;
		} else if( t>1 ){
			closestPt.x = lineEPt.x;
			closestPt.y = lineEPt.y;
			DX = pt.x - lineEPt.x;
			DY = pt.y - lineEPt.y;
		}else{
			closestPt.x = lineSPt.x + t * DX;
			closestPt.y = lineSPt.y + t * DY;
			DX = pt.x - closestPt.x;
			DY = pt.y - closestPt.y;
		}

		dist = Math.sqrt(DX * DX + DY * DY);

		return closestPt;
	}




	///////////////////////////////////////////////////////////////////////
	//  Intersect & Contain


	/**
	 * 포인트가 사각영역에 포함되는 지 확인
	 * @param pt
	 * @param rect
	 * @return
	 */
	public static boolean pointInRect(GPoint pt, MBR rect)
	{
		if(rect.minX > pt.x || rect.maxX < pt.x
				|| rect.minY > pt.y || rect.maxY < pt.y)
			return false;

		return true;
	}

	/**
	 * 2개의 포인트 사이의 거리가 임계값보다 작은지 검사합니다.
	 * @param pt1 첫번째 포인트
	 * @param pt2 두번째 포인트
	 * @param tol 임계값
	 * @return 포인트 사이의 거리가 임계값보다 작을 경우 true를 반환
	 */
	public static boolean pointInPoint(GPoint pt1, GPoint pt2, double tol) {
		double distance = Math.sqrt(Math.pow(pt1.x-pt2.x, 2.0) + Math.pow(pt1.y-pt2.y, 2.0));
		return distance < tol;
	}

	/**
	 * 폴리라인과 어떤 포인트 사이의 최소 거리가 임계값보다 작은지 검사합니다.
	 * 어떤 포인트가 폴리라인 상에 존재하는지를 허용치(tol)를 사용하여 검사합니다.
	 * @param points 폴리라인을 구성하는 포인트
	 * @param pt 어떤 포인트
	 * @param tol 임계값
	 * @return 최소 거리가 임계값보다 작을 경우 true를 반환
	 */
	public static boolean  pointInPolyline(ArrayList<GPoint> points, GPoint pt, double tol) {
		int n = points.size();
		GPoint p1, p2;

		for(int i=1; i<n; i++) {
			p1 = points.get(i-1);
			p2 = points.get(i);

			double dist = getDistanceBetweenLineAndPoint(p1, p2, pt);
			if(dist > 0.0 && dist < tol) {
				return true;
			}
		}

		return false;
	}



	/**
	 * 폴리곤이 어떤 포인트를 포함하고 있는지 검사합니다.
	 * @param points 폴리곤을 구성하는 포인트
	 * @param pt 어떤 포인트
	 * @return 폴리곤이 어떤 포인트를 포함할 경우 true 반환
	 */
	public static boolean  pointInPolygon(ArrayList<GPoint> points, GPoint pt) {
		int n = points.size();
		int i, j = n-1;
		boolean oddNodes = false;
		GPoint p1, p2;

		for(i=0; i<n; i++) {
			p1 = points.get(i);
			p2 = points.get(j);

			if((p1.y< pt.y && p2.y>=pt.y || p2.y< pt.y && p1.y>=pt.y)
					&& (p1.x<=pt.x || p2.x<=pt.x)) {
				if(p1.x+(pt.y-p1.y)/(p2.y-p1.y)*(p2.x-p1.x)<pt.x) {
					oddNodes=!oddNodes;
				}
			}

			j = i;
		}

		return oddNodes;
	}


	/**
	 * 어떤 포인트에서 하나의 선분에 이르는 가장 가까운 거리가 임계값보다 작을때 만나는 점을 구합니다.
	 * @param P1 하나의 선분에 대한 시작점
	 * @param P2 하나의 선분에 대한 끝점
	 * @param Pt 어떤 포인트
	 * @param tol 임계값
	 * @return 만나는 점으로 거리가 임계값보다 클때 null 반환
	 */
	public static GPoint getIntersectPointFromLine(GPoint P1, GPoint P2, GPoint Pt, double tol)
	{
		if(P1.x != P2.x || P1.y != P2.y)
		{
			double LineMag = Math.sqrt((P1.x-P2.x)*(P1.x-P2.x) + (P1.y-P2.y)*(P1.y-P2.y));

			double U = ( ( ( Pt.x - P1.x ) * ( P2.x - P1.x ) ) +
					( ( Pt.y - P1.y ) * ( P2.y - P1.y ) )) /
					( LineMag * LineMag );

			if( U < 0.0 || U > 1.0 ) return null;

			GPoint itPt = new GPoint(P1.x + U * ( P2.x - P1.x), P1.y + U * ( P2.y - P1.y));
			double dist = Math.sqrt((Pt.x-itPt.x)*(Pt.x-itPt.x) + (Pt.y-itPt.y)*(Pt.y-itPt.y));
			if(dist > tol)
			{
				return null;
			}
			else
			{
				return itPt;
			}
		}

		return null;
	}


	/**
	 * 두 개의 ring의 교차여부 확인
	 * @param ring1
	 * @param ring2
	 * @return
	 */
	public static boolean checkRingsIntersect(ArrayList<GPoint>  ring1, ArrayList<GPoint>  ring2)
	{
		int c1,v1,c2,v2;

		/* STEP 1: look for intersecting line segments */
		int NumPoints1, NumPoints2;
		NumPoints1 = ring1.size();
		NumPoints2 = ring2.size();

		for(v1=1; v1<NumPoints1; v1++ )
		{
			for(v2=1; v2<NumPoints2; v2++ )
			{
				if(checkLinesIntersect(ring1.get(v1-1), ring1.get(v1),
						ring2.get(v2-1), ring2.get(v2)) ==  true)
					return true;
			}
		}
		/*
		 ** At this point we know there are are no intersections between edges. However, one polygon might
		 ** contain portions of the other. There may be a case that the following two steps don't catch, but
		 ** I haven't found one yet.
		 */

		/* STEP 2: polygon 1 completely contains 2 (only need to check one point from each part) */
		for(c2=0; c2<ring1.size(); c2++) {
			if(checkPointRingIntersect(ring1.get(c2), ring2) == true)
			return true;
		}

		/* STEP 3: polygon 2 completely contains 1 (only need to check one point from each part) */
		for(c1=0; c1<ring2.size(); c1++) {
			if(checkPointRingIntersect(ring2.get(c2), ring1) == true)
				return true;
		}

		return false;
	}

	/**
	 * 두 개의 polyline의 교차 여부 확인
	 * @param line1
	 * @param line2
	 * @return
	 */
	public static boolean  checkPolylinesIntersect(ArrayList<GPoint>  line1, ArrayList<GPoint>  line2)
	{
		int c1,v1,c2,v2;
		int NumPoints1, NumPoints2;

		NumPoints1 = line1.size();
		NumPoints2 = line2.size();

		for(v1=1; v1<NumPoints1; v1++ )
		{
				for(v2=1; v2<NumPoints2; v2++ )
				{
					if(checkLinesIntersect(line1.get(v1-1), line1.get(v1),
							line2.get(v2-1), line2.get(v2)) ==  true)
						return true;
				}
		}



		return false;
	}


	/**
	 * Polygon과 ployline의 교차여부 확인
	 * @param line
	 * @param polygon
	 * @return
	 */
	public static boolean  checkPolylinePolygonIntersect(ArrayList<GPoint>  line, ArrayList<GPoint>  polygon)
	{
		int c1,v1,c2,v2;
		int NumPoints1, NumPoints2;

		NumPoints1 = line.size();
		NumPoints2 = polygon.size();

		for(v1=1; v1<NumPoints1; v1++ )
		{
				for(v2=1; v2<NumPoints2; v2++ )
				{
					if(checkLinesIntersect(line.get(v1-1), line.get(v1),
							polygon.get(v2-1), polygon.get(v2)) ==  true)
						return true;
				}
		}

		/* STEP 2: polygon 1 completely contains 2 (only need to check one point from each part) */
		for(c2=0; c2<line.size(); c2++) {
			if(checkPointRingIntersect(line.get(c2), polygon) == true)
				return true;
		}

		return false;
	}


	public static boolean checkPointRingIntersect(GPoint point, ArrayList<GPoint> poly)
	{
		int i;
		boolean status=false;
		int NumPoints;

		if(pointInPolygon(poly, point) == true){
			status = !status;
		}

		return status;
	}

        

	public static boolean checkPointPolyonIntersect(double x, double y, Geometry poly){
		GPoint point = new GPoint(x,y);
		return checkPointPolyonIntersect(point, poly);
	}

	public static boolean checkPointPolyonIntersect(GPoint point, Geometry poly)
	{
		GeometryFactory geometryFactory = new GeometryFactory();
		Point pt =  geometryFactory.createPoint(new Coordinate(point.x, point.y));
		return poly.intersects(pt) ;
	}


	public static boolean checkSimplePolygonIntersect(ArrayList<GPoint>  ring1, ArrayList<GPoint>  ring2)
	{

		int NumPoints1, NumPoints2;
		NumPoints1 = ring1.size();
		NumPoints2 = ring2.size();

		if(NumPoints1 ==0 | NumPoints2 == 0){
			return false;
		}

		Geometry poly1 = JTSBasicOperator.convertToGeometry(ring1, ShapeType.Polygon);
		Geometry poly2 = JTSBasicOperator.convertToGeometry(ring2, ShapeType.Polygon);

		boolean res = poly1.intersects(poly2);

		return res;
	}

	public static boolean checkPolygonIntersect(Geometry poly1, Geometry poly2)
	{
		boolean res = poly1.intersects(poly2);
		return res;
	}

	// mbr이 폴리곤에 닿는지 확인
	public static boolean mbrIntersect(Geometry area, MBR mbr){
		Geometry mbrPoly = JTSBasicOperator.mbrToGeometry(mbr);
		return area.intersects(mbrPoly);
	}
        
        

	private static double S_MIN (double a, double b){
		return a < b ? a : b;
	}
	private static double S_MAX (double a, double b){
		return a > b ? a : b;
	}

	private static double S_ROUND (double a, double b){
		return a > 0 ?  (int)( a + 0.5) : -(int)(0.5- a);
	}

	/**
	 * 두 라인의 교차를 확인
	 * @param a 라인1의 시작점
	 * @param b 라인1의 끝점
	 * @param c 라인2의 시작점
	 * @param d 라인2의 끝점
	 * @return
	 */
	public static boolean checkLinesIntersect(GPoint a, GPoint b, GPoint c, GPoint d)
	{ /* from comp.graphics.alogorithms FAQ */

		double r, s;
		double denominator, numerator;

		numerator = ((a.y-c.y)*(d.x-c.x) - (a.x-c.x)*(d.y-c.y));
		denominator = ((b.x-a.x)*(d.y-c.y) - (b.y-a.y)*(d.x-c.x));

		if((denominator == 0) && (numerator == 0)) { /* lines are coincident, intersection is a line segement if it exists */
			if(a.y == c.y) { /* coincident horizontally, check x's */
				if(((a.x >= S_MIN(c.x,d.x)) && (a.x <= S_MAX(c.x,d.x)))
						|| ((b.x >= S_MIN(c.x,d.x)) && (b.x <= S_MAX(c.x,d.x))))
					return(true);
				else
					return(false);
			} else { /* test for y's will work fine for remaining cases */
				if(((a.y >= S_MIN(c.y,d.y)) && (a.y <= S_MAX(c.y,d.y)))
						|| ((b.y >= S_MIN(c.y,d.y)) && (b.y <= S_MAX(c.y,d.y))))
					return(true);
				else
					return(false);
			}
		}

		if(denominator == 0) /* lines are parallel, can't intersect */
			return false;

		r = numerator/denominator;

		if((r<0) || (r>1))
			return false; /* no intersection */

		numerator = ((a.y-c.y)*(b.x-a.x) - (a.x-c.x)*(b.y-a.y));
		s = numerator/denominator;

		if((s<0) || (s>1))
			return false; /* no intersection */

		return true;
	}

	/**
	 * 라인과 원 간의 교점을 찾는 함수
	 * @param sPoint 라인의 시작점
	 * @param ePoint 라인의 끝점
	 * @param center 원 중심점
	 * @param radius 원 반경
	 * @return
	 */
	public static ArrayList<GPoint> getLineCircleIntersect(GPoint sPoint, GPoint ePoint, GPoint center, double radius) {

		ArrayList<GPoint> ptList = new ArrayList<GPoint>();
		double baX = ePoint.x - sPoint.x;
		double baY = ePoint.y - sPoint.y;
		double caX = center.x - sPoint.x;
		double caY = center.y - sPoint.y;

		double a = baX * baX + baY * baY;
		double bBy2 = baX * caX + baY * caY;
		double c = caX * caX + caY * caY - radius * radius;

		double pBy2 = bBy2 / a;
		double q = c / a;

		double disc = pBy2 * pBy2 - q;
		if (disc < 0) {
			return null;
		}
		// if disc == 0 ... dealt with later
		double tmpSqrt = Math.sqrt(disc);
		double abScalingFactor1 = -pBy2 + tmpSqrt;
		double abScalingFactor2 = -pBy2 - tmpSqrt;

		GPoint p1 = new GPoint(sPoint.x - baX * abScalingFactor1, sPoint.y - baY * abScalingFactor1);
		ptList.add(p1);

		if (disc == 0) { // abScalingFactor1 == abScalingFactor2
			return null;
		}

		GPoint p2 = new GPoint(sPoint.x - baX * abScalingFactor2, sPoint.y - baY * abScalingFactor2);
		ptList.add(p2);

		return ptList;
	}


	//////////////////////////////////////////////////////////////////////////
	// Convert Shape



	// 원을 폴리곤으로 변환 (32개 버텍스를 가짐)
	public static ArrayList<GPoint> circleToPolygon(GPoint pt, int radius){

		int points = 32;
		double ex, ey, theta;

		ArrayList<GPoint> pointList = new ArrayList<GPoint>();

		for (int i=0; i < points; i++) // one extra here makes sure we connect the
		{
			theta = Math.PI * ((double)i / (points/2));
			ex = pt.x + (radius * Math.cos(theta)); // center a + radius x * cos(theta)
			ey = pt.y + (radius * Math.sin(theta)); // center b + radius y * sin(theta)
			pointList.add(new GPoint(ex, ey));
		}

		pointList.add(pointList.get(0).copy());

		return pointList;

	}

	/**
	 * Polyline의 모든 Edge를 돌면서 이동 지점을 수집 함
	 * @param polyline
	 * @param moveDistance
	 * @return
	 */

	public static ArrayList<GPoint> getMovingPoints(ArrayList<GPoint> polyline, double moveDistance ){
		ArrayList<GPoint> ptList = new ArrayList<GPoint>();
		double remainDist;
		double inDist;

		for(int i=0; i < polyline.size()-1 ; i++){
			ptList.add(polyline.get(i));
			remainDist = getDistanceBetweenPointAndPoint(polyline.get(i), polyline.get(i+1));
			inDist = moveDistance;
			while(remainDist > moveDistance){
				ptList.add(getPointFarDistance(polyline.get(i), polyline.get(i+1), inDist ) );
				remainDist -= moveDistance;
				inDist += moveDistance;
			}

			ptList.add(polyline.get(i+1));
		}


		return ptList;
	}

	/**
	 * 라인의 시작점에서 일정 거리 떨어진 라인상의 점 찾기
	 * @param sPoint  라인의 시작점
	 * @param ePoint  라인의 끝점
	 * @param radius  떨어진 거리
	 * @return
	 */
	public static GPoint getPointFarDistance(GPoint sPoint, GPoint ePoint, double radius) {

		ArrayList<GPoint> ptList = getLineCircleIntersect(sPoint, ePoint, sPoint, radius);

		if(withinMBR(sPoint, ePoint, ptList.get(0) ) == true ){

			return  ptList.get(0);

		}else if(withinMBR(sPoint, ePoint, ptList.get(1)) == true ){

			return ptList.get(1);

		}

		return null;

	}


	public static boolean withinMBR(GPoint pt1, GPoint pt2, GPoint ipt){
		double minX = pt1.x < pt2.x ? pt1.x : pt2.x;
		double minY = pt1.y < pt2.y ? pt1.y : pt2.y;
		double maxX = pt1.x > pt2.x ? pt1.x : pt2.x;
		double maxY = pt1.y > pt2.y ? pt1.y : pt2.y;

		if(ipt.x < minX || ipt.x > maxX || ipt.y < minY || ipt.y > maxY){
			return false;
		}

		return true;
	}

}
