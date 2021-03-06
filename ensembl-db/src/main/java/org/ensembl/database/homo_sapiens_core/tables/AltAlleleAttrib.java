/*
 * This file is generated by jOOQ.
*/
package org.ensembl.database.homo_sapiens_core.tables;


import javax.annotation.Generated;

import org.ensembl.database.homo_sapiens_core.HomoSapiensCore_89_37;
import org.ensembl.database.homo_sapiens_core.enums.AltAlleleAttribAttrib;
import org.ensembl.database.homo_sapiens_core.tables.records.AltAlleleAttribRecord;
import org.jooq.Field;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.impl.TableImpl;
import org.jooq.types.UInteger;


/**
 * This class is generated by jOOQ.
 */
@Generated(
    value = {
        "http://www.jooq.org",
        "jOOQ version:3.9.5"
    },
    comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class AltAlleleAttrib extends TableImpl<AltAlleleAttribRecord> {

    private static final long serialVersionUID = 162618567;

    /**
     * The reference instance of <code>homo_sapiens_core_89_37.alt_allele_attrib</code>
     */
    public static final AltAlleleAttrib ALT_ALLELE_ATTRIB = new AltAlleleAttrib();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<AltAlleleAttribRecord> getRecordType() {
        return AltAlleleAttribRecord.class;
    }

    /**
     * The column <code>homo_sapiens_core_89_37.alt_allele_attrib.alt_allele_id</code>.
     */
    public final TableField<AltAlleleAttribRecord, UInteger> ALT_ALLELE_ID = createField("alt_allele_id", org.jooq.impl.SQLDataType.INTEGERUNSIGNED, this, "");

    /**
     * The column <code>homo_sapiens_core_89_37.alt_allele_attrib.attrib</code>.
     */
    public final TableField<AltAlleleAttribRecord, AltAlleleAttribAttrib> ATTRIB = createField("attrib", org.jooq.util.mysql.MySQLDataType.VARCHAR.asEnumDataType(org.ensembl.database.homo_sapiens_core.enums.AltAlleleAttribAttrib.class), this, "");

    /**
     * Create a <code>homo_sapiens_core_89_37.alt_allele_attrib</code> table reference
     */
    public AltAlleleAttrib() {
        this("alt_allele_attrib", null);
    }

    /**
     * Create an aliased <code>homo_sapiens_core_89_37.alt_allele_attrib</code> table reference
     */
    public AltAlleleAttrib(String alias) {
        this(alias, ALT_ALLELE_ATTRIB);
    }

    private AltAlleleAttrib(String alias, Table<AltAlleleAttribRecord> aliased) {
        this(alias, aliased, null);
    }

    private AltAlleleAttrib(String alias, Table<AltAlleleAttribRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, "");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Schema getSchema() {
        return HomoSapiensCore_89_37.HOMO_SAPIENS_CORE_89_37;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AltAlleleAttrib as(String alias) {
        return new AltAlleleAttrib(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public AltAlleleAttrib rename(String name) {
        return new AltAlleleAttrib(name, null);
    }
}
