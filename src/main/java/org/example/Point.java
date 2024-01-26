package org.example;

import java.util.Objects;
import java.util.Set;

//The class that will initialize points on the 2d dimension
public class Point {
    double x, y;
    Set<String> keywords; //Optional keyword
    public Point(double x, double y) {
        this.x = x;
        this.y = y;
        this.keywords = null; //if no keyword provided
    }

    public Point(double x, double y, Set<String> keywords){
        this.x = x;
        this.y = y;
        this.keywords = keywords;

    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public Set<String> getKeywords() {
        return keywords;
    }



    @Override
    public String toString() {
        return "Point{" +
                "x=" + x +
                ", y=" + y +
                ", keywords=" + keywords +
                '}';
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Point point = (Point) o;
        return Double.compare(point.x, x) == 0 &&
                Double.compare(point.y, y) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }


}
