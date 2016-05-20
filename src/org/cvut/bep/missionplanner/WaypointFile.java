package org.cvut.bep.missionplanner;

import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * Created by janzaloudek on 19/05/16.
 */
public class WaypointFile {
    static final String LINE_SEPARATOR = System.getProperty("line.separator").toString();

    String version = "110";
    String header;
    ArrayList<WaypointItem> items;

    public WaypointFile(String header, ArrayList<WaypointItem> items) {
        this.header = header;
        this.items = items;
    }

    public String formatAsString() {
        StringBuilder sb = new StringBuilder();
        sb.append(header + LINE_SEPARATOR);
        for (WaypointItem item : items) {
            sb.append(item.formatLine() + LINE_SEPARATOR);
        }

        return sb.toString();
    }

    public void saveToFile(String filename) throws FileNotFoundException {
        PrintStream writer = new PrintStream(filename);
        writer.print(formatAsString());
    }

    public static WaypointFile createFromReader(Reader source) {
        Scanner scanner = new Scanner(source);
        String header = scanner.nextLine();

        ArrayList<WaypointItem> items = new ArrayList<>();
        while (scanner.hasNextLine()) {
            items.add(WaypointItem.createFromString(scanner.nextLine()));
        }

        return new WaypointFile(header, items);
    }

    public static WaypointFile createFromFile(String filename) throws FileNotFoundException {
        return createFromReader(new FileReader(filename));
    }

    public String getVersion() {
        return version;
    }

    public ArrayList<WaypointItem> getItems() {
        return items;
    }

    public void setItems(ArrayList<WaypointItem> items) {
        this.items = items;
    }

    public void updateItem(int index, WaypointItem item) {
        this.items.remove(index);
        this.items.add(index, item);
    }
}
