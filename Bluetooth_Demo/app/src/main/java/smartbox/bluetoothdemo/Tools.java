package smartbox.bluetoothdemo;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Yuan Zhang on 2/25/2019.
 */


public class Tools {

    public class Wifi {
        private String name;
        private boolean encryption;
        private String authentication;

        public Wifi(String name, boolean encryp, String authentication) {
            this.name = name; this.encryption = encryp; this.authentication = authentication;
        }
    }

    public static HashMap<String, String> getSSIDs(String SSIDinput){
        Set<String> mySet = new HashSet<>();
        HashMap<String, String> myMap = new HashMap<>();

        String[] strings = SSIDinput.split("\n");
        for (int i = 0; i < strings.length; i++) {
            String another = strings[i].replace("E:", "");
            if (another.length() < 1 || another.equals("None"))
                continue;
            if(i+1 < strings.length && strings[i+1].contains("A:")) {
                String authen = strings[i+1];
                myMap.put(another, authen);
                i++;
            } else {
                myMap.put(another, "None");
            }
        }
//        String[] ret_list = new String[strings.length];
//        int i = 0;
//        for (String s: strings) {
//            String another = s;
//            another = another.replace("ESSID:", "");
//            another = another.replace("\"", "");
//            mySet.add(another);
//        }
//
//        for (String item : mySet) {
//            if(item.length() > 0)
//                ret_list[i++] = item;
//            System.out.println(i + " " + item);
//        }

//        return ret_list;
        return myMap;
    }

    /*
    Encryption key:on
                   ESSID:"GTother"
                        Authentication Suites (1) : PSK
                    Encryption key:on
                    ESSID:"eduroam"
                        Authentication Suites (1) :
                   Encryption key:on
                  ESSID:"GTother"
                     Authentication Suites (1) : PSK
                    Encryption key:on
                    ESSID:"eduroam"
                      Authentication Suites (1) :
                      ESSID:"GTVisitor"
                      ESSID:"eduroam"
                      Authentication
     */

}
