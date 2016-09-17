package l2p.commons.net.utils;

import l2p.commons.net.AdvIP;

public class NetUtils {

    private final static NetList PRIVATE = new NetList();

    static {
        PRIVATE.add(Net.valueOf("127.0.0.0/8"));
        PRIVATE.add(Net.valueOf("10.0.0.0/8"));
        PRIVATE.add(Net.valueOf("172.16.0.0/12"));
        PRIVATE.add(Net.valueOf("192.168.0.0/16"));
        PRIVATE.add(Net.valueOf("169.254.0.0/16"));
    }

    public static boolean isInternalIP(String address) {
        return PRIVATE.isInRange(address);
    }

    public static Boolean CheckSubNet(String paramString, AdvIP paramAdvIP) {
        String[] arrayOfString1 = paramString.split("\\.");
        String[] arrayOfString2 = paramAdvIP.bitmask.split("\\.");
        String str = "";
        for (int i = 0; i < arrayOfString1.length; i++) {
            str = str + (Integer.valueOf(arrayOfString1[i]).intValue() & Integer.valueOf(arrayOfString2[i]).intValue()) + ".";
        }
        return str.equals(paramAdvIP.ipmask.replace("\\.", "") + ".");
    }
}
