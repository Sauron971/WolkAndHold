package com.kyas.wolkandhold.data;

/**
 * Константы приложения для замены магических чисел
 */
public class Constants {
    
    private Constants() {
        // Утилитный класс - запрещаем создание экземпляров
    }
    
    /**
     * ID для локального пользователя (собственные полигоны и маршруты)
     */
    public static final long LOCAL_USER_ID = -1;
    
    /**
     * Недействительный ID маршрута
     */
    public static final long INVALID_ROUTE_ID = -1;
    
    /**
     * Стандартный радиус поиска полигонов в метрах
     */
    public static final int DEFAULT_SEARCH_RADIUS_METERS = 10000;
    public static final int DEFAULT_CLOSING_RADIUS_METERS = 100;

    /**
     * Максимальное количество точек в маршруте
     */
    public static final int MAX_ROUTE_POINTS = 10000;
    /**
     * Прозрачность полилинии для отрисовки других игроков
     */
    public static final int ALPHA_COLOR_OF_POLYLINE = 128;
}

