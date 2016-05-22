package org.cvut.bep.missionplanner;

import com.sun.istack.internal.NotNull;

import java.io.StringReader;
import java.util.Scanner;

/**
 * Created by janzaloudek on 19/05/16.
 */
public class WaypointItem {
    public final static char SEPARATOR = '\t';

    int index, currentWp, coordFrame, command;
    double param1, param2, param3, param4;
    double longitude, latitude, altitude;
    int autocontinue;

    public static WaypointItem createFromString(@NotNull String item) {
        WaypointItem waypointItem = new WaypointItem();

        Scanner scanner = new Scanner(new StringReader(item));
        waypointItem.setIndex(scanner.nextInt());
        waypointItem.setCurrentWp(scanner.nextInt());
        waypointItem.setCoordFrame(scanner.nextInt());
        waypointItem.setCommand(scanner.nextInt());
        waypointItem.setParam1(scanner.nextDouble());
        waypointItem.setParam2(scanner.nextDouble());
        waypointItem.setParam3(scanner.nextDouble());
        waypointItem.setParam4(scanner.nextDouble());
        waypointItem.setLatitude(scanner.nextDouble());
        waypointItem.setLongitude(scanner.nextDouble());
        waypointItem.setAltitude(scanner.nextDouble());
        waypointItem.setAutocontinue(scanner.nextInt());

        return waypointItem;
    }

    public String formatLine() {
        StringBuilder sb = new StringBuilder();
        sb.append(index);
        sb.append(SEPARATOR);
        sb.append(currentWp);
        sb.append(SEPARATOR);
        sb.append(coordFrame);
        sb.append(SEPARATOR);
        sb.append(command);
        sb.append(SEPARATOR);
        sb.append(param1);
        sb.append(SEPARATOR);
        sb.append(param2);
        sb.append(SEPARATOR);
        sb.append(param3);
        sb.append(SEPARATOR);
        sb.append(param4);
        sb.append(SEPARATOR);
        sb.append(longitude);
        sb.append(SEPARATOR);
        sb.append(latitude);
        sb.append(SEPARATOR);
        sb.append(altitude);
        sb.append(SEPARATOR);
        sb.append(autocontinue);

        return sb.toString();
    }

    // setters
    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    public void setAutocontinue(int autocontinue) {
        this.autocontinue = autocontinue;
    }

    public void setCommand(int command) {
        this.command = command;
    }

    public void setCoordFrame(int coordFrame) {
        this.coordFrame = coordFrame;
    }

    public void setCurrentWp(int currentWp) {
        this.currentWp = currentWp;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public void setParam1(double param1) {
        this.param1 = param1;
    }

    public void setParam2(double param2) {
        this.param2 = param2;
    }

    public void setParam3(double param3) {
        this.param3 = param3;
    }

    public void setParam4(double param4) {
        this.param4 = param4;
    }

    // getters
    public double getAltitude() {
        return altitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getParam1() {
        return param1;
    }

    public double getParam2() {
        return param2;
    }

    public double getParam3() {
        return param3;
    }

    public double getParam4() {
        return param4;
    }

    public float getAutocontinue() {
        return autocontinue;
    }

    public int getCommand() {
        return command;
    }

    public int getCoordFrame() {
        return coordFrame;
    }

    public int getCurrentWp() {
        return currentWp;
    }

    public int getIndex() {
        return index;
    }
}
