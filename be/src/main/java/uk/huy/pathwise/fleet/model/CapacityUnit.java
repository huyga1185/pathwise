package uk.huy.pathwise.fleet.model;

/**
 * What a vehicle's capacity counts (D-36). Stop demand is given per unit; a vehicle is only limited in its own unit.
 * Values are whole numbers (P-14). New units are added here; the database stores the name.
 */
public enum CapacityUnit {
    /** Mass, whole kilograms. */
    KG,
    /** Boxes ("thùng"). */
    BOX,
    /** Packages, parcels ("kiện"). */
    PACKAGE,
    /** Orders, i.e. delivery stops: every stop counts as 1, entered by nobody. */
    ORDER
}
