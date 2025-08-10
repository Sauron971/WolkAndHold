package com.kyas.wolkandhold;

import com.kyas.wolkandhold.database.dao.RouteDao;
import com.kyas.wolkandhold.database.dao.RoutePointDao;
import com.kyas.wolkandhold.database.entities.Route;
import com.kyas.wolkandhold.database.entities.RoutePoint;
import com.yandex.mapkit.geometry.Point;

import java.util.ArrayList;
import java.util.List;

public class RouteRepository {
    private final RouteDao routeDao;
    private final RoutePointDao pointDao;
    public long currentRouteId = -1;

    public RouteRepository(RouteDao routeDao, RoutePointDao pointDao) {

        this.routeDao = routeDao;
        this.pointDao = pointDao;
    }

    public void addNewRoute(String name, double distance, long userId) {
        Route r = new Route();
        r.name = name;
        r.createdAt = System.currentTimeMillis();
        r.distance = distance;
        r.userId = userId;
        currentRouteId = routeDao.insert(r);
    }
    public void addPointsToRoute(List<Point> pointList) {
        if (currentRouteId == -1)
            return;
        List<RoutePoint> points = new ArrayList<>();
        pointList.forEach(p -> {
            RoutePoint point = new RoutePoint();
            point.routeId = currentRouteId;
            point.latitude = p.getLatitude();
            point.longitude = p.getLongitude();
            points.add(point);
        });
        pointDao.insertAll(points);

    }

}
