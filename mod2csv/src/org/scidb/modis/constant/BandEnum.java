package org.scidb.modis.constant;

public enum BandEnum {

    b1("1", "EV_250", 0),
    b2("2", "EV_250", 1),
    b3("3", "EV_500", 0),
    b4("4", "EV_500", 1),
    b5("5", "EV_500", 2),
    b6("6", "EV_500", 3),
    b7("7", "EV_500", 4),
    b8("8", "EV_1KM_R", 0),
    b9("9", "EV_1KM_R", 1),
    b10("10", "EV_1KM_R", 2),
    b11("11", "EV_1KM_R", 3),
    b12("12", "EV_1KM_R", 4),
    b13lo("13lo", "EV_1KM_R", 5),
    b13hi("13hi", "EV_1KM_R", 6),
    b14lo("14lo", "EV_1KM_R", 7),
    b14hi("14hi", "EV_1KM_R", 8),
    b15("15", "EV_1KM_R", 9),
    b16("16", "EV_1KM_R", 10),
    b17("17", "EV_1KM_R", 11),
    b18("18", "EV_1KM_R", 12),
    b19("19", "EV_1KM_R", 13),
    b20("20", "EV_1KM_E", 0),
    b21("21", "EV_1KM_E", 1),
    b22("22", "EV_1KM_E", 2),
    b23("23", "EV_1KM_E", 3),
    b24("24", "EV_1KM_E", 4),
    b25("25", "EV_1KM_E", 5),
    b26("26", "EV_B", 0),
    b27("27", "EV_1KM_E", 6),
    b28("28", "EV_1KM_E", 7),
    b29("29", "EV_1KM_E", 8),
    b30("30", "EV_1KM_E", 9),
    b31("31", "EV_1KM_E", 10),
    b32("32", "EV_1KM_E", 11),
    b33("33", "EV_1KM_E", 12),
    b34("34", "EV_1KM_E", 13),
    b35("35", "EV_1KM_E", 14),
    b36("36", "EV_1KM_E", 15);
    public final String name;
    public final String dsPrefix;
    public final int dsOffset;

    BandEnum(String name, String dsPrefix, int dsOffset) {
        this.name = name;
        this.dsPrefix = dsPrefix;
        this.dsOffset = dsOffset;
    }

    @Override
    public String toString() {
        return name;
    }
}