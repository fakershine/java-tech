import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
       String[] names= {"maimaiti-aierken-batuer", "maimaiti-aierken-kuerban"};
       String[] arr={"batuer", "aierken-batuer", "maimaiti","maimaiti-aierken-kuerban"};

        System.out.println(getClassMonitor(new ArrayList<>(Arrays.asList(names)),new ArrayList<>(Arrays.asList(arr))));
    }

    public static String getClassMonitor(ArrayList<String> names, ArrayList<String> ballotTickets) {
        // write code here
        if (ballotTickets.size() > names.size() * 3) {
            return "Invalid election";
        }
        HashMap<String, Integer> countMap = new HashMap<>();
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            for (String ballotTicket : ballotTickets) {
                if (contains(name,ballotTicket) && isValid(names, ballotTicket, i + 1)) {
                    countMap.put(name, countMap.getOrDefault(name, 0) + 1);
                }
            }
        }
        int count = 0;
        ArrayList<String> list = new ArrayList<>();
        for (String key : countMap.keySet()) {
            Integer orDefault = countMap.getOrDefault(key, 0);
            if (orDefault > count) {
                list.clear();
                list.add(key);
                count = orDefault;
            } else if (orDefault == count) {
                list.add(key);
            }
        }
        list.sort(Comparator.comparing(x -> x));
        return !list.isEmpty() ? list.get(0) : "Invalid election";
    }

    private static boolean isValid(ArrayList<String> names, String ballotTicket, int start) {
        for (int i = start; i < names.size(); i++) {
            String name = names.get(i);
            if ( contains(name,ballotTicket)){
                return false;
            }
        }
        return true;
    }

    private static boolean contains(String name, String ballotTicket) {
        String[] split = name.split("-");
        if (split.length == 1 && name.contains(ballotTicket)) {
            return true;
        }
        for (String s : split) {
            if (s.equals(ballotTicket)) {
                return true;
            }
        }
        return false;
    }


    public static int[] timeClassification(String timeRange) {
        String[] s = timeRange.split(" ");

        // "8:00 23:30"
        DateTimeFormatter pattern = DateTimeFormatter.ofPattern("HH:mm");
        LocalTime start;
        try {
            start = LocalTime.parse(s[0], pattern);
        } catch (Exception e) {
            start = LocalTime.parse("0" + s[0], pattern);
        }
        LocalTime end;
        try {
            end = LocalTime.parse(s[1], pattern);
        } catch (Exception e) {
            end = LocalTime.parse("0" + s[1], pattern);
        }
        // 第一档：12:00-13:30、17:30-18:00，共120分钟
        // "8:00 23:30"
        LocalTime start1 = LocalTime.parse("12:00", pattern);
        LocalTime end1 = LocalTime.parse("13:30", pattern);

        LocalTime start2 = LocalTime.parse("17:30", pattern);
        LocalTime end2 = LocalTime.parse("18:00", pattern);

        int[] minutes = getMinutes(start, end.isBefore(start2) ? end : start2, start1, end1);
        if (end.isAfter(start2)) {
            int[] minutes2 = getMinutes(start.isBefore(start2) ? start2 : start, end, start2, end2);
            minutes[0] = minutes[0] + minutes2[0];
            minutes[1] = minutes[1] + minutes2[1];
        }
        int[] arr = new int[3];
        arr[0] = minutes[0];
        arr[1] = Math.min(minutes[1], 600);
        arr[2] = minutes[1] > 600 ? minutes[1] - 600 : 0;
        return arr;
    }

    private static int[] getMinutes(LocalTime start, LocalTime end, LocalTime start1, LocalTime end1) {
        int[] arr = new int[2];
        if (end.isBefore(start1) || start.isAfter(end1)) {
            arr[1] = getMinutes(start, end);
            return arr;
        }

        int total = 0;
        total += getMinutes(start, start1);
        total += getMinutes(end1, end);
        int a = getMinutes(start.isBefore(start1) ? start1 : start, end.isAfter(end1) ? end1 : end);
        arr[0] = a;
        arr[1] = total;
        return arr;
    }

    private static int getMinutes(LocalTime start, LocalTime end) {
        if (start.isAfter(end)) {
            return 0;
        }
        return (end.getHour() - start.getHour()) * 60 + end.getMinute() - start.getMinute();
    }


    public String featureExtraction(String[] docs) {
        // write code here
        ConcurrentHashMap<Character, Integer> countMap = new ConcurrentHashMap<>();
        for (int i = 0; i < docs.length; i++) {
            String str = docs[i];
            HashMap<Character, Integer> count2Map = new HashMap<>();
            for (int j = 0; j < str.length(); j++) {
                Character c = str.charAt(j);
                count2Map.put(c, count2Map.getOrDefault(c, 0) + 1);
            }
            if (i == 0) {
                countMap.putAll(count2Map);
            } else {
                for (Character key : countMap.keySet()) {
                    Integer count = count2Map.getOrDefault(key, 0);
                    if (count == 0) {
                        countMap.remove(key);
                    } else {
                        countMap.put(key, Math.min(count, countMap.get(key)));
                    }
                }
            }
        }
        if (countMap.isEmpty()) {
            return "";
        }
        ArrayList<Character> characters = new ArrayList<>(countMap.keySet());
        characters.sort(Comparator.comparing(x -> x));
        StringBuilder res = new StringBuilder();
        for (Character c : countMap.keySet()) {
            for (int i = 0; i < countMap.get(c); i++) {
                res.append(c.toString());
            }
        }
        return res.toString();
    }

}
